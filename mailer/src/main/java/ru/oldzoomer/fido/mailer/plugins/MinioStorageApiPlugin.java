package ru.oldzoomer.fido.mailer.plugins;

import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.core.api.StorageApi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MinioStorageApiPlugin implements StorageApi {
    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageApiPlugin(MinioClient minioClient,
                                 @Value("${minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public void save(byte[] bytes, int fileSize, String filePath) {
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(filePath).stream(
                    new ByteArrayInputStream(bytes), fileSize, -1).build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.error("Error while saving file to minio", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] get(String filePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(filePath).build()).readAllBytes();
        } catch (IOException | ServerException | InsufficientDataException | ErrorResponseException |
                 NoSuchAlgorithmException |
                 InvalidKeyException | InvalidResponseException | XmlParserException | InternalException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filePath).build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.error("Error while deleting file from minio", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String filePath) {
        try {
            return minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(filePath).build()) != null;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.error("Error while checking file existence in minio", e);
            return false;
        }
    }

    @Override
    public List<String> list(String filePath) {
        List<String> list = new ArrayList<>();
        minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(filePath).build()).
                forEach(x -> {
                    try {
                        list.add(x.get().objectName());
                    } catch (ErrorResponseException | InsufficientDataException | InternalException |
                             InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException |
                             ServerException | XmlParserException e) {
                        throw new RuntimeException(e);
                    }
                });
        return list;
    }
}
