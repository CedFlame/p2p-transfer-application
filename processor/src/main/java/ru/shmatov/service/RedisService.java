package ru.shmatov.service;

import ru.shmatov.enums.CodeVerificationResult;

public interface RedisService {
    void saveTransferCode(String username, Long transactionId, String code);
    CodeVerificationResult verifyTransferCode(String username, Long transactionId, String code);
    void deleteTransferCode(String username, Long transactionId);
}

