package ru.oldzoomer.fido.mailer.model;

import ru.oldzoomer.fido.mailer.constant.BinkpFrameType;

public record BinkpFrame(BinkpFrameType type, int length, byte[] data) {
    public BinkpFrame(BinkpFrameType type, byte[] data) {
        this(type, data.length + 2, data);
    }
}
