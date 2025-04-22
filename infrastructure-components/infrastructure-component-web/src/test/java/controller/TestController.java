package controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import model.BaseArgs;
import org.carl.infrastructure.component.web.annotations.ControllerLogged;

@ControllerLogged
@Path("/test")
public class TestController {
    @GET
    public Response root() {
        return Response.ok("success").build();
    }

    @Path("/logger.base/args")
    @POST
    public Response base(BaseArgs b) {
        return Response.ok(b).build();
    }
}
