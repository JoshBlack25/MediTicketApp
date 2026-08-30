package za.ac.cput.api;
//JOSHUA REID ADAMS - 230317693
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.domain.Appointment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class AppointmentApiClient {

    private final BaseApiClient client;

    public AppointmentApiClient(BaseApiClient client) {
        this.client = client;
    }

    public BaseApiClient.ApiResult<Appointment> create(Appointment appointment) {
        return client.post("/appointment/create", appointment, Appointment.class);
    }

    public BaseApiClient.ApiResult<Appointment> read(Integer id) {
        return client.get("/appointment/read/" + id, Appointment.class);
    }

    public BaseApiClient.ApiResult<Appointment> update(Appointment appointment) {
        return client.put("/appointment/update", appointment, Appointment.class);
    }

    public BaseApiClient.ApiResult<Void> delete(Integer id) {
        return client.delete("/appointment/delete/" + id);
    }

    public BaseApiClient.ApiResult<List<Appointment>> getAll() {
        return client.getList("/appointment/getAll", new TypeReference<List<Appointment>>() {});
    }

    public BaseApiClient.ApiResult<List<Appointment>> findByDoctor(int doctorId) {
        return client.getList("/appointment/findByDoctor/" + doctorId, new TypeReference<List<Appointment>>() {});
    }

    public BaseApiClient.ApiResult<List<Appointment>> findByStaff(int staffId) {
        return client.getList("/appointment/findByStaff/" + staffId, new TypeReference<List<Appointment>>() {});
    }

    public BaseApiClient.ApiResult<List<Appointment>> findByDate(LocalDate date) {
        return client.getList("/appointment/findByDate/" + date, new TypeReference<List<Appointment>>() {});
    }

    public BaseApiClient.ApiResult<List<Appointment>> findByStatus(String status) {
        return client.getList("/appointment/findByStatus/" + status, new TypeReference<List<Appointment>>() {});
    }

    public BaseApiClient.ApiResult<List<Appointment>> findByDoctorAndDate(int doctorId, LocalDate date) {
        return client.getList("/appointment/findByDoctorAndDate/" + doctorId + "/" + date,
                new TypeReference<List<Appointment>>() {});
    }

    // ==========================================
    // NEW — existed on AppointmentController but were missing here.
    // approve/reject/complete are the core Nurse + Doctor workflow actions.
    // ==========================================

    /** Matches AppointmentController#findByPatientUserId — GET /findByPatient/{patientId} */
    public BaseApiClient.ApiResult<List<Appointment>> findByPatient(int patientId) {
        return client.getList("/appointment/findByPatient/" + patientId, new TypeReference<List<Appointment>>() {});
    }

    /** Matches AppointmentController#approve — POST /{appointmentId}/approve?doctorId=&staffId= */
    public BaseApiClient.ApiResult<Appointment> approve(int appointmentId, int doctorId, int staffId) {
        String endpoint = "/appointment/" + appointmentId + "/approve?doctorId=" + doctorId + "&staffId=" + staffId;
        return client.post(endpoint, null, Appointment.class);
    }

    /** Matches AppointmentController#reject — POST /{appointmentId}/reject?staffId=&reason= */
    public BaseApiClient.ApiResult<Appointment> reject(int appointmentId, int staffId, String reason) {
        StringBuilder endpoint = new StringBuilder("/appointment/" + appointmentId + "/reject?staffId=" + staffId);
        if (reason != null && !reason.isBlank()) {
            endpoint.append("&reason=").append(URLEncoder.encode(reason, StandardCharsets.UTF_8));
        }
        return client.post(endpoint.toString(), null, Appointment.class);
    }


}