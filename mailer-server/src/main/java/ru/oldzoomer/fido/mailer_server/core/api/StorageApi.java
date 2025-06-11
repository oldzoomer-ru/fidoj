package ru.oldzoomer.fido.mailer_server.core.api;

import java.util.List;

/**
 * Storage API.
 *
 * @author oldzoomer
 */
public interface StorageApi {
    /**
     * Save file to storage.
     *
     * @param bytes    data
     * @param fileSize file size
     * @param filePath file path
     */
    void save(byte[] bytes, int fileSize, String filePath);

    /**
     * Get file from storage.
     *
     * @param filePath file path
     * @return data
     */
    byte[] get(String filePath);

    /**
     * Delete file from storage.
     *
     * @param filePath file path
     */
    void delete(String filePath);

    /**
     * Get file list from path in storage.
     * @param filePath file path
     * @return file list
     */
    List<String> list(String filePath);
}
