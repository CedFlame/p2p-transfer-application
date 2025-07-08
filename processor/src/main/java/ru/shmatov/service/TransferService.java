package ru.shmatov.service;

import ru.shmatov.TransactionIdPairDTO;
import ru.shmatov.response.APIResponse;
import ru.shmatov.response.TransferResponse;

public interface TransferService {
    TransferResponse transfer(String username, Long amount, String fromBalanceNumber, String toBalanceNumber);
    APIResponse processTransferConfirmation(String username, TransactionIdPairDTO idPair, String code);
}
