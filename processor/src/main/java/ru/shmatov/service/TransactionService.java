package ru.shmatov.service;

import ru.shmatov.TransactionIdPairDTO;
import ru.shmatov.enums.TransactionStatusEnum;

public interface TransactionService {
    TransactionIdPairDTO create(String username, Long amount, String fromBalanceNumber, String toBalanceNumber);
    TransactionIdPairDTO updateStatus(String username, TransactionStatusEnum status, TransactionStatusEnum statusMapped, TransactionIdPairDTO idPair);
}
