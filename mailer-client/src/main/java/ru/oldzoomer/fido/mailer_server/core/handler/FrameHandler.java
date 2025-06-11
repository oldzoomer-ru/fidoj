package ru.oldzoomer.fido.mailer_server.core.handler;

import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.fido.mailer_server.core.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer_server.core.constant.BinkpFrameType;
import ru.oldzoomer.fido.mailer_server.core.model.BinkpFrame;
import ru.oldzoomer.fido.mailer_server.core.util.BinkpFrameUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static ru.oldzoomer.fido.mailer_server.core.constant.BinkpFrameSizes.BINKP_CHUNK_SIZE;
import static ru.oldzoomer.fido.mailer_server.core.constant.BinkpFrameSizes.BINKP_FRAME_FULL_SIZE;

/**
 * Handler for frames in binkp protocol
 *
 * @author oldzoomer
 */
@Slf4j
public class FrameHandler {
    /**
     * Send command frame
     *
     * @param outputStream OutputStream
     * @param commandType  Command type
     * @param data         Data for frame
     */
    public static void sendCommandFrame(OutputStream outputStream, BinkpCommandType commandType, String data) {
        BinkpFrame frame = BinkpFrameUtil.createCommandFrame(commandType, data);
        byte[] frameBytes = BinkpFrameUtil.toBytes(frame);
        try {
            int bytesWritten = 0;
            while (bytesWritten < frameBytes.length) {
                int chunkSize = Math.min(frameBytes.length - bytesWritten, BINKP_CHUNK_SIZE);
                outputStream.write(frameBytes, bytesWritten, chunkSize);
                bytesWritten += chunkSize;
            }
        } catch (IOException e) {
            log.error("Error sending command frame", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Send data frame
     *
     * @param outputStream OutputStream
     * @param data         Data for frame
     */
    public static void sendDataFrame(OutputStream outputStream, byte[] data) {
        BinkpFrame frame = new BinkpFrame(BinkpFrameType.BINKP_FRAME_TYPE_DATA, data);
        byte[] frameBytes = BinkpFrameUtil.toBytes(frame);
        try {
            int bytesWritten = 0;
            while (bytesWritten < frameBytes.length) {
                int chunkSize = Math.min(frameBytes.length - bytesWritten, BINKP_CHUNK_SIZE);
                outputStream.write(frameBytes, bytesWritten, chunkSize);
                bytesWritten += chunkSize;
            }
        } catch (IOException e) {
            log.error("Error sending data frame", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Send command frame
     *
     * @param inputStream InputStream
     * @return BinkpFrame
     */
    public static BinkpFrame readResponse(InputStream inputStream) {
        try {
            final int chunkSize = BINKP_FRAME_FULL_SIZE;
            byte[] bytes = new byte[chunkSize]; // Read in chunks
            int bytesRead;
            int allBytesRead = 0;

            while (allBytesRead < chunkSize && (bytesRead = inputStream.read(bytes, allBytesRead, chunkSize)) != -1) {
                allBytesRead += bytesRead;
            }

            return BinkpFrameUtil.toFrame(bytes);
        } catch (IOException e) {
            log.error("Error reading response", e);
            throw new RuntimeException(e);
        }
    }
}
