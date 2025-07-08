package ru.shmatov.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import ru.shmatov.AccountAndBalancesPairDTO;
import ru.shmatov.AccountBalanceDTO;
import ru.shmatov.AccountMasterBalanceNumberPairDTO;
import ru.shmatov.TransactionDTO;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.exception.*;
import ru.shmatov.model.Account;
import ru.shmatov.model.AccountBalance;
import ru.shmatov.model.Transaction;
import ru.shmatov.model.User;
import ru.shmatov.repository.AccountBalanceRepository;
import ru.shmatov.repository.AccountRepository;
import ru.shmatov.repository.TransactionRepository;
import ru.shmatov.repository.UserRepository;
import ru.shmatov.request.AccountCreateRequest;
import ru.shmatov.response.AccountViewResponse;
import ru.shmatov.service.AccountService;
import ru.shmatov.util.AccountNumberGenerator;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionRepository transactionRepository;

    private TransactionDTO mapToTransactionDTO(Transaction tx) {
        var sender = accountBalanceRepository.findById(tx.getBalanceId())
                .orElseThrow(() -> new BalanceNotFoundException("sender"));
        var receiver = accountBalanceRepository.findById(tx.getReceiverBalanceId())
                .orElseThrow(() -> new BalanceNotFoundException("receiver"));
        return TransactionDTO.builder()
                .id(tx.getId())
                .transactionType(tx.getTransactionType())
                .transactionStatus(tx.getTransactionStatus())
                .senderBalanceNumber(sender.getBalanceNumber())
                .receiverBalanceNumber(receiver.getBalanceNumber())
                .amount(tx.getAmount())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private AccountBalanceDTO mapToAccountBalanceDTO(AccountBalance b,
                                                     String accountNumber,
                                                     List<TransactionDTO> txs) {
        return AccountBalanceDTO.builder()
                .balanceNumber(b.getBalanceNumber())
                .accountNumber(accountNumber)
                .balance(b.getBalance())
                .createdAt(b.getCreatedAt())
                .isPrimary(b.getIsPrimary())
                .transactions(txs)
                .build();
    }

    @Override
    @Transactional
    @LogExecutionTime
    public AccountMasterBalanceNumberPairDTO create(AccountCreateRequest req) {
        String username = req.getUserUsername();

        if (!userRepository.existsByUsername(username))
            throw new UserNotFoundException(username);
        if (accountRepository.findByUsername(username).isPresent())
            throw new AccountAlreadyExistsException(username);

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username))
                .getId();

        String accountNumber;
        int retry = 0;
        do {
            accountNumber = AccountNumberGenerator.generateAccountNumber();
            retry++;
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent() && retry < 5);

        Account account = Account.builder()
                .userId(userId)
                .userUsername(username)
                .userTelegramUsername(req.getUserTelegramUsername())
                .accountNumber(accountNumber)
                .build();

        Long accountId = accountRepository.save(account);

        long now = System.currentTimeMillis();
        String masterBalanceNumber = AccountNumberGenerator.generateBalanceNumber(accountNumber, 1);
        AccountBalance masterBalance = AccountBalance.builder()
                .accountId(accountId)
                .balanceNumber(masterBalanceNumber)
                .isPrimary(true)
                .createdAt(now)
                .balance(req.getInitialBalance())
                .build();

        accountBalanceRepository.save(masterBalance);

        log.info("Account {} created for user {}", accountNumber, username);
        return new AccountMasterBalanceNumberPairDTO(accountNumber, masterBalanceNumber);
    }

    @Override
    @Transactional
    @LogExecutionTime
    public AccountAndBalancesPairDTO delete(String username) {
        if (!userRepository.existsByUsername(username))
            throw new UserNotFoundException(username);

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        List<AccountBalance> balances = accountBalanceRepository.findAllByAccountId(account.getId());
        if (balances.stream().mapToLong(AccountBalance::getBalance).sum() != 0)
            throw new AccountNotEmptyException(account.getAccountNumber());
        balances.forEach(b -> {
            if (b.getBalance() != 0) throw new BalanceNotEmptyException(b.getBalanceNumber());
        });

        List<String> numbers = new ArrayList<>();
        balances.forEach(b -> numbers.add(accountBalanceRepository.deleteById(b.getId())));
        String accountNumber = accountRepository.deleteByUserUsername(username);

        log.info("Account {} deleted for user {}", accountNumber, username);
        return new AccountAndBalancesPairDTO(accountNumber, numbers);
    }

    @Override
    @LogExecutionTime
    public AccountViewResponse getAccountView(String username) {
        if (!userRepository.existsByUsername(username))
            throw new UserNotFoundException(username);

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        List<AccountBalance> balances = accountBalanceRepository.findAllByAccountId(account.getId());
        List<AccountBalanceDTO> dtoBalances = balances.stream()
                .map(b -> {
                    List<TransactionDTO> txs = transactionRepository
                            .findAllByBalanceId(b.getId()).stream()
                            .map(this::mapToTransactionDTO)
                            .toList();
                    return mapToAccountBalanceDTO(b, account.getAccountNumber(), txs);
                })
                .toList();

        long total = balances.stream().mapToLong(AccountBalance::getBalance).sum();
        return AccountViewResponse.builder()
                .accountNumber(account.getAccountNumber())
                .userUsername(account.getUserUsername())
                .userTelegramUsername(account.getUserTelegramUsername())
                .balance(total)
                .balances(dtoBalances)
                .build();
    }

    @Override
    @LogExecutionTime
    public String deleteBalance(String username, String balanceNumber) {
        if (!userRepository.existsByUsername(username))
            throw new UserNotFoundException(username);

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        List<AccountBalance> balances = accountBalanceRepository.findAllByAccountId(account.getId());
        AccountBalance target = balances.stream()
                .filter(b -> b.getBalanceNumber().equals(balanceNumber))
                .findFirst()
                .orElseThrow(() -> new BalanceNotFoundException(balanceNumber));

        if (balances.size() == 1)
            throw new OnlyOneBalanceException(balanceNumber);
        if (target.getBalance() != 0)
            throw new BalanceNotEmptyException(balanceNumber);
        if (target.getIsPrimary()) {
            throw new CantDeletePrimaryAccountException(target.getBalanceNumber());
        }

        String num = accountBalanceRepository.deleteById(target.getId());
        log.info("Balance {} deleted for user {}", num, username);
        return num;
    }

    @Override
    @LogExecutionTime
    public String createBalance(String username, long initialBalance) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        List<AccountBalance> balances = accountBalanceRepository.findAllByAccountId(account.getId());
        if (balances.size() >= user.getBalanceCountLimit())
            throw new BalanceLimitExceededException(username);

        int nextIndex = balances.stream()
                .map(balance -> balance.getBalanceNumber())
                .filter(number -> number.length() == 20)
                .mapToInt(number -> Character.getNumericValue(number.charAt(19)))
                .max()
                .orElse(-1) + 1;

        String balanceNumber = AccountNumberGenerator.generateBalanceNumber(
                account.getAccountNumber(), nextIndex);


        if (balances.stream().anyMatch(b -> b.getBalanceNumber().equals(balanceNumber)))
            throw new DuplicateBalanceNumberException(balanceNumber);

        AccountBalance newBalance = AccountBalance.builder()
                .accountId(account.getId())
                .balanceNumber(balanceNumber)
                .isPrimary(false)
                .createdAt(System.currentTimeMillis())
                .balance(initialBalance)
                .build();

        accountBalanceRepository.save(newBalance);
        log.info("Balance {} created for user {}", balanceNumber, username);
        return balanceNumber;
    }

    @Override
    @Transactional
    @LogExecutionTime
    public String switchPrimaryBalance(String username, String balanceNumber) {
        if (!userRepository.existsByUsername(username))
            throw new UserNotFoundException(username);

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        List<AccountBalance> balances = accountBalanceRepository.findAllByAccountId(account.getId());
        AccountBalance newPrimary = balances.stream()
                .filter(b -> b.getBalanceNumber().equals(balanceNumber))
                .findFirst()
                .orElseThrow(() -> new BalanceNotFoundException(balanceNumber));

        if (newPrimary.getIsPrimary())
            throw new AlreadyPrimaryBalanceException(balanceNumber);

        AccountBalance currentPrimary = balances.stream()
                .filter(AccountBalance::getIsPrimary)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Primary balance not found"));

        accountBalanceRepository.updateIsPrimary(currentPrimary.getId(), false);
        accountBalanceRepository.updateIsPrimary(newPrimary.getId(), true);

        log.info("Primary balance switched from {} to {} for user {}",
                currentPrimary.getBalanceNumber(), balanceNumber, username);
        return currentPrimary.getBalanceNumber();
    }
}
