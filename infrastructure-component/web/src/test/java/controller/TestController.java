package controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import model.BaseArgs;
import model.Member;
import model.PageQuery;

import org.carl.infrastructure.component.web.annotations.ControllerLogged;

import java.util.List;

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

    @Path("/user.manager.employee/view")
    @POST
    public Response view(PageQuery q) {

        return Response.ok(List.of(new Member("carl"), new Member("jack"))).build();
    }
}
