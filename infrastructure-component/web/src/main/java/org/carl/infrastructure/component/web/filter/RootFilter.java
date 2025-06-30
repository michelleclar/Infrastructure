package org.carl.infrastructure.component.web.filter;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;

import org.carl.infrastructure.component.web.config.ScaffoldConfig;
import org.carl.infrastructure.component.web.model.ApiRequest;
import org.carl.infrastructure.component.web.runtime.IRuntimeProvider;
import org.carl.infrastructure.component.web.runtime.IRuntimeUser;
import org.carl.infrastructure.tool.constant.Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

// NOTE: web runtime context
@ApplicationScoped
public class RootFilter {

    @ConfigProperty(name = "quarkus.rest.path", defaultValue = "/api")
    String restPath;

    public static String userID = "userID";

    @Inject ScaffoldConfig config;

    @Inject Instance<IRuntimeProvider> runtimeProvider;

    private SessionHandler sessionHandler;

    void init(@Observes StartupEvent ev, Vertx vertx) {
        if (config.useSession()) {
            int hour = config.sessionTimeoutHour();
            long timeOut = (long) hour * 60 * 60 * 1000;
            sessionHandler =
                    SessionHandler.create(LocalSessionStore.create(vertx))
                            .setSessionTimeout(timeOut);
        }
    }

    @RouteFilter(Priorities.USER + 100)
    void sessionFilter(RoutingContext context) {
        String path = context.request().path();
        if (config.useSession() && path.startsWith(restPath)) {
            sessionHandler.handle(context);
        } else {
            context.next();
        }
    }

    @RouteFilter(Priorities.USER)
    void userFilter(RoutingContext context) {
        IRuntimeUser runtimeUser = resolveRuntimeUser(context);

        context.put(Constants.Fields.RUNTIME_USER, runtimeUser);

        context.next();
    }

    @RouteFilter(Priorities.HEADER_DECORATOR)
    void apiRequestFilter(RoutingContext context) {

        IRuntimeUser runtimeUser = context.get(Constants.Fields.RUNTIME_USER);

        if (runtimeProvider.isResolvable()) {
            ApiRequest apiRequest = runtimeProvider.get().apiRequest(context);
            apiRequest.setUser(runtimeUser);
            context.put(Constants.Fields.API_REQUEST, apiRequest);
        } else {
            context.put(Constants.Fields.API_REQUEST, ApiRequest.BLANK);
        }

        context.next();
    }

    private IRuntimeUser resolveRuntimeUser(RoutingContext context) {
        if (runtimeProvider.isResolvable()) {
            Optional<IRuntimeUser> userOptional = runtimeProvider.get().getUser(context);
            if (userOptional.isPresent()) {
                return userOptional.get();
            }
            if (config.isTestMode() && context.request().getHeader(RootFilter.userID) != null) {
                String userID = context.request().getHeader(RootFilter.userID);
                return IRuntimeUser.build(userID);
            }
        }

        return IRuntimeUser.WHITE;
    }
}
