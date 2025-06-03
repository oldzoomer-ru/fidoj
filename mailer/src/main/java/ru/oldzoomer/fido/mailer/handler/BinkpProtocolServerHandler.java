package ru.oldzoomer.fido.mailer.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.service.AuthService;
import ru.oldzoomer.fido.mailer.util.BinkpFrameUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Slf4j
@Component
public class BinkpProtocolServerHandler {
    private final FrameHandler frameHandler = new FrameHandler();
    private final BinkpProtocolHandler binkpProtocolHandler;
    private final AuthService authService;

    public BinkpProtocolServerHandler(AuthService authService,
                                      BinkpProtocolHandler binkpProtocolHandler) {
        this.authService = authService;
        this.binkpProtocolHandler = binkpProtocolHandler;
    }

    public void handleClient(Socket clientSocket) {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            if (!authenticateClient(inputStream, outputStream)) {
                log.warn("Authentication failed for {}", clientSocket.getRemoteSocketAddress());
                return;
            }

            binkpProtocolHandler.receiveMail(inputStream, outputStream);
            binkpProtocolHandler.sendMail(inputStream, outputStream);
        } catch (Exception e) {
            log.error("Error handling client", e);
        }
    }

    private boolean authenticateClient(InputStream inputStream, OutputStream outputStream) {
        BinkpFrame addressFrame = frameHandler.readResponse(inputStream);
        BinkpFrame passwordFrame = frameHandler.readResponse(inputStream);

        if (BinkpFrameUtil.getCommand(addressFrame) == BinkpCommandType.M_ADR &&
                BinkpFrameUtil.getCommand(passwordFrame) == BinkpCommandType.M_PWD &&
                authService.authenticate(BinkpFrameUtil.readCommandFrameString(addressFrame),
                        BinkpFrameUtil.readCommandFrameString(passwordFrame))) {
            frameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_OK, "");
            return true;
        } else {
            frameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_ERR, "Incorrect password");
            return false;
        }
    }
}
