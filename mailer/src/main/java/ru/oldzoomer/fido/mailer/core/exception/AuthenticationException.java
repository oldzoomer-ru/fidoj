package ru.oldzoomer.fido.mailer.core.exception;

/**
 * Authentication exception.
 *
 * @author oldzoomer
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(String message) {
        super(message);
    }
}
