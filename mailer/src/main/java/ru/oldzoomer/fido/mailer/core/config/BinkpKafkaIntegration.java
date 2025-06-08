package ru.oldzoomer.fido.mailer.core.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import ru.oldzoomer.fido.mailer.core.BinkpProtocolClient;
import ru.oldzoomer.fido.mailer.core.model.Node;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableIntegration
public class BinkpKafkaIntegration {
    private final String bootstrapServers;
    private final String groupId;

    public BinkpKafkaIntegration(@Value("${kafka.bootstrap-servers}") String bootstrapServers,
                                 @Value("${kafka.group-id}") String groupId) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
    }

    @Bean
    public IntegrationFlow kafkaInboundFlow(BinkpProtocolClient client) {
        return IntegrationFlow
                .from(Kafka.messageDrivenChannelAdapter(
                        consumerFactory(),
                        KafkaMessageDrivenChannelAdapter.ListenerMode.record,
                        "binkp-transfer-requests"))
                .handle((Message<Node> nodeMessage, @Headers MessageHeaders headers) -> {
                    Node node = nodeMessage.getPayload();
                    client.transferMail(node.host(), node.port(), node.ftnAddress(), node.password());
                    return true;
                })
                .get();
    }

    private ConsumerFactory<String, Node> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
