package za.ac.cput.api;
//AIDAN BARENDS - 230155639
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.domain.PatientTicket;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PatientTicketApiClient {

    private final BaseApiClient client;

    public PatientTicketApiClient(BaseApiClient client) {
        this.client = client;
    }

    public BaseApiClient.ApiResult<PatientTicket> create(PatientTicket ticket) {
        return client.post("/patienttickets/create", ticket, PatientTicket.class);
    }

    public BaseApiClient.ApiResult<PatientTicket> read(Integer id) {
        return client.get("/patienttickets/read/" + id, PatientTicket.class);
    }

    public BaseApiClient.ApiResult<PatientTicket> update(PatientTicket ticket) {
        return client.put("/patienttickets/update", ticket, PatientTicket.class);
    }

    public BaseApiClient.ApiResult<Void> delete(Integer id) {
        return client.delete("/patienttickets/delete/" + id);
    }

    public BaseApiClient.ApiResult<List<PatientTicket>> getAll() {
        return client.getList("/patienttickets/getall", new TypeReference<List<PatientTicket>>() {});
    }

    // ==========================================
    // NEW — existed on PatientTicketController but were missing here.
    // progressStatus is the core ticket status-change action
    // (nurse creates a ticket, doctor progresses it through StatusType).
    // ==========================================

    /** Matches PatientTicketController#findByCurrentStatus — GET /status/{status} */
    public BaseApiClient.ApiResult<List<PatientTicket>> findByCurrentStatus(String status) {
        return client.getList("/patienttickets/status/" + status, new TypeReference<List<PatientTicket>>() {});
    }

    /** Matches PatientTicketController#findByPatientUserId — GET /patient/{patientId} */
    public BaseApiClient.ApiResult<List<PatientTicket>> findByPatientUserId(int patientId) {
        return client.getList("/patienttickets/patient/" + patientId, new TypeReference<List<PatientTicket>>() {});
    }

    /**
     * Matches PatientTicketController#progressStatus —
     * PATCH /{ticketId}/status?newStatus=&notes=
     *
     * newStatus is passed as a String matching StatusType's enum name
     * (e.g. "OPEN", "IN_PROGRESS", "RESOLVED") — Spring converts it
     * server-side, same pattern used by findByStatus() elsewhere in
     * this client layer.
     */
    public BaseApiClient.ApiResult<PatientTicket> progressStatus(int ticketId, String newStatus, String notes) {
        StringBuilder endpoint = new StringBuilder("/patienttickets/" + ticketId + "/status?newStatus=" + newStatus);
        if (notes != null && !notes.isBlank()) {
            endpoint.append("&notes=").append(URLEncoder.encode(notes, StandardCharsets.UTF_8));
        }
        return client.patch(endpoint.toString(), null, PatientTicket.class);
    }

    /** Matches PatientTicketController#findByAppointmentId — GET /byappointment/{appointmentId} */
    public BaseApiClient.ApiResult<PatientTicket> findByAppointmentId(int appointmentId) {
        return client.get("/patienttickets/byappointment/" + appointmentId, PatientTicket.class);
    }
}