package ru.oldzoomer.fido.mailer.plugins.config;

import org.keycloak.admin.client.Keycloak;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.oldzoomer.fido.mailer.core.api.AuthApi;
import ru.oldzoomer.fido.mailer.plugins.KeycloakAuthApiPlugin;

@Configuration
public class KeycloakConfig {
    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakConfig(String serverUrl, String realm,
                          String clientId, String clientSecret) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Bean
    Keycloak keycloak() {
        return Keycloak.getInstance(
                serverUrl,
                realm,
                clientId,
                clientSecret);
    }

    @Bean
    AuthApi authApi(Keycloak keycloak) {
        return new KeycloakAuthApiPlugin(keycloak, realm);
    }
}