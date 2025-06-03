package ru.oldzoomer.fido.mailer.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.core.api.AuthApi;
import ru.oldzoomer.fido.mailer.core.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.core.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.core.util.BinkpFrameUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Slf4j
@Component
public class BinkpProtocolServerHandler {
    private final BinkpProtocolHandler binkpProtocolHandler;
    private final AuthApi authApi;

    public BinkpProtocolServerHandler(AuthApi authApi,
                                      BinkpProtocolHandler binkpProtocolHandler) {
        this.authApi = authApi;
        this.binkpProtocolHandler = binkpProtocolHandler;
    }

    public void handleClient(Socket clientSocket) {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String clientAddress = authenticateClient(inputStream, outputStream);
            if (clientAddress == null) {
                log.warn("Authentication failed for {}", clientSocket.getRemoteSocketAddress());
                return;
            }

            binkpProtocolHandler.receiveMail(inputStream, outputStream, clientAddress);
            binkpProtocolHandler.sendMail(inputStream, outputStream, clientAddress);
        } catch (Exception e) {
            log.error("Error handling client", e);
        }
    }

    private String authenticateClient(InputStream inputStream, OutputStream outputStream) {
        BinkpFrame addressFrame = FrameHandler.readResponse(inputStream);
        BinkpFrame passwordFrame = FrameHandler.readResponse(inputStream);

        if (BinkpFrameUtil.getCommand(addressFrame) == BinkpCommandType.M_ADR &&
                BinkpFrameUtil.getCommand(passwordFrame) == BinkpCommandType.M_PWD &&
                authApi.authenticate(BinkpFrameUtil.readCommandFrameString(addressFrame),
                        BinkpFrameUtil.readCommandFrameString(passwordFrame))) {
            FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_OK, "");
            return BinkpFrameUtil.readCommandFrameString(addressFrame);
        } else {
            FrameHandler.sendCommandFrame(outputStream, BinkpCommandType.M_ERR, "Incorrect password");
            return null;
        }
    }
}
