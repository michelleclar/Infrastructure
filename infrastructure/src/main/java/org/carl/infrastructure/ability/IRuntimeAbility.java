package org.carl.infrastructure.ability;

import io.vertx.ext.web.RoutingContext;
import org.carl.infrastructure.config.IProfile;
import org.carl.infrastructure.constant.Constants;
import org.carl.infrastructure.model.ApiRequest;
import org.carl.infrastructure.runtime.IRuntimeUser;

public interface IRuntimeAbility extends IProfile {
    RoutingContext getRoutingContext();

    default ApiRequest getApiRequest() {
        try {
            return getRoutingContext().get(Constants.Fields.API_REQUEST);
        } catch (Exception e) {
            return ApiRequest.BLANK;
        }
    }

    default IRuntimeUser getUser() {
        try {
            return getRoutingContext().get(Constants.Fields.RUNTIME_USER);
        } catch (Exception e) {
            return IRuntimeUser.WHITE;
        }
    }
}
