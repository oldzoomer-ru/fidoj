package ru.oldzoomer.fido.mailer.core.api;

public interface AuthApi {
    boolean authenticate(String nodeAddress, String password);
}
