package ru.oldzoomer.fido.mailer.model;

import ru.oldzoomer.fido.mailer.constant.BinkpFrameType;

public record BinkpFrame(BinkpFrameType type, byte[] data) {
}
