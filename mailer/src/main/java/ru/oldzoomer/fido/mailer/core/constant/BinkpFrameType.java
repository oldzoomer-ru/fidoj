package ru.oldzoomer.fido.mailer.core.constant;


/**
 * Binkp frame type
 *
 * @author oldzoomer
 */
public enum BinkpFrameType {
    BINKP_FRAME_TYPE_COMMAND,
    BINKP_FRAME_TYPE_DATA;

    /**
     * Get frame type by value
     *
     * @param value frame type value
     * @return frame type
     */
    public static BinkpFrameType fromValue(int value) {
        return BinkpFrameType.values()[value];
    }
}
