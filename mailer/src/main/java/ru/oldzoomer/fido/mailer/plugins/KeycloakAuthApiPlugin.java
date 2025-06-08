package ru.oldzoomer.fido.mailer.plugins;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
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
        CredentialRepresentation representation = keycloak.realm(realmName).users().get(nodeAddress).credentials().stream()
                .filter(credentialRepresentation -> "binkp_password".equals(credentialRepresentation.getType())
                        && password.equals(credentialRepresentation.getValue()))
                .findFirst()
                .orElse(null);
        return representation != null;
    }
}
