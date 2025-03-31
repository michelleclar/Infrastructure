package org.carl.infrastructure.authorization;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/external/users")
public class UserResource {

    @Inject SecurityIdentity securityIdentity;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String user() {
        return String.format(
                "Granted standard user: %s", securityIdentity.getPrincipal().getName());
    }
}
