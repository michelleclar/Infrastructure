package org.carl.infrastructure.runtime;

import io.vertx.ext.web.RoutingContext;
import org.carl.infrastructure.model.ApiRequest;


import java.util.Optional;

public interface IRuntimeProvider {

    Optional<IRuntimeUser> getUser(RoutingContext context);

    default ApiRequest apiRequest(RoutingContext context) {
        return new ApiRequest(context.request().path());
    }

}
