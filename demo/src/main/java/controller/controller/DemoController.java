package controller.controller;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.carl.generated.tables.pojos.ImMessages;
import org.carl.generated.tables.records.ImMessagesRecord;
import org.carl.infrastructure.annotations.Logged;
import org.carl.infrastructure.persistence.PersistenceStd;

@Logged
@Path("demo")
public class DemoController extends PersistenceStd {
    @Path("insert")
    @POST
    public Response insert(ImMessages po) {
        ImMessagesRecord insertMessage = new ImMessagesRecord(po);
        getPersistenceContext().insert(insertMessage);
        po.setMessageId(insertMessage.getMessageId());
        return Response.ok(po).build();
    }
}
