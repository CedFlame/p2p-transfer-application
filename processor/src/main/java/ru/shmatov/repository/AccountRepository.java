package ru.shmatov.repository;

import ru.shmatov.model.Account;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByUserId(Long userId);
    Optional<Account> findByAccountNumber(String accountNumber);
    Long save(Account account);
    String deleteByUserUsername(String username);
}