package ru.oldzoomer.fido.mailer.plugins;

import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.core.BinkpProtocolClient;

@Component
public class BinkpKafkaRequestsListener {
    private final Keycloak keycloak;
    private final String realm;
    private final BinkpProtocolClient binkpProtocolClient;

    public BinkpKafkaRequestsListener(Keycloak keycloak, @Value("${keycloak.realm}") String realm,
                                 BinkpProtocolClient binkpProtocolClient) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.binkpProtocolClient = binkpProtocolClient;
    }

    @KafkaListener(topics = "binkp-transfer-requests")
    public void handleTransferRequest() {
        keycloak.realm(realm).users().list()
                .stream()
                .filter(user -> user.getAttributes().get("isLink").getFirst().equals("true"))
                .forEach(user -> {
                    String ftnAddress = user.getAttributes().get("ftnAddress").getFirst();
                    String password = user.getAttributes().get("password").getFirst();
                    String host = user.getAttributes().get("host").getFirst();
                    int port = Integer.parseInt(user.getAttributes().get("port").getFirst());
                    binkpProtocolClient.transferMail(host, port, ftnAddress, password);
                });
    }
}
