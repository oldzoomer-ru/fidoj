package ru.oldzoomer.fido.mailer.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.core.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.core.handler.BinkpProtocolHandler;
import ru.oldzoomer.fido.mailer.core.handler.FrameHandler;
import ru.oldzoomer.fido.mailer.core.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.core.util.BinkpFrameUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Slf4j
@Component
public class BinkpProtocolClient {
    private final BinkpProtocolHandler binkpProtocolHandler;

    public BinkpProtocolClient(BinkpProtocolHandler binkpProtocolHandler) {
        this.binkpProtocolHandler = binkpProtocolHandler;
    }

    public void transferMail(String host, int port, String ftnAddress, String password) {
        log.info("Connecting to {}:{}", host, port);
        try (Socket socket = new Socket(host, port);
             InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {

            if (!authenticateClient(inputStream, outputStream,
                    ftnAddress, password)) {
                log.warn("Authentication failed for {}", socket.getRemoteSocketAddress());
                throw new RuntimeException("Authentication failed");
            }

            binkpProtocolHandler.receiveMail(inputStream, outputStream, ftnAddress);
            binkpProtocolHandler.sendMail(inputStream, outputStream, ftnAddress);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean authenticateClient(InputStream inputStream, OutputStream outputStream,
                                       String ftnAddress, String password) {
        FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_ADR, ftnAddress);
        FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_PWD, password);

        BinkpFrame response = FrameHandler.readResponse(inputStream);
        return BinkpFrameUtil.getCommand(response) == BinkpCommandType.M_OK;
    }
}
