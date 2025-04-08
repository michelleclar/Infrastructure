package controller.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.carl.infrastructure.annotations.Logged;
import org.carl.infrastructure.persistence.PersistenceStd;

@Logged
@Path("demo")
public class DemoController extends PersistenceStd {
    @GET
    @Path("/hello")
    public String hello() {
        return "Hello World!";
    }
}
