package ru.shmatov.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.model.AccountBalance;
import ru.shmatov.repository.AccountBalanceRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class AccountBalanceRepositoryImpl implements AccountBalanceRepository {

    private final JdbcTemplate jdbc;
    private final RowMapper<AccountBalance> mapper = (rs, n) -> mapBalance(rs);

    @Override
    public List<AccountBalance> findAllByAccountId(Long accountId) {
        return jdbc.query("SELECT * FROM account_balances WHERE account_id = ?", mapper, accountId);
    }

    @Override
    public Optional<AccountBalance> findByBalanceNumber(String balanceNumber) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(
                            "SELECT * FROM account_balances WHERE balance_number = ?",
                            mapper, balanceNumber));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AccountBalance> findById(Long id) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(
                            "SELECT * FROM account_balances WHERE id = ?",
                            mapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @LogExecutionTime
    @Override
    public Long save(AccountBalance balance) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO account_balances (account_id, balance, is_primary, created_at, balance_number) VALUES (?,?,?,?,?)",
                    new String[]{"id"});
            ps.setLong(1, balance.getAccountId());
            ps.setLong(2, balance.getBalance());
            ps.setBoolean(3, balance.getIsPrimary());
            ps.setLong(4, balance.getCreatedAt());
            ps.setString(5, balance.getBalanceNumber());
            return ps;
        }, keyHolder);

        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        log.info("Account balance saved with id={}", id);
        return id;
    }

    @LogExecutionTime
    @Override
    public String deleteById(Long id) {
        String balanceNumber = jdbc.queryForObject(
                "SELECT balance_number FROM account_balances WHERE id = ?",
                String.class,
                id
        );

        jdbc.update("DELETE FROM account_balances WHERE id = ?", id);
        log.info("Deleted balance with id={}, number={}", id, balanceNumber);
        return balanceNumber;
    }

    @Override
    public void updateIsPrimary(Long balanceId, boolean isPrimary) {
        jdbc.update(
                "UPDATE account_balances SET is_primary = ? WHERE id = ?",
                isPrimary, balanceId
        );
        log.info("Updated primary status for balanceId={}, isPrimary={}", balanceId, isPrimary);
    }

    @LogExecutionTime
    @Override
    public void updateBalance(Long balanceId, Long amount) {
        jdbc.update(
                "UPDATE account_balances SET balance = balance + ? WHERE id = ?",
                amount, balanceId
        );
        log.info("Updated balance: id={}, delta={}", balanceId, amount);
    }

    private AccountBalance mapBalance(ResultSet rs) throws java.sql.SQLException {
        return AccountBalance.builder()
                .id(rs.getLong("id"))
                .accountId(rs.getLong("account_id"))
                .balance(rs.getLong("balance"))
                .isPrimary(rs.getBoolean("is_primary"))
                .createdAt(rs.getLong("created_at"))
                .balanceNumber(rs.getString("balance_number"))
                .build();
    }
}
