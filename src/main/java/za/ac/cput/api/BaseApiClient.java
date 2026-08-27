package za.ac.cput.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Generic API client for all backend controllers.
 * Handles JSON serialization, auth headers, and common HTTP patterns.
 */
public class BaseApiClient {

    private static final String BASE_URL = "http://localhost:8080/api";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String authToken;

    public BaseApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    }

    /** Set JWT token after login — all subsequent requests include it */
    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public void clearAuthToken() {
        this.authToken = null;
    }

    // ── Generic CRUD Operations ───────────────────────────────

    public <T> ApiResult<T> get(String endpoint, Class<T> responseClass) {
        try {
            HttpRequest request = buildRequest(endpoint).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response, responseClass);
        } catch (Exception e) {
            return ApiResult.error("Network error: " + e.getMessage());
        }
    }

    public <T> ApiResult<List<T>> getList(String endpoint, TypeReference<List<T>> typeRef) {
        try {
            HttpRequest request = buildRequest(endpoint).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                List<T> data = objectMapper.readValue(response.body(), typeRef);
                return ApiResult.success(data);
            }
            return ApiResult.failure(response.statusCode(), response.body());
        } catch (Exception e) {
            return ApiResult.error("Network error: " + e.getMessage());
        }
    }

    public <T> ApiResult<T> post(String endpoint, Object body, Class<T> responseClass) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = buildRequest(endpoint)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response, responseClass);
        } catch (Exception e) {
            return ApiResult.error("Network error: " + e.getMessage());
        }
    }

    public <T> ApiResult<T> put(String endpoint, Object body, Class<T> responseClass) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = buildRequest(endpoint)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response, responseClass);
        } catch (Exception e) {
            return ApiResult.error("Network error: " + e.getMessage());
        }
    }

    /**
     * NEW — required for endpoints like PatientTicketController#progressStatus,
     * which is mapped with @PatchMapping. java.net.http.HttpRequest has no
     * built-in .PATCH() like it does .PUT()/.POST(), so it's built via .method().
     */
    public <T> ApiResult<T> patch(String endpoint, Object body, Class<T> responseClass) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = buildRequest(endpoint)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response, responseClass);
        } catch (Exception e) {
            return ApiResult.error("Network error: " + e.getMessage());
        }
    }

    public ApiResult<Void> delete(String endpoint) {
        try {
            HttpRequest request = buildRequest(endpoint).DELETE().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return ApiResult.success(null);
            }
            return ApiResult.failure(response.statusCode(), response.body());
        } catch (Exception e) {
            return ApiResult.error("Network error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private HttpRequest.Builder buildRequest(String endpoint) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .timeout(Duration.ofSeconds(10));
        if (authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        return builder;
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResult<T> handleResponse(HttpResponse<String> response, Class<T> clazz) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            try {
                if (clazz == String.class) {
                    return ApiResult.success((T) response.body());
                }
                if (clazz == Void.class) {
                    return ApiResult.success(null);
                }
                T data = objectMapper.readValue(response.body(), clazz);
                return ApiResult.success(data);
            } catch (Exception e) {
                return ApiResult.error("Failed to parse response: " + e.getMessage());
            }
        }
        return ApiResult.failure(response.statusCode(), response.body());
    }

    // ── ApiResult (moved here so all clients share it) ────────

    public static class ApiResult<T> {
        private final boolean success;
        private final T data;
        private final String message;
        private final int statusCode;

        private ApiResult(boolean success, T data, String message, int statusCode) {
            this.success = success;
            this.data = data;
            this.message = message;
            this.statusCode = statusCode;
        }

        public static <T> ApiResult<T> success(T data) {
            return new ApiResult<>(true, data, null, 200);
        }

        public static <T> ApiResult<T> failure(int statusCode, String message) {
            return new ApiResult<>(false, null, message, statusCode);
        }

        public static <T> ApiResult<T> error(String message) {
            return new ApiResult<>(false, null, message, 0);
        }

        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public String getMessage() { return message; }
        public int getStatusCode() { return statusCode; }
    }
    public boolean hasAuthToken() {
        return authToken != null;
    }
}