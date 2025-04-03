package org.carl.infrastructure.authorization;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/external/admin")
public class AdminResource {

    @Inject SecurityIdentity securityIdentity;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String admin() {
        return String.format("Granted user: %s", securityIdentity.getPrincipal().getName());
    }
}
