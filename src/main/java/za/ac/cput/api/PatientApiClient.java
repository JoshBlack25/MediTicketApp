package za.ac.cput.api;
//AIDAN BARENDS - 230155639
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.domain.Patient;

import java.time.LocalDate;
import java.util.List;

public class PatientApiClient {

    private final BaseApiClient client;

    public PatientApiClient(BaseApiClient client) {
        this.client = client;
    }

    public BaseApiClient.ApiResult<Patient> create(Patient patient) {
        return client.post("/patients/create", patient, Patient.class);
    }

    public BaseApiClient.ApiResult<Patient> read(Integer id) {
        return client.get("/patients/read/" + id, Patient.class);
    }

    public BaseApiClient.ApiResult<Patient> update(Patient patient) {
        return client.put("/patients/update", patient, Patient.class);
    }

    public BaseApiClient.ApiResult<Void> delete(Integer id) {
        return client.delete("/patients/delete/" + id);
    }

    public BaseApiClient.ApiResult<List<Patient>> getAll() {
        return client.getList("/patients/getall", new TypeReference<List<Patient>>() {});
    }

    public BaseApiClient.ApiResult<Patient> findByEmail(String email) {
        return client.get("/patients/email/" + email, Patient.class);
    }

    public BaseApiClient.ApiResult<List<Patient>> findByDateRegistered(LocalDate date) {
        return client.getList("/patients/dateregistered/" + date, new TypeReference<List<Patient>>() {});
    }
}