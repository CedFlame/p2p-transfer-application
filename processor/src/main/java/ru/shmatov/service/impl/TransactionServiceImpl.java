package ru.shmatov.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import ru.shmatov.TransactionIdPairDTO;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.enums.TransactionStatusEnum;
import ru.shmatov.enums.TransactionType;
import ru.shmatov.exception.*;
import ru.shmatov.model.Account;
import ru.shmatov.model.AccountBalance;
import ru.shmatov.model.Transaction;
import ru.shmatov.model.User;
import ru.shmatov.repository.AccountBalanceRepository;
import ru.shmatov.repository.AccountRepository;
import ru.shmatov.repository.TransactionRepository;
import ru.shmatov.repository.UserRepository;
import ru.shmatov.service.TransactionService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final UserRepository userRepository;

    private AccountBalance doPrepare(String username, String fromBalanceNumber) {
        if (!userRepository.existsByUsername(username)) {
            throw new UserNotFoundException(username);
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        List<AccountBalance> balances = accountBalanceRepository.findAllByAccountId(account.getId());

        return balances.stream()
                .filter(b -> b.getBalanceNumber().equals(fromBalanceNumber))
                .findFirst()
                .orElseThrow(() -> new BalanceNotFoundException(fromBalanceNumber));
    }

    @Override
    @Transactional
    @LogExecutionTime
    public TransactionIdPairDTO create(String username, Long amount, String fromBalanceNumber, String toBalanceNumber) {
        AccountBalance fromAccountBalance = doPrepare(username, fromBalanceNumber);
        AccountBalance toAccountBalance = accountBalanceRepository.findByBalanceNumber(toBalanceNumber)
                .orElseThrow(() -> new BalanceNotFoundException(toBalanceNumber));

        long now = System.currentTimeMillis();

        Long fromTransactionId = transactionRepository.save(
                Transaction.builder()
                        .createdAt(now)
                        .amount(amount * -1)
                        .transactionType(TransactionType.TRANSFER_TO)
                        .transactionStatus(TransactionStatusEnum.CREATED)
                        .balanceId(fromAccountBalance.getId())
                        .receiverBalanceId(toAccountBalance.getId())
                        .build()
        );

        Long toTransactionId = transactionRepository.save(
                Transaction.builder()
                        .createdAt(now)
                        .amount(amount)
                        .transactionType(TransactionType.TRANSFER_FROM)
                        .transactionStatus(TransactionStatusEnum.CREATED)
                        .balanceId(toAccountBalance.getId())
                        .receiverBalanceId(fromAccountBalance.getId())
                        .build()
        );

        transactionRepository.updateReceiverTransactionId(fromTransactionId, toTransactionId);
        transactionRepository.updateReceiverTransactionId(toTransactionId, fromTransactionId);

        log.debug("Created transaction pair: fromId={}, toId={}, amount={}, fromBalance={}, toBalance={}",
                fromTransactionId, toTransactionId, amount, fromBalanceNumber, toBalanceNumber);

        return new TransactionIdPairDTO(fromTransactionId, toTransactionId);
    }

    @Override
    @Transactional
    @LogExecutionTime
    public TransactionIdPairDTO updateStatus(String username, TransactionStatusEnum status, TransactionStatusEnum statusMapped, TransactionIdPairDTO idPair) {
        Transaction fromTransaction = transactionRepository.findById(idPair.getId())
                .orElseThrow(() -> new SenderTransactionNotFoundException(idPair.getId()));

        Transaction toTransaction = transactionRepository.findById(idPair.getMappedId())
                .orElseThrow(() -> new ReceiverTransactionNotFoundException(idPair.getMappedId()));

        AccountBalance fromBalance = accountBalanceRepository.findById(fromTransaction.getBalanceId())
                .orElseThrow(() -> new BalanceNotFoundException("unknown balance"));

        AccountBalance toBalance = accountBalanceRepository.findById(toTransaction.getBalanceId())
                .orElseThrow(() -> new BalanceNotFoundException("unknown balance"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Account sender = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        List<AccountBalance> balances = accountBalanceRepository.findAllByAccountId(sender.getId());

        boolean notOwned = balances.stream()
                .noneMatch(b -> b.getId().equals(fromBalance.getId()));

        if (notOwned) {
            throw new SecurityBalanceNotBelongTransactionException(
                    "Transaction " + idPair.getId() + " does not belong to balance " +
                            fromBalance.getBalanceNumber() + " — security error. All changes have been reverted."
            );
        }

        transactionRepository.updateStatus(idPair.getId(), status);
        transactionRepository.updateStatus(idPair.getMappedId(), statusMapped);

        log.debug("Updated transaction statuses: id={} => {}, mappedId={} => {}",
                idPair.getId(), status, idPair.getMappedId(), statusMapped);

        return idPair;
    }
}
