package za.ac.cput.api;
//JADEN CLAYTON ABRAHAMS - 222206721
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.domain.Doctor;

import java.util.List;

public class DoctorApiClient {

    private final BaseApiClient client;

    public DoctorApiClient(BaseApiClient client) {
        this.client = client;
    }

    public BaseApiClient.ApiResult<Doctor> create(Doctor doctor) {
        return client.post("/doctors/create", doctor, Doctor.class);
    }

    public BaseApiClient.ApiResult<Doctor> read(Integer id) {
        return client.get("/doctors/read/" + id, Doctor.class);
    }

    public BaseApiClient.ApiResult<Doctor> update(Doctor doctor) {
        return client.put("/doctors/update", doctor, Doctor.class);
    }

    public BaseApiClient.ApiResult<Void> delete(Integer id) {
        return client.delete("/doctors/delete/" + id);
    }

    public BaseApiClient.ApiResult<List<Doctor>> getAll() {
        return client.getList("/doctors/getall", new TypeReference<List<Doctor>>() {});
    }

    public BaseApiClient.ApiResult<Doctor> findByEmail(String email) {
        return client.get("/doctors/email/" + email, Doctor.class);
    }
}