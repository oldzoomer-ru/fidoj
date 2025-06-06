package ru.oldzoomer.fido.mailer.plugins.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.oldzoomer.fido.mailer.core.api.StorageApi;
import ru.oldzoomer.fido.mailer.plugins.MinioStorageApiPlugin;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    StorageApi storageApi(MinioClient minioClient) {
        return new MinioStorageApiPlugin(minioClient, bucket);
    }
}