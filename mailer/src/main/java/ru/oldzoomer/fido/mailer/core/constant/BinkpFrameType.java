package ru.oldzoomer.fido.mailer.core.constant;

public enum BinkpFrameType {
    BINKP_FRAME_TYPE_COMMAND,
    BINKP_FRAME_TYPE_DATA;

    public static BinkpFrameType fromValue(int value) {
        return BinkpFrameType.values()[value];
    }

    public int getValue() {
        return ordinal();
    }
}
