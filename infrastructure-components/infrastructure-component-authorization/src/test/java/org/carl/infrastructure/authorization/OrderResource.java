package org.carl.infrastructure.authorization;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("order")
public class OrderResource implements External {
    @Path("")
    @GET
    public String order() {
        return """
               {
               "orderId": 1,
               "userId": 2,
               }
               """;
    }
}
