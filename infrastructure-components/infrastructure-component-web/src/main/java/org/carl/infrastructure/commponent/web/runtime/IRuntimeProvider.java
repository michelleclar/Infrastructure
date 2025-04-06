package org.carl.infrastructure.commponent.web.runtime;

import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import org.carl.infrastructure.commponent.web.model.ApiRequest;

public interface IRuntimeProvider {

    Optional<IRuntimeUser> getUser(RoutingContext context);

    default ApiRequest apiRequest(RoutingContext context) {
        return new ApiRequest(context.request().path());
    }
}
