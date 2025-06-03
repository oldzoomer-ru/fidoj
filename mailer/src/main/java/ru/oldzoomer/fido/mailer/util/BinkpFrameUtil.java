package ru.oldzoomer.fido.mailer.util;

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

        int type = ((data[0] >> 7) & 1);
        int length = (data[0] & 0x7F);
        byte[] payload = new byte[length];
        System.arraycopy(data, 1, payload, 0, length);
        return new BinkpFrame(BinkpFrameType.fromValue(type), payload);
    }

    public static byte[] toBytes(BinkpFrame frame) {
        byte[] data = new byte[BINKP_FRAME_HEADER_SIZE + frame.data().length];
        data[0] = (byte) ((frame.type().getValue() << 7) | frame.data().length);
        System.arraycopy(frame.data(), 0, data, 1, frame.data().length);
        return data;
    }
}
