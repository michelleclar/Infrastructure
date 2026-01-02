package org.carl.infrastructure.authorization.pdp;

import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.configuration.ApiToken;
import dev.openfga.sdk.api.configuration.ClientConfiguration;
import dev.openfga.sdk.api.configuration.Credentials;
import dev.openfga.sdk.errors.FgaInvalidParameterException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FgaClientProvider {

    @ConfigProperty(name = "fga.api-url")
    String apiUrl;

    @ConfigProperty(name = "fga.store-id", defaultValue = "")
    String storeId;

    @ConfigProperty(name = "fga.authorization-model-id", defaultValue = "")
    String authorizationModelId;

    @ConfigProperty(name = "fga.api-token", defaultValue = "")
    String apiToken;

    @Produces
    @ApplicationScoped
    public OpenFgaClient openFgaClient() throws FgaInvalidParameterException {
        ClientConfiguration cfg =
                new ClientConfiguration()
                        .apiUrl(apiUrl)
                        .storeId(storeId)
                        .authorizationModelId(authorizationModelId);
        if (apiToken != null && !apiToken.isEmpty()) {
            cfg = cfg.credentials(new Credentials(new ApiToken(apiToken)));
        }
        // TODO: 可以配置租户Id
        //        cfg.defaultHeaders(Map.of("X-telnet-id",""));
        OpenFgaClient openFgaClient = new OpenFgaClient(cfg);
        return openFgaClient;
    }
}
