package controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/user.order")
public class UserController {
    int capacity = 10;

    @Path("/pay")
    @GET
    public Response pay() {
        return Response.ok("success").build();
    }

    @Path("/status/{orderId}")
    @GET
    public Response status(@PathParam("orderId") String orderId) {
        System.out.println(orderId);
        return Response.ok("wait").build();
    }

    @Path("/status/change/{orderId}")
    @GET
    public Response statusChange(@PathParam("orderId") String orderId) {
        System.out.println(orderId);
        return Response.ok("true").build();
    }

    @Path("/send/email")
    @GET
    public Response sendEmail() {
        return Response.ok("carlmichelle493@gmail.com").build();
    }

    @Path("/send/phone")
    @GET
    public Response sendPhone() {
        return Response.ok("12345678").build();
    }

    @Path("/goods/sub")
    @GET
    public Response getData() {
        return Response.ok(--capacity).build();
    }

    @Path("/goods/recover")
    @GET
    public Response recover() {
        return Response.ok(++capacity).build();
    }

    @Path("/over")
    @GET
    public Response over() {
        System.out.println("over");
        return Response.ok("true").build();
    }
}
