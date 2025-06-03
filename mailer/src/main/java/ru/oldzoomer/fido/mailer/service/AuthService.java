package ru.oldzoomer.fido.mailer.service;

public interface AuthService {
    boolean authenticate(String nodeAddress, String password);
}
