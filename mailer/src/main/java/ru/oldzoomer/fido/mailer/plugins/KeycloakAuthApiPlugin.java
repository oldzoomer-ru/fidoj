package ru.oldzoomer.fido.mailer.plugins;

import org.keycloak.admin.client.Keycloak;
import ru.oldzoomer.fido.mailer.core.api.AuthApi;

public class KeycloakAuthApiPlugin implements AuthApi {
    private final Keycloak keycloak;
    private final String realmName;

    public KeycloakAuthApiPlugin(Keycloak keycloak, String realmName) {
        this.keycloak = keycloak;
        this.realmName = realmName;
    }

    @Override
    public boolean authenticate(String nodeAddress, String password) {
        keycloak.realm(realmName).users().get(nodeAddress).credentials().stream()
                .filter(credentialRepresentation -> "password".equals(credentialRepresentation.getType())
                        && password.equals(credentialRepresentation.getValue()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        return true;
    }
}
