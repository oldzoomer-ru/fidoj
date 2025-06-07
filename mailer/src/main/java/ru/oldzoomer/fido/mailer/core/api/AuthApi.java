package ru.oldzoomer.fido.mailer.core.api;

/**
 * Authentication API.
 *
 * @author oldzoomer
 */
public interface AuthApi {
    /**
     * Authenticate user.
     *
     * @param nodeAddress Node address.
     * @param password    Password.
     * @return True if authentication was successful.
     */
    boolean authenticate(String nodeAddress, String password);
}
