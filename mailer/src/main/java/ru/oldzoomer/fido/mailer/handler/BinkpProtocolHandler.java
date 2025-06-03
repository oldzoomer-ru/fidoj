package ru.oldzoomer.fido.mailer.handler;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.fido.mailer.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.service.AuthService;
import ru.oldzoomer.fido.mailer.util.BinkpFrameUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Slf4j
public class BinkpProtocolHandler {
    private final AuthService authService;
    private final FrameHandler frameHandler = new FrameHandler();
    private final MinioClient minioClient;
    private final String bucketName;

    public BinkpProtocolHandler(AuthService authService,
                                MinioClient minioClient,
                                String bucketName) {
        this.authService = authService;
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public void handleClient(Socket clientSocket) {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            if (!authenticateClient(inputStream, outputStream)) {
                log.warn("Authentication failed for {}", clientSocket.getRemoteSocketAddress());
                return;
            }

            receiveMail(inputStream);
            sendMail(outputStream);
        } catch (Exception e) {
            log.error("Error handling client", e);
        }
    }

    private boolean authenticateClient(InputStream inputStream, OutputStream outputStream) throws IOException {
        BinkpFrame addressFrame = readResponse(inputStream);
        BinkpFrame passwordFrame = readResponse(inputStream);

        if (BinkpFrameUtil.getCommand(addressFrame) == BinkpCommandType.M_ADR &&
                BinkpFrameUtil.getCommand(passwordFrame) == BinkpCommandType.M_PWD &&
                authService.authenticate(BinkpFrameUtil.readCommandFrameString(addressFrame),
                        BinkpFrameUtil.readCommandFrameString(passwordFrame))) {
            sendCommandFrame(outputStream, BinkpCommandType.M_OK, "");
            return true;
        } else {
            sendCommandFrame(outputStream, BinkpCommandType.M_ERR, "Incorrect password");
            return false;
        }
    }

    private void receiveMail(InputStream inputStream) {
        while (true) {
            try {
                BinkpFrame frame = readResponse(inputStream);
                if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_FILE) {
                    String fileName = BinkpFrameUtil.readCommandFrameString(frame);

                    while (true) {
                        BinkpFrame dataFrame = readResponse(inputStream);
                        if (BinkpFrameUtil.getCommand(dataFrame) == BinkpCommandType.M_EOB) {
                            break;
                        }
                        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(fileName)
                                .stream(new ByteArrayInputStream(BinkpFrameUtil.toBytes(dataFrame)),
                                        -1, 10485760).build());
                    }
                } else if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_OK) {
                    break;
                }
            } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                     InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                     XmlParserException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendMail(OutputStream outputStream) throws IOException {
        minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build()).forEach(itemResult -> {
            try {
                String fileName = itemResult.get().objectName();
                InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucketName).object(fileName).build());

                sendCommandFrame(outputStream, BinkpCommandType.M_FILE, fileName);

                byte[] buffer = new byte[32767 + 2];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    sendDataFrame(outputStream, Arrays.copyOf(buffer, bytesRead));
                }

                sendCommandFrame(outputStream, BinkpCommandType.M_EOB, "");
            } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                     InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                     XmlParserException e) {
                throw new RuntimeException(e);
            }
        });

        sendCommandFrame(outputStream, BinkpCommandType.M_OK, "");
    }

    private BinkpFrame readResponse(InputStream inputStream) throws IOException {
        return frameHandler.readResponse(inputStream);
    }

    private void sendCommandFrame(OutputStream outputStream, BinkpCommandType commandType, String data) throws IOException {
        frameHandler.sendCommandFrame(outputStream, commandType, data);
    }

    private void sendDataFrame(OutputStream outputStream, byte[] data) throws IOException {
        frameHandler.sendDataFrame(outputStream, data);
    }
}
