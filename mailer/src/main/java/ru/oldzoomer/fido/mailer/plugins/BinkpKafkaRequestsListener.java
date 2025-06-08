package ru.oldzoomer.fido.mailer.plugins;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.core.BinkpProtocolClient;
import ru.oldzoomer.fido.mailer.plugins.models.Node;

@Component
public class BinkpKafkaRequestsListener {
    private final BinkpProtocolClient binkpProtocolClient;

    public BinkpKafkaRequestsListener(BinkpProtocolClient binkpProtocolClient) {
        this.binkpProtocolClient = binkpProtocolClient;
    }

    @KafkaListener(topics = "binkp-transfer-requests", groupId = "binkp")
    public void handleTransferRequest(Node node) {
        binkpProtocolClient.transferMail(node.host(), node.port(),
                node.ftnAddress(), node.password());
    }
}
