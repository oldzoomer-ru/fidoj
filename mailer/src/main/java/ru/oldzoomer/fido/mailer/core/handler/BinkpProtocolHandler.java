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

import static ru.oldzoomer.fido.mailer.core.constant.BinkpFrameSizes.BINKP_FRAME_HEADER_SIZE;
import static ru.oldzoomer.fido.mailer.core.constant.BinkpFrameSizes.BINKP_FRAME_MAX_SIZE;

/**
 * Binkp protocol handler
 *
 * @author oldzoomer
 */
@Slf4j
@Component
public class BinkpProtocolHandler {
    private final KafkaTemplate<String, NewReceiving> kafkaTemplate;
    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final StorageApi storageApi;

    /**
     * Constructor
     *
     * @param kafkaTemplate       KafkaTemplate
     * @param stringKafkaTemplate KafkaTemplate for strings
     * @param storageApi          Storage API
     */
    public BinkpProtocolHandler(KafkaTemplate<String, NewReceiving> kafkaTemplate,
                                KafkaTemplate<String, String> stringKafkaTemplate,
                                StorageApi storageApi) {
        this.kafkaTemplate = kafkaTemplate;
        this.stringKafkaTemplate = stringKafkaTemplate;
        this.storageApi = storageApi;
    }

    /**
     * Receive mail from FTN networks
     *
     * @param inputStream InputStream from Socket
     * @param outputStream OutputStream to Socket
     * @param ftnAddress FTN address (x:xxxx/xxx.(xx))
     */
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

                while (readSize <= fileSize) {
                    BinkpFrame dataFrame = FrameHandler.readResponse(inputStream);
                    fileBytes = ArrayUtils.addAll(fileBytes, dataFrame.data());
                    readSize += dataFrame.length() - BINKP_FRAME_HEADER_SIZE;
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

    /**
     * Send mail to FTN station
     *
     * @param inputStream InputStream from Socket
     * @param outputStream OutputStream to Socket
     * @param ftnAddress FTN address of the station
     *                   (x:xxxx/xxx.(xx))
     */
    public void sendMail(InputStream inputStream, OutputStream outputStream, String ftnAddress) {
        String ftnAddressForStorageApi = ftnAddress.replaceAll("[:/.@]", "_");
        storageApi.list(ftnAddressForStorageApi).forEach(itemResult -> {
            byte[] fileBytes = storageApi.get(itemResult);
            int fileSize = fileBytes.length;
            String[] fileInfo = {itemResult, String.valueOf(fileSize),
                    String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)), "0"};

            FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_FILE, String.join(" ", fileInfo));

            int writtenSize = 0;

            while (writtenSize <= fileSize) {
                int dataFrameSize = Math.min(BINKP_FRAME_MAX_SIZE, fileSize - writtenSize);
                FrameHandler.sendDataFrame(outputStream, Arrays.copyOfRange(fileBytes, writtenSize, writtenSize + dataFrameSize));
                writtenSize += dataFrameSize;

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
