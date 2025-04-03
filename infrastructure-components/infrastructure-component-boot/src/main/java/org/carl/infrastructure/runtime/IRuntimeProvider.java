package org.carl.infrastructure.runtime;

import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import org.carl.infrastructure.model.ApiRequest;

public interface IRuntimeProvider {

    Optional<IRuntimeUser> getUser(RoutingContext context);

    default ApiRequest apiRequest(RoutingContext context) {
        return new ApiRequest(context.request().path());
    }
}
