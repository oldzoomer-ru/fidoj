package ru.oldzoomer.fido.mailer.core;

import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.fido.mailer.core.handler.BinkpProtocolServerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class BinkpProtocolServer implements AutoCloseable {
    private final int port;
    private final BinkpProtocolServerHandler handler;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    public BinkpProtocolServer(int port, BinkpProtocolServerHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("Server started on port {}", port);

            executorService = Executors.newCachedThreadPool();

            while (serverSocket.isBound() && !serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            log.error("Error starting server", e);
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket) {
            handler.handleClient(clientSocket);
        } catch (IOException e) {
            log.error("Error closing client socket", e);
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
            executorService.shutdown(); // Gracefully shutdown the executor service
        }
    }
}
