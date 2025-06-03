package ru.oldzoomer.fido.mailer.util;

import org.apache.commons.lang3.ArrayUtils;
import ru.oldzoomer.fido.mailer.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.constant.BinkpFrameType;
import ru.oldzoomer.fido.mailer.model.BinkpFrame;

import java.nio.charset.StandardCharsets;

public class BinkpFrameUtil {
    public static final int BINKP_FRAME_HEADER_SIZE = 2;
    public static final int BINKP_FRAME_MAX_SIZE = 32767;

    public static BinkpFrame createCommandFrame(BinkpCommandType command,
                                                String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.US_ASCII);
        byte[] commandBytes = new byte[textBytes.length + 1];
        commandBytes[0] = (byte) command.ordinal();
        System.arraycopy(textBytes, 0, commandBytes, 1, textBytes.length);
        return new BinkpFrame(BinkpFrameType.BINKP_FRAME_TYPE_COMMAND, commandBytes);
    }

    public static String readCommandFrameString(BinkpFrame frame) {
        byte[] data = frame.data();
        return new String(data, 1, data.length - 1, StandardCharsets.US_ASCII);
    }

    public static BinkpCommandType getCommand(BinkpFrame frame) {
        if (frame.type() != BinkpFrameType.BINKP_FRAME_TYPE_COMMAND) {
            throw new IllegalArgumentException("Frame is not a command");
        }
        return BinkpCommandType.fromCode(frame.data()[0]);
    }

    public static BinkpFrame toFrame(byte[] data) {
        if (data.length > BINKP_FRAME_MAX_SIZE) {
            throw new IllegalArgumentException("Binkp frame size must be less than " + BINKP_FRAME_MAX_SIZE);
        }

        // length is 2 octets, but first bit in first octet is type
        // so we only have 15 bits for length
        int type = (data[0] & 0x80) >> 7;
        int length = ((data[0] & 0x7F) << 8) | (data[1] & 0xFF);
        if (length > BINKP_FRAME_MAX_SIZE - BINKP_FRAME_HEADER_SIZE) {
            throw new IllegalArgumentException("Binkp frame size must be less than " + BINKP_FRAME_MAX_SIZE);
        }
        byte[] payload = new byte[length];
        System.arraycopy(data, 1, payload, 0, length);
        return new BinkpFrame(BinkpFrameType.fromValue(type), payload);
    }

    public static byte[] toBytes(BinkpFrame frame) {
        byte length = (byte) frame.length();
        byte[] header = new byte[BINKP_FRAME_HEADER_SIZE];
        // first bit is type, next 7 bits are length
        header[0] = (byte) ((frame.type().ordinal() << 7) | ((length >> 8) & 0x7F));
        header[1] = (byte) (length & 0xFF);
        return ArrayUtils.addAll(header, frame.data());
    }
}
