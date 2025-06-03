package ru.oldzoomer.fido.mailer.core.api;

import java.util.List;

public interface StorageApi {
    void save(byte[] bytes, int fileSize, String filePath);

    byte[] get(String filePath);

    void delete(String filePath);

    boolean exists(String filePath);

    List<String> list(String filePath);
}
