package ru.oldzoomer.fido.mailer.core.auth;

public interface AuthService {
    boolean authenticate(String nodeAddress, String password);
}
