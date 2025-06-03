package ru.oldzoomer.fido.mailer.core;

import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.fido.mailer.constant.BinkpCommandType;
import ru.oldzoomer.fido.mailer.handler.FrameHandler;
import ru.oldzoomer.fido.mailer.model.BinkpFrame;
import ru.oldzoomer.fido.mailer.util.BinkpFrameUtil;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BinkpProtocolClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final String password;
    private final String ftnAddress;
    private final FrameHandler frameHandler;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    public BinkpProtocolClient(String host, int port, String password, String ftnAddress) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.ftnAddress = ftnAddress;
        this.frameHandler = new FrameHandler();
    }

    public void connect(Socket socket) throws IOException {
        log.info("Connecting to {}:{}", host, port);
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        sendConnectionInfo();
    }

    public Map<String, InputStream> transferMail(Map<String, InputStream> inputStreams) {
        sendMail(inputStreams);
        return receiveMail();
    }

    @Override
    public void close() throws IOException {
        log.info("Disconnecting from {}:{}", host, port);
        socket.close();
    }

    private void sendMail(Map<String, InputStream> inputStreams) {
        for (Map.Entry<String, InputStream> entry : inputStreams.entrySet()) {
            String fileName = entry.getKey();
            InputStream inputStream = entry.getValue();

            sendCommandFrame(BinkpCommandType.M_FILE, fileName);

            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    sendDataFrame(Arrays.copyOf(buffer, bytesRead));
                }
            } catch (IOException e) {
                log.error("Error sending file data", e);
            }

            sendCommandFrame(BinkpCommandType.M_EOB, "");
        }

        sendCommandFrame(BinkpCommandType.M_OK, "");
    }

    private Map<String, InputStream> receiveMail() {
        Map<String, InputStream> receivedMails = new HashMap<>();
        try {
            while (true) {
                BinkpFrame frame = readCommandFrame();
                if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_FILE) {
                    String fileName = BinkpFrameUtil.readCommandFrameString(frame);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    while (true) {
                        BinkpFrame dataFrame = readCommandFrame();
                        if (BinkpFrameUtil.getCommand(dataFrame) == BinkpCommandType.M_EOB) {
                            break;
                        }
                        outputStream.write(BinkpFrameUtil.toBytes(dataFrame));
                    }

                    receivedMails.put(fileName, new ByteArrayInputStream(outputStream.toByteArray()));
                } else if (BinkpFrameUtil.getCommand(frame) == BinkpCommandType.M_OK) {
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Error receiving mail", e);
        }
        return receivedMails;
    }

    private void sendConnectionInfo() throws IOException {
        sendCommandFrame(BinkpCommandType.M_ADR, ftnAddress);
        sendCommandFrame(BinkpCommandType.M_PWD, password);

        if (BinkpFrameUtil.getCommand(readCommandFrame()) != BinkpCommandType.M_OK) {
            throw new RuntimeException("Authentication failed");
        }
    }

    private void sendCommandFrame(BinkpCommandType command, String data) {
        try {
            frameHandler.sendCommandFrame(outputStream, command, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendDataFrame(byte[] data) {
        try {
            frameHandler.sendDataFrame(new ByteArrayOutputStream(), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BinkpFrame readCommandFrame() throws IOException {
        return BinkpFrameUtil.toFrame(inputStream.readAllBytes());
    }
}
