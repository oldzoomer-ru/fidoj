package ru.oldzoomer.fido.mailer.core.model;

/**
 * New receiving
 *
 * @param ftnAddress     FTN address
 * @param storageApiPath path to file in Storage API
 * @author oldzoomer
 */
public record NewReceiving(String ftnAddress, String storageApiPath) {
}
