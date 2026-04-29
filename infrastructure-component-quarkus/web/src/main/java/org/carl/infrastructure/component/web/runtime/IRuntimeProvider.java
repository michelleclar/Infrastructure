package org.carl.infrastructure.component.web.runtime;

import io.vertx.ext.web.RoutingContext;

import org.carl.infrastructure.component.web.model.ApiRequest;

import java.util.Optional;

public interface IRuntimeProvider {

    Optional<IRuntimeUser> getUser(RoutingContext context);

    default ApiRequest apiRequest(RoutingContext context) {
        return new ApiRequest(context.request().path());
    }
}
