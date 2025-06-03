package ru.oldzoomer.fido.mailer.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.handler.BinkpProtocolHandler;
import ru.oldzoomer.fido.mailer.handler.FrameHandler;
import ru.oldzoomer.fido.mailer.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.util.BinkpFrameUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Slf4j
@Component
public class BinkpProtocolClient implements AutoCloseable {
    private final FrameHandler frameHandler;
    private final BinkpProtocolHandler binkpProtocolHandler;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private String host;
    private int port;
    private String ftnAddress;
    private String password;

    public BinkpProtocolClient(BinkpProtocolHandler binkpProtocolHandler,
                               FrameHandler frameHandler) {
        this.binkpProtocolHandler = binkpProtocolHandler;
        this.frameHandler = frameHandler;
    }

    public void connect(String host, int port, String ftnAddress, String password) throws IOException {
        log.info("Connecting to {}:{}", host, port);
        this.socket = new Socket(host, port);
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.host = host;
        this.port = port;
        this.ftnAddress = ftnAddress;
        this.password = password;

        if (!authenticateClient(inputStream, outputStream)) {
            log.warn("Authentication failed for {}", socket.getRemoteSocketAddress());
            throw new RuntimeException("Authentication failed");
        }
    }

    public void transferMail() {
        log.info("Transferring mail to {}:{}", host, port);
        binkpProtocolHandler.receiveMail(inputStream, outputStream);
        binkpProtocolHandler.sendMail(inputStream, outputStream);
    }

    public void disconnect() {
        log.info("Disconnecting from {}:{}", host, port);
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.host = null;
        this.port = 0;
        this.ftnAddress = null;
        this.password = null;
    }

    @Override
    public void close() {
        disconnect();
    }

    private boolean authenticateClient(InputStream inputStream, OutputStream outputStream) {
        frameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_ADR, ftnAddress);
        frameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_PWD, password);

        BinkpFrame response = frameHandler.readResponse(inputStream);
        return BinkpFrameUtil.getCommand(response) == BinkpCommandType.M_OK;
    }
}
