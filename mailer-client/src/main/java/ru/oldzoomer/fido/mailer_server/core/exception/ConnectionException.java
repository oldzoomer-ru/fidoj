package ru.oldzoomer.fido.mailer_server.core.exception;

/**
 * Connection exception.
 *
 * @author oldzoomer
 */
public class ConnectionException extends RuntimeException {
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
