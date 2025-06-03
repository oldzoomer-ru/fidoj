package ru.oldzoomer.fido.mailer.constant;

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

    public static BinkpCommandType fromCode(int code) {
        return BinkpCommandType.values()[code];
    }
}
