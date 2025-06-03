package ru.oldzoomer.fido.mailer.core;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.fido.mailer.handler.BinkpProtocolHandler;
import ru.oldzoomer.fido.mailer.service.AuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class BinkpProtocolServer implements AutoCloseable {
    private final int port;
    private final AuthService authService;
    private final MinioClient minioClient;
    private final String bucketName;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    public BinkpProtocolServer(int port, AuthService authService,
                               MinioClient minioClient, String bucketName) {
        this.port = port;
        this.authService = authService;
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("Server started on port {}", port);

            executorService = Executors.newCachedThreadPool();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            log.error("Error starting server", e);
        }
    }

    private void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket) {
            BinkpProtocolHandler handler = new BinkpProtocolHandler(
                    authService, minioClient, bucketName);
            handler.handleClient(socket);
        } catch (IOException e) {
            log.error("Error handling client", e);
        }
    }

    @Override
    public void close() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
