package ru.shmatov.service;

import ru.shmatov.TransactionIdPairDTO;

import java.util.Optional;

public interface RedisService {

    void saveJwt(String tgId, String jwt);

    Optional<String> getJwt(String tgId);

    void deleteJwt(String tgId);

    void saveTxPair(String tgId, TransactionIdPairDTO pair);

    Optional<TransactionIdPairDTO> getTxPair(String tgId);

    void deleteTxPair(String tgId);
}


