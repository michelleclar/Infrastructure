package org.carl.infra.component.web.ability;

import io.vertx.ext.web.RoutingContext;

import org.carl.infra.component.web.config.IProfile;
import org.carl.infra.component.web.constant.Constants;
import org.carl.infra.component.web.model.ApiRequest;
import org.carl.infra.component.web.runtime.IRuntimeUser;

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
