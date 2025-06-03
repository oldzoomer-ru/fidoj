package ru.oldzoomer.fido.mailer.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.oldzoomer.fido.mailer.core.BinkpProtocolServer;
import ru.oldzoomer.fido.mailer.service.AuthService;

@Configuration
public class BinkpServerConfig {
    private final MinioClient minioClient;
    private final AuthService authService;
    private final int port;
    private final String bucketName;

    public BinkpServerConfig(MinioClient minioClient, AuthService authService,
                             @Value("${binkp.port}") int port, @Value("${minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.authService = authService;
        this.port = port;
        this.bucketName = bucketName;
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public BinkpProtocolServer binkpProtocolServer() {
        return new BinkpProtocolServer(port, authService, minioClient, bucketName);
    }
}
