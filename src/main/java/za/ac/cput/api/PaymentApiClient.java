package za.ac.cput.api;
//ABDULLAHI RAAGE FARAH - 230971091
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.domain.Payment;

import java.util.List;

public class PaymentApiClient {

    private final BaseApiClient client;

    public PaymentApiClient(BaseApiClient client) {
        this.client = client;
    }

    public BaseApiClient.ApiResult<Payment> create(Payment payment) {
        return client.post("/payments/create", payment, Payment.class);
    }

    public BaseApiClient.ApiResult<Payment> read(Integer id) {
        return client.get("/payments/read/" + id, Payment.class);
    }

    public BaseApiClient.ApiResult<Payment> update(Payment payment) {
        return client.put("/payments/update", payment, Payment.class);
    }

    public BaseApiClient.ApiResult<Void> delete(Integer id) {
        return client.delete("/payments/delete/" + id);
    }

    public BaseApiClient.ApiResult<List<Payment>> getAll() {
        return client.getList("/payments/getall", new TypeReference<List<Payment>>() {});
    }

    public BaseApiClient.ApiResult<List<Payment>> findByStatus(String status) {
        return client.getList("/payments/status/" + status, new TypeReference<List<Payment>>() {});
    }

    public BaseApiClient.ApiResult<List<Payment>> findByMethod(String method) {
        return client.getList("/payments/method/" + method, new TypeReference<List<Payment>>() {});
    }
}