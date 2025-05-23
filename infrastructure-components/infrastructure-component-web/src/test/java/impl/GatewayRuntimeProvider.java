package impl;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.component.web.runtime.IRuntimeProvider;
import org.carl.infrastructure.component.web.runtime.IRuntimeUser;

import java.util.Optional;

@ApplicationScoped
public class GatewayRuntimeProvider implements IRuntimeProvider {
    @Inject SecurityIdentity identity;

    @Override
    public Optional<IRuntimeUser> getUser(RoutingContext context) {
        HttpServerRequest request = context.request();
        String authorization = request.getHeader("Authorization");
        if (authorization != null) {
            return Optional.of(IRuntimeUser.WHITE);
        }
        // decode
        return Optional.empty();
    }
}
