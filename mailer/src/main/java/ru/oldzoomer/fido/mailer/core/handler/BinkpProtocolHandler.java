package ru.oldzoomer.fido.mailer.core.handler;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.core.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.core.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.core.model.NewReceiving;
import ru.oldzoomer.fido.mailer.core.util.BinkpFrameUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

@Slf4j
@Component
public class BinkpProtocolHandler {
    private final MinioClient minioClient;
    private final String bucketName;
    private final KafkaTemplate<String, NewReceiving> kafkaTemplate;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    public BinkpProtocolHandler(MinioClient minioClient,
                                @Value("${minio.bucket}") String bucketName,
                                KafkaTemplate<String, NewReceiving> kafkaTemplate,
                                KafkaTemplate<String, String> stringKafkaTemplate) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.kafkaTemplate = kafkaTemplate;
        this.stringKafkaTemplate = stringKafkaTemplate;
    }

    public void receiveMail(InputStream inputStream, OutputStream outputStream, String ftnAddress) {
        while (true) {
            try {
                BinkpFrame frame = FrameHandler.readResponse(inputStream);
                if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_FILE ||
                        BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_GET) {
                    String[] fileInfo = BinkpFrameUtil.readCommandFrameString(frame).split(" ");
                    String fileName = fileInfo[0];
                    int fileSize = Integer.parseInt(fileInfo[1]);
                    int offset = Integer.parseInt(fileInfo[3]);
                    log.info("Receiving file: {} with size: {} and offset: {}", fileName, fileSize, offset);
                    byte[] fileBytes = new byte[fileSize];

                    for (int i = 0; i < fileSize; i += 32767) {
                        BinkpFrame dataFrame = FrameHandler.readResponse(inputStream);
                        fileBytes = ArrayUtils.addAll(fileBytes, dataFrame.data());
                    }

                    FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_GOT, String.join(" ", fileInfo));

                    String ftnAddressForMinio = ftnAddress.replaceAll("[:/.@]", "_");
                    String path = ftnAddressForMinio + "/" + fileName;
                    minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(path).stream(
                            new ByteArrayInputStream(fileBytes), fileSize, -1).build());

                    kafkaTemplate.send("file-received", new NewReceiving(ftnAddress, fileName));
                } else if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_EOB) {
                    break;
                }
            } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                     InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                     XmlParserException e) {
                throw new RuntimeException(e);
            }
        }

        stringKafkaTemplate.send("binkp-session", "EOB");
    }

    public void sendMail(InputStream inputStream, OutputStream outputStream, String ftnAddress) {
        minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build()).forEach(itemResult -> {
            try {
                String fileName = itemResult.get().objectName();
                String ftnAddressForMinio = ftnAddress.replaceAll("[:/.@]", "_");
                String path = ftnAddressForMinio + "/" + fileName;
                InputStream minioClientObject = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucketName).object(path).build());

                long fileSize = minioClientObject.available();
                String[] fileInfo = {fileName, String.valueOf(fileSize),
                        String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)), "0"};

                FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_FILE, String.join(" ", fileInfo));

                byte[] buffer = new byte[32767];
                int bytesRead = 0;
                while ((bytesRead = minioClientObject.read(buffer, bytesRead, 32767)) != -1) {
                    FrameHandler.sendDataFrame(outputStream, Arrays.copyOf(buffer, bytesRead));
                    BinkpFrame response = FrameHandler.readResponse(inputStream);
                    if (BinkpFrameUtil.getCommand(response) != BinkpCommandType.M_GOT) {
                        break;
                    }
                }

                minioClientObject.close();
            } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                     InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                     XmlParserException e) {
                throw new RuntimeException(e);
            }
        });

        FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_EOB, "");
        stringKafkaTemplate.send("binkp-session", "EOB");
    }
}
