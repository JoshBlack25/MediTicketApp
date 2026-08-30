package za.ac.cput.api;
//RAUL JAAIM EVERTS - 230270565
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.domain.Notification;

import java.util.List;

public class NotificationApiClient {

    private final BaseApiClient client;

    public NotificationApiClient(BaseApiClient client) {
        this.client = client;
    }

    public BaseApiClient.ApiResult<Notification> create(Notification notification) {
        return client.post("/notifications/create", notification, Notification.class);
    }

    public BaseApiClient.ApiResult<Notification> read(int id) {
        return client.get("/notifications/read/" + id, Notification.class);
    }

    public BaseApiClient.ApiResult<Void> markAsRead(int id) {
        return client.put("/notifications/markread/" + id, "", Void.class);
    }

    public BaseApiClient.ApiResult<Notification> update(Notification notification) {
        return client.put("/notifications/update", notification, Notification.class);
    }

    public BaseApiClient.ApiResult<Void> delete(int id) {
        return client.delete("/notifications/delete/" + id);
    }

    public BaseApiClient.ApiResult<List<Notification>> getAll() {
        return client.getList("/notifications/getall", new TypeReference<List<Notification>>() {});
    }

    public BaseApiClient.ApiResult<List<Notification>> findByPatient(int patientId) {
        return client.getList("/notifications/patient/" + patientId, new TypeReference<List<Notification>>() {});
    }

    public BaseApiClient.ApiResult<List<Notification>> findByDoctor(int doctorId) {
        return client.getList("/notifications/doctor/" + doctorId, new TypeReference<List<Notification>>() {});
    }

    public BaseApiClient.ApiResult<List<Notification>> findByClinicStaff(int staffId) {
        return client.getList("/notifications/clinicstaff/" + staffId, new TypeReference<List<Notification>>() {});
    }

    public BaseApiClient.ApiResult<List<Notification>> findByStatus(String status) {
        return client.getList("/notifications/status/" + status, new TypeReference<List<Notification>>() {});
    }

    public BaseApiClient.ApiResult<List<Notification>> findByType(String type) {
        return client.getList("/notifications/type/" + type, new TypeReference<List<Notification>>() {});
    }
}