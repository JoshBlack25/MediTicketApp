package za.ac.cput.api;
//MATTHEW BARRON - 230398863
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.domain.ClinicStaff;

import java.util.List;

public class ClinicStaffApiClient {

    private final BaseApiClient client;

    public ClinicStaffApiClient(BaseApiClient client) {
        this.client = client;
    }

    public BaseApiClient.ApiResult<ClinicStaff> create(ClinicStaff staff) {
        return client.post("/clinicstaff/create", staff, ClinicStaff.class);
    }

    public BaseApiClient.ApiResult<ClinicStaff> read(Integer id) {
        return client.get("/clinicstaff/read/" + id, ClinicStaff.class);
    }

    public BaseApiClient.ApiResult<ClinicStaff> update(ClinicStaff staff) {
        return client.put("/clinicstaff/update", staff, ClinicStaff.class);
    }

    public BaseApiClient.ApiResult<Void> delete(Integer id) {
        return client.delete("/clinicstaff/delete/" + id);
    }

    public BaseApiClient.ApiResult<List<ClinicStaff>> getAll() {
        return client.getList("/clinicstaff/getall", new TypeReference<List<ClinicStaff>>() {});
    }

    public BaseApiClient.ApiResult<ClinicStaff> findByEmail(String email) {
        return client.get("/clinicstaff/email/" + email, ClinicStaff.class);
    }

    public BaseApiClient.ApiResult<List<ClinicStaff>> findByDepartment(String department) {
        return client.getList("/clinicstaff/department/" + department, new TypeReference<List<ClinicStaff>>() {});
    }

    public BaseApiClient.ApiResult<List<ClinicStaff>> findByStaffRole(String staffRole) {
        return client.getList("/clinicstaff/staffrole/" + staffRole, new TypeReference<List<ClinicStaff>>() {});
    }
}