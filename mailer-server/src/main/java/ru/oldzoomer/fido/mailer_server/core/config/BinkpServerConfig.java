package ru.oldzoomer.fido.mailer_server.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.oldzoomer.fido.mailer_server.core.BinkpProtocolServer;
import ru.oldzoomer.fido.mailer_server.core.handler.BinkpProtocolServerHandler;

@Configuration
public class BinkpServerConfig {
    private final int port;
    private final BinkpProtocolServerHandler binkpProtocolServerHandler;

    public BinkpServerConfig(@Value("${binkp.port}") int port,
                             BinkpProtocolServerHandler binkpProtocolServerHandler) {
        this.port = port;
        this.binkpProtocolServerHandler = binkpProtocolServerHandler;
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public BinkpProtocolServer binkpProtocolServer() {
        return new BinkpProtocolServer(port, binkpProtocolServerHandler);
    }
}
