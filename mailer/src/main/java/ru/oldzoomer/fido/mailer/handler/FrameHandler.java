package ru.oldzoomer.fido.mailer.handler;

import ru.oldzoomer.fido.mailer.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.constant.BinkpFrameType;
import ru.oldzoomer.fido.mailer.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.util.BinkpFrameUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FrameHandler {

    public void sendCommandFrame(OutputStream outputStream, BinkpCommandType commandType, String data) throws IOException {
        BinkpFrame frame = BinkpFrameUtil.createCommandFrame(commandType, data);
        byte[] frameBytes = BinkpFrameUtil.toBytes(frame);
        outputStream.write(frameBytes);
    }

    public void sendDataFrame(OutputStream outputStream, byte[] data) throws IOException {
        BinkpFrame frame = new BinkpFrame(BinkpFrameType.BINKP_FRAME_TYPE_DATA, data);
        byte[] frameBytes = BinkpFrameUtil.toBytes(frame);
        outputStream.write(frameBytes);
    }

    public BinkpFrame readResponse(InputStream inputStream) throws IOException {
        return BinkpFrameUtil.toFrame(inputStream.readAllBytes());
    }
}