package org.carl.infra.component.web.runtime;

import io.vertx.ext.web.RoutingContext;

import org.carl.infra.component.web.model.ApiRequest;

import java.util.Optional;

public interface IRuntimeProvider {

    Optional<IRuntimeUser> getUser(RoutingContext context);

    default ApiRequest apiRequest(RoutingContext context) {
        return new ApiRequest(context.request().path());
    }
}
