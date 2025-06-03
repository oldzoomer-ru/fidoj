package ru.oldzoomer.fido.mailer.handler;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.util.BinkpFrameUtil;

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
    private final FrameHandler frameHandler;
    private final MinioClient minioClient;
    private final String bucketName;

    public BinkpProtocolHandler(MinioClient minioClient, FrameHandler frameHandler,
                                @Value("${minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.frameHandler = frameHandler;
    }

    public void receiveMail(InputStream inputStream, OutputStream outputStream) {
        while (true) {
            try {
                BinkpFrame frame = frameHandler.readResponse(inputStream);
                if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_FILE ||
                        BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_GET) {
                    String[] fileInfo = BinkpFrameUtil.readCommandFrameString(frame).split(" ");
                    String fileName = fileInfo[0];
                    int fileSize = Integer.parseInt(fileInfo[1]);
                    int offset = Integer.parseInt(fileInfo[3]);
                    log.info("Receiving file: {} with size: {} and offset: {}", fileName, fileSize, offset);
                    byte[] fileBytes = new byte[fileSize];

                    for (int i = 0; i < fileSize; i += 32767) {
                        BinkpFrame dataFrame = frameHandler.readResponse(inputStream);
                        fileBytes = ArrayUtils.addAll(fileBytes, dataFrame.data());
                    }

                    frameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_GOT, String.join(" ", fileInfo));
                    minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                            new ByteArrayInputStream(fileBytes), fileSize, -1).build());
                } else if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_EOB) {
                    break;
                }
            } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                     InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                     XmlParserException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMail(InputStream inputStream, OutputStream outputStream) {
        minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build()).forEach(itemResult -> {
            try {
                String fileName = itemResult.get().objectName();
                InputStream minioClientObject = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucketName).object(fileName).build());

                long fileSize = minioClientObject.available();
                String[] fileInfo = {fileName, String.valueOf(fileSize),
                        String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)), "0"};

                frameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_FILE, String.join(" ", fileInfo));

                byte[] buffer = new byte[32767];
                int bytesRead = 0;
                while ((bytesRead = minioClientObject.read(buffer, bytesRead, 32767)) != -1) {
                    frameHandler.sendDataFrame(outputStream, Arrays.copyOf(buffer, bytesRead));
                    BinkpFrame response = frameHandler.readResponse(inputStream);
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

        frameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_EOB, "");
    }
}
