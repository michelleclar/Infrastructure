package org.carl.infrastructure.authorization;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/external/users")
public class UsersResource {
    private static final Logger log = LoggerFactory.getLogger(UsersResource.class);
    @Inject SecurityIdentity securityIdentity;

    @GET
    @Path("/me")
    public Response me() {
        log.info("user identity: {}", securityIdentity);
        return Response.ok().build();
    }
}
