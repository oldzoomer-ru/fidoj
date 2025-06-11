package ru.oldzoomer.fido.mailer_server.core.constant;

/**
 * Binkp frame sizes
 *
 * @author oldzoomer
 */
public class BinkpFrameSizes {
    public static final int BINKP_FRAME_HEADER_SIZE = 2;
    public static final int BINKP_FRAME_MAX_SIZE = 32767;
    public static final int BINKP_FRAME_FULL_SIZE = BINKP_FRAME_HEADER_SIZE + BINKP_FRAME_MAX_SIZE;
    public static final int BINKP_CHUNK_SIZE = 1024;
}
