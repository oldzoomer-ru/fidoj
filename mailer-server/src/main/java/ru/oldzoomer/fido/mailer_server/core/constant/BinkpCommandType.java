package ru.oldzoomer.fido.mailer_server.core.constant;

/**
 * Binkp command type
 *
 * @author oldzoomer
 */
public enum BinkpCommandType {
    M_NUL,
    M_ADR,
    M_PWD,
    M_OK,
    M_FILE,
    M_EOB,
    M_GOT,
    M_ERR,
    M_BSY,
    M_GET,
    M_SKIP;

    /**
     * Get command type by code
     *
     * @param code command code
     * @return command type
     */
    public static BinkpCommandType fromCode(int code) {
        return BinkpCommandType.values()[code];
    }
}
