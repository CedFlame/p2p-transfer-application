package ru.shmatov.repository;

import ru.shmatov.enums.TransactionStatusEnum;
import ru.shmatov.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Optional<Transaction> findById(Long id);
    List<Transaction> findAllByBalanceId(Long balanceId);
    Long save(Transaction transaction);
    void updateStatus(Long transactionId, TransactionStatusEnum newStatus);
    void updateReceiverTransactionId(Long transactionId, Long receiverTransactionId);
    boolean existsById(Long id);
}
