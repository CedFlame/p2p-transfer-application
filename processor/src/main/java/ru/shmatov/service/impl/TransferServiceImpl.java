package ru.shmatov.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import ru.shmatov.TransactionIdPairDTO;
import ru.shmatov.enums.CodeVerificationResult;
import ru.shmatov.enums.TransactionStatusEnum;
import ru.shmatov.exception.*;
import ru.shmatov.model.Account;
import ru.shmatov.model.AccountBalance;
import ru.shmatov.model.Transaction;
import ru.shmatov.repository.AccountBalanceRepository;
import ru.shmatov.repository.AccountRepository;
import ru.shmatov.repository.TransactionRepository;
import ru.shmatov.repository.UserRepository;
import ru.shmatov.response.APIResponse;
import ru.shmatov.response.TransferResponse;
import ru.shmatov.service.RedisService;
import ru.shmatov.service.TransactionService;
import ru.shmatov.service.TransferService;

import java.util.Objects;

import static ru.shmatov.util.ConfirmationCodeGenerator.generateCode;

@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final RedisService redisService;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransferResponse transfer(String username, Long amount, String fromBalanceNumber, String toBalanceNumber) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        Account sender = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        AccountBalance fromBalance = accountBalanceRepository.findAllByAccountId(sender.getId()).stream()
                .filter(b -> b.getBalanceNumber().equals(fromBalanceNumber))
                .findFirst()
                .orElseThrow(() -> new BalanceNotFoundException(fromBalanceNumber));

        if (fromBalance.getBalance() < amount) {
            throw new InsufficientFundsException(
                    "Not enough money on balance %s: need %d, have %d"
                            .formatted(fromBalanceNumber, amount, fromBalance.getBalance()));
        }

        TransactionIdPairDTO idPair = transactionService.create(username, amount, fromBalanceNumber, toBalanceNumber);

        String confirmationCode = generateCode();
        redisService.saveTransferCode(username, idPair.getId(), confirmationCode);

        transactionService.updateStatus(username, TransactionStatusEnum.PENDING_CONFIRMATION,
                TransactionStatusEnum.PENDING_CONFIRMATION, idPair);

        return TransferResponse.builder()
                .code(confirmationCode)
                .idPair(idPair)
                .build();
    }

    @Override
    @Transactional(noRollbackFor = InvalidConfirmationCodeException.class)
    public APIResponse processTransferConfirmation(String username, TransactionIdPairDTO idPair, String code) {
        if (!userRepository.existsByUsername(username)) {
            throw new UserNotFoundException(username);
        }

        Account sender = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        Transaction fromTx = transactionRepository.findById(idPair.getId())
                .orElseThrow(() -> new SenderTransactionNotFoundException(idPair.getId()));

        AccountBalance senderBalance = accountBalanceRepository.findById(fromTx.getBalanceId())
                .orElseThrow(() -> new BalanceNotFoundException("unknown balance"));

        boolean notOwned = accountBalanceRepository.findAllByAccountId(sender.getId()).stream()
                .noneMatch(b -> b.getId().equals(senderBalance.getId()));

        if (notOwned) {
            throw new SecurityBalanceNotBelongTransactionException(
                    "Transaction %d does not belong to balance %s — security error."
                            .formatted(idPair.getId(), senderBalance.getBalanceNumber()));
        }

        CodeVerificationResult result = redisService.verifyTransferCode(username, idPair.getId(), code);
        log.debug("Verification code check for user={} tx={} result={}", username, idPair.getId(), result);

        return switch (result) {
            case SUCCESS -> {
                APIResponse response = processTransfer(username, idPair);
                transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED,
                        TransactionStatusEnum.CONFIRMED, idPair);
                yield response;
            }
            case CODE_MISMATCH, CODE_NOT_FOUND -> {
                transactionService.updateStatus(username, TransactionStatusEnum.DECLINED,
                        TransactionStatusEnum.DECLINED, idPair);
                throw new InvalidConfirmationCodeException(code);
            }
            default -> throw new IllegalStateException("Unexpected verification result: " + result);
        };
    }

    private APIResponse processTransfer(String username, TransactionIdPairDTO idPair) {
        Transaction fromTx = transactionRepository.findById(idPair.getId())
                .orElseThrow(() -> new SenderTransactionNotFoundException(idPair.getId()));
        Transaction toTx = transactionRepository.findById(idPair.getMappedId())
                .orElseThrow(() -> new ReceiverTransactionNotFoundException(idPair.getMappedId()));

        AccountBalance fromBalance = accountBalanceRepository.findById(fromTx.getBalanceId())
                .orElseThrow(() -> new BalanceNotFoundException("From balance not found"));
        AccountBalance toBalance = accountBalanceRepository.findById(toTx.getBalanceId())
                .orElseThrow(() -> new BalanceNotFoundException("To balance not found"));

        boolean inconsistent = !Objects.equals(fromTx.getReceiverBalanceId(), toBalance.getId()) ||
                !Objects.equals(toTx.getReceiverBalanceId(), fromBalance.getId());

        if (inconsistent) {
            throw new SecurityBalanceNotBelongTransactionException(
                    "Transactions are not linked correctly — security error");
        }

        transactionService.updateStatus(username, TransactionStatusEnum.NO_ACTIVE,
                TransactionStatusEnum.NO_ACTIVE, idPair);

        accountBalanceRepository.updateBalance(fromBalance.getId(), fromTx.getAmount());
        accountBalanceRepository.updateBalance(toBalance.getId(), toTx.getAmount());

        return new APIResponse("Transfer completed successfully");
    }
}
