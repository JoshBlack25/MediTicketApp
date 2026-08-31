package za.ac.cput.api;
//JOSHUA REID ADAMS - 230317693
import com.fasterxml.jackson.core.type.TypeReference;
import za.ac.cput.model.domain.TicketStatus;

import java.util.List;

public class TicketStatusApiClient {

    private final BaseApiClient client;

    public TicketStatusApiClient(BaseApiClient client) {
        this.client = client;
    }

    public BaseApiClient.ApiResult<TicketStatus> create(TicketStatus status) {
        return client.post("/ticketstatus/create", status, TicketStatus.class);
    }

    public BaseApiClient.ApiResult<TicketStatus> read(Integer id) {
        return client.get("/ticketstatus/read/" + id, TicketStatus.class);
    }

    public BaseApiClient.ApiResult<TicketStatus> update(TicketStatus status) {
        return client.put("/ticketstatus/update", status, TicketStatus.class);
    }

    public BaseApiClient.ApiResult<Void> delete(Integer id) {
        return client.delete("/ticketstatus/delete/" + id);
    }

    public BaseApiClient.ApiResult<List<TicketStatus>> getAll() {
        return client.getList("/ticketstatus/getall", new TypeReference<List<TicketStatus>>() {});
    }

    public BaseApiClient.ApiResult<List<TicketStatus>> findByStatusType(String statusType) {
        return client.getList("/ticketstatus/statustype/" + statusType, new TypeReference<List<TicketStatus>>() {});
    }
}