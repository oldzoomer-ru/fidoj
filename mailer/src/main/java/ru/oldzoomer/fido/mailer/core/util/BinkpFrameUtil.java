package ru.oldzoomer.fido.mailer.core.util;

import org.apache.commons.lang3.ArrayUtils;
import ru.oldzoomer.fido.mailer.core.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.core.constant.BinkpFrameType;
import ru.oldzoomer.fido.mailer.core.model.BinkpFrame;

import java.nio.charset.StandardCharsets;

import static ru.oldzoomer.fido.mailer.core.constant.BinkpFrameSizes.BINKP_FRAME_HEADER_SIZE;
import static ru.oldzoomer.fido.mailer.core.constant.BinkpFrameSizes.BINKP_FRAME_MAX_SIZE;


/**
 * Binkp frame util.
 *
 * @author oldzoomer
 */
public class BinkpFrameUtil {
    /**
     * Create command frame.
     *
     * @param command command type
     * @param text    text to send
     * @return Binkp frame object
     */
    public static BinkpFrame createCommandFrame(BinkpCommandType command,
                                                String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.US_ASCII);
        byte[] commandBytes = new byte[textBytes.length + 1];
        commandBytes[0] = (byte) command.ordinal();
        System.arraycopy(textBytes, 0, commandBytes, 1, textBytes.length);
        return new BinkpFrame(BinkpFrameType.BINKP_FRAME_TYPE_COMMAND, commandBytes);
    }

    /**
     * Create response frame.
     *
     * @param frame Binkp frame object
     * @return command frame string
     */
    public static String readCommandFrameString(BinkpFrame frame) {
        byte[] data = frame.data();
        return new String(data, 1, data.length - 1, StandardCharsets.US_ASCII);
    }

    /**
     * Get command from frame
     *
     * @param frame Binkp frame object
     * @return command code
     */
    public static BinkpCommandType getCommand(BinkpFrame frame) {
        if (frame.type() != BinkpFrameType.BINKP_FRAME_TYPE_COMMAND) {
            throw new IllegalArgumentException("Frame is not a command");
        }
        return BinkpCommandType.fromCode(frame.data()[0]);
    }

    /**
     * Create frame.
     *
     * @param data frame data
     * @return Binkp frame object
     */
    public static BinkpFrame toFrame(byte[] data) {
        if (data.length > BINKP_FRAME_MAX_SIZE) {
            throw new IllegalArgumentException("Binkp frame size must be less than " + BINKP_FRAME_MAX_SIZE);
        }

        // length is 2 octets, but first bit in first octet is type
        // so we only have 15 bits for length of the data
        int type = (data[0] & 0x80) >> 7;
        int length = ((data[0] & 0x7F) << 8) | (data[1] & 0xFF);
        if (length > BINKP_FRAME_MAX_SIZE - BINKP_FRAME_HEADER_SIZE) {
            throw new IllegalArgumentException("Binkp frame size must be less than " + BINKP_FRAME_MAX_SIZE);
        }
        byte[] payload = new byte[length];
        System.arraycopy(data, 1, payload, 0, length);
        return new BinkpFrame(BinkpFrameType.fromValue(type), payload);
    }

    /**
     * Converts a BinkpFrame to byte array
     *
     * @param frame the BinkpFrame to convert
     * @return the byte array
     */
    public static byte[] toBytes(BinkpFrame frame) {
        byte length = (byte) frame.length();
        byte[] header = new byte[BINKP_FRAME_HEADER_SIZE];
        // first bit is type, next 7 bits are length of data
        header[0] = (byte) ((frame.type().ordinal() << 7) | ((length >> 8) & 0x7F));
        header[1] = (byte) (length & 0xFF);
        return ArrayUtils.addAll(header, frame.data());
    }
}
