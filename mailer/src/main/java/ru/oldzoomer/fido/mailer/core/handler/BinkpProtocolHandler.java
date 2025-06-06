package ru.oldzoomer.fido.mailer.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.core.api.StorageApi;
import ru.oldzoomer.fido.mailer.core.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.core.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.core.model.NewReceiving;
import ru.oldzoomer.fido.mailer.core.util.BinkpFrameUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

@Slf4j
@Component
public class BinkpProtocolHandler {
    private final KafkaTemplate<String, NewReceiving> kafkaTemplate;
    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final StorageApi storageApi;

    public BinkpProtocolHandler(KafkaTemplate<String, NewReceiving> kafkaTemplate,
                                KafkaTemplate<String, String> stringKafkaTemplate,
                                StorageApi storageApi) {
        this.kafkaTemplate = kafkaTemplate;
        this.stringKafkaTemplate = stringKafkaTemplate;
        this.storageApi = storageApi;
    }

    public void receiveMail(InputStream inputStream, OutputStream outputStream, String ftnAddress) {
        while (true) {
            BinkpFrame frame = FrameHandler.readResponse(inputStream);
            if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_FILE ||
                    BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_GET) {
                String[] fileInfo = BinkpFrameUtil.readCommandFrameString(frame).split(" ");
                String fileName = fileInfo[0];
                int fileSize = Integer.parseInt(fileInfo[1]);
                int offset = Integer.parseInt(fileInfo[3]);
                log.info("Receiving file: {} with size: {} and offset: {}", fileName, fileSize, offset);

                byte[] fileBytes = new byte[fileSize];
                int readSize = 0;

                while (readSize < fileSize) {
                    BinkpFrame dataFrame = FrameHandler.readResponse(inputStream);
                    fileBytes = ArrayUtils.addAll(fileBytes, dataFrame.data());
                    readSize += dataFrame.length() - BinkpFrameUtil.BINKP_FRAME_HEADER_SIZE;
                }

                FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_GOT, String.join(" ", fileInfo));

                String ftnAddressForMinio = ftnAddress.replaceAll("[:/.@]", "_");
                String path = ftnAddressForMinio + "/" + fileName;
                storageApi.save(fileBytes, fileSize, path);

                kafkaTemplate.send("file-received", new NewReceiving(ftnAddress, fileName));
            } else if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_EOB) {
                break;
            }
        }

        stringKafkaTemplate.send("binkp-session", "EOB");
    }

    public void sendMail(InputStream inputStream, OutputStream outputStream, String ftnAddress) {
        String ftnAddressForStorageApi = ftnAddress.replaceAll("[:/.@]", "_");
        storageApi.list(ftnAddressForStorageApi).forEach(itemResult -> {
            byte[] fileBytes = storageApi.get(itemResult);
            int fileSize = fileBytes.length;
            String[] fileInfo = {itemResult, String.valueOf(fileSize),
                    String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)), "0"};

            FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_FILE, String.join(" ", fileInfo));

            for (int i = 0; i < fileSize; i += 32767) {
                FrameHandler.sendDataFrame(outputStream, Arrays.copyOfRange(fileBytes, i, i + 32767));
                BinkpFrame response = FrameHandler.readResponse(inputStream);
                if (BinkpFrameUtil.getCommand(response) != BinkpCommandType.M_GOT) {
                    break;
                }
            }

            storageApi.delete(itemResult);
        });

        FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_EOB, "");
        stringKafkaTemplate.send("binkp-session", "EOB");
    }
}
