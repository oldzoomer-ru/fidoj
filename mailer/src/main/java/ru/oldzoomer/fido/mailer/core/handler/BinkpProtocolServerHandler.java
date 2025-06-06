package ru.oldzoomer.fido.mailer.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.oldzoomer.fido.mailer.core.api.AuthApi;
import ru.oldzoomer.fido.mailer.core.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.core.exception.AuthenticationException;
import ru.oldzoomer.fido.mailer.core.exception.ConnectionException;
import ru.oldzoomer.fido.mailer.core.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.core.util.BinkpFrameUtil;

import java.io.IOException;
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
                throw new AuthenticationException("Authentication failed for " +
                        clientSocket.getRemoteSocketAddress());
            }

            binkpProtocolHandler.receiveMail(inputStream, outputStream, clientAddress);
            binkpProtocolHandler.sendMail(inputStream, outputStream, clientAddress);
        } catch (AuthenticationException e) {
            log.warn("Authentication failed: {}", e.getMessage());
            // TODO: Optionally send error response to client
        } catch (IOException e) {
            log.error("Connection error: {}", e.getMessage());
            throw new ConnectionException("Connection error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error handling client", e);
            throw new RuntimeException("Unexpected error handling client", e);
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
