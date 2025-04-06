package org.carl.infrastructure.commponent.web.runtime;

import io.vertx.ext.web.RoutingContext;
import org.carl.infrastructure.commponent.web.model.ApiRequest;

import java.util.Optional;

public interface IRuntimeProvider {

    Optional<IRuntimeUser> getUser(RoutingContext context);

    default ApiRequest apiRequest(RoutingContext context) {
        return new ApiRequest(context.request().path());
    }
}
