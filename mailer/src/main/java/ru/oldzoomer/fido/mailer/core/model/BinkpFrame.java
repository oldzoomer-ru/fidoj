package ru.oldzoomer.fido.mailer.core.model;

import ru.oldzoomer.fido.mailer.core.constant.BinkpFrameType;

import static ru.oldzoomer.fido.mailer.core.constant.BinkpFrameSizes.BINKP_FRAME_HEADER_SIZE;

/**
 * Binkp frame in object form
 *
 * @param type   frame type
 * @param length frame length
 * @param data   frame data
 * @author oldzoomer
 */
public record BinkpFrame(BinkpFrameType type, int length, byte[] data) {
    public BinkpFrame(BinkpFrameType type, byte[] data) {
        this(type, data.length + BINKP_FRAME_HEADER_SIZE, data);
    }
}
