package za.ac.cput.api;
//JOSHUA REID ADAMS - 230317693
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.auth.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AuthApiClient {

    private final BaseApiClient client;

    public AuthApiClient(BaseApiClient client) {
        this.client = client;
    }

    // ==========================================
    // Public / Patient Authentication Endpoints
    // ==========================================

    public BaseApiClient.ApiResult<AuthResponse> login(LoginRequest request) {
        return client.post("/auth/login", request, AuthResponse.class);
    }

    public BaseApiClient.ApiResult<String> signup(PatientSignupRequest request) {
        return client.post("/auth/signup", request, String.class);
    }

    public BaseApiClient.ApiResult<String> verify(String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return client.get("/auth/verify?token=" + encodedToken, String.class);
    }

    public BaseApiClient.ApiResult<String> resendVerification(String email) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return client.post("/auth/resend-verification?email=" + encodedEmail, null, String.class);
    }

    public BaseApiClient.ApiResult<String> changePassword(ChangePasswordRequest request) {
        return client.put("/auth/change-password", request, String.class);
    }

    public BaseApiClient.ApiResult<AuthResponse> refreshToken(RefreshRequest request) {
        return client.post("/auth/refresh", request, AuthResponse.class);
    }

    // ==========================================
    // Employee Onboarding / Invite Endpoints
    // ==========================================

    public BaseApiClient.ApiResult<String> inviteEmployee(EmployeeInviteRequest request) {
        return client.post("/auth/employee/invite", request, String.class);
    }

    public BaseApiClient.ApiResult<EmployeeInviteResponse> verifyEmployeeInvite(String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return client.get("/auth/employee/invite/verify?token=" + encodedToken, EmployeeInviteResponse.class);
    }

    public BaseApiClient.ApiResult<String> signupDoctor(DoctorSignupRequest request) {
        return client.post("/auth/employee/signup/doctor", request, String.class);
    }

    public BaseApiClient.ApiResult<String> signupClinicStaff(ClinicStaffSignupRequest request) {
        return client.post("/auth/employee/signup/clinicstaff", request, String.class);
    }

    // ==========================================
    // Self-Service Access Request & Admin Review
    // ==========================================

    public BaseApiClient.ApiResult<String> requestAccess(EmployeeAccessRequestSubmission request) {
        return client.post("/auth/employee/request-access", request, String.class);
    }

    /** Matches AuthController#listAccessRequests — GET /employee/access-requests?status= (defaults to PENDING server-side) */
    public BaseApiClient.ApiResult<List<EmployeeAccessRequest>> getAccessRequests(String status) {
        String url = "/auth/employee/access-requests";
        if (status != null && !status.isBlank()) {
            url += "?status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);
        }
        return client.getList(url, new TypeReference<List<EmployeeAccessRequest>>() {});
    }

    public BaseApiClient.ApiResult<String> approveAccessRequest(int id) {
        return client.post("/auth/employee/access-requests/" + id + "/approve", null, String.class);
    }

    public BaseApiClient.ApiResult<String> rejectAccessRequest(int id, String adminNotes) {
        String url = "/auth/employee/access-requests/" + id + "/reject";
        if (adminNotes != null && !adminNotes.isBlank()) {
            url += "?adminNotes=" + URLEncoder.encode(adminNotes, StandardCharsets.UTF_8);
        }
        return client.post(url, null, String.class);
    }

    // ==========================================
    // Forgot Password Flow
    // ==========================================

    public BaseApiClient.ApiResult<String> forgotPassword(ForgotPasswordRequest request) {
        return client.post("/auth/forgot-password", request, String.class);
    }

    /** Returns the reset session token as the response body on success. */
    public BaseApiClient.ApiResult<String> verifyResetCode(VerifyResetCodeRequest request) {
        return client.post("/auth/verify-reset-code", request, String.class);
    }

    public BaseApiClient.ApiResult<String> resetPassword(ResetPasswordRequest request) {
        return client.post("/auth/reset-password", request, String.class);
    }
}