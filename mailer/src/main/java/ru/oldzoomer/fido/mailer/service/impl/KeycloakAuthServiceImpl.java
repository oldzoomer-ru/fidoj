package ru.oldzoomer.fido.mailer.service.impl;

import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.oldzoomer.fido.mailer.service.AuthService;

@Service
public class KeycloakAuthServiceImpl implements AuthService {
    private final Keycloak keycloak;
    private final String realmName;

    public KeycloakAuthServiceImpl(Keycloak keycloak, @Value("${keycloak.realm}") String realmName) {
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
