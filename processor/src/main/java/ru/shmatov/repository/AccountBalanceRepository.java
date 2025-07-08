package ru.shmatov.repository;

import ru.shmatov.model.AccountBalance;

import java.util.List;
import java.util.Optional;

public interface AccountBalanceRepository {
    List<AccountBalance> findAllByAccountId(Long accountId);
    Optional<AccountBalance> findByBalanceNumber(String balanceNumber);
    Optional<AccountBalance> findById(Long id);
    Long save(AccountBalance balance);
    String deleteById(Long id);
    void updateIsPrimary(Long balanceId, boolean isPrimary);
    void updateBalance(Long balanceId, Long amount);
}
