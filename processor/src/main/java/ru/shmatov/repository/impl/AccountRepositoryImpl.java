package ru.shmatov.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.model.Account;
import ru.shmatov.repository.AccountRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final JdbcTemplate jdbc;
    private final RowMapper<Account> mapper = (rs, n) -> mapAccount(rs);

    @Override
    public Optional<Account> findByUsername(String username) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(
                            "SELECT * FROM accounts WHERE user_username = ?",
                            mapper, username));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Account> findByUserId(Long userId) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(
                            "SELECT * FROM accounts WHERE user_id = ?",
                            mapper, userId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(
                            "SELECT * FROM accounts WHERE account_number = ?",
                            mapper, accountNumber));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @LogExecutionTime
    @Override
    public Long save(Account account) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO accounts (user_id, user_username, user_telegram_username, account_number) VALUES (?,?,?,?)",
                    new String[]{"id"});
            ps.setLong(1, account.getUserId());
            ps.setString(2, account.getUserUsername());
            ps.setString(3, account.getUserTelegramUsername());
            ps.setString(4, account.getAccountNumber());
            return ps;
        }, keyHolder);
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        log.info("Account saved with id={}", id);
        return id;
    }

    @LogExecutionTime
    @Override
    public String deleteByUserUsername(String username) {
        String accountNumber = jdbc.queryForObject(
                "SELECT account_number FROM accounts WHERE user_username = ?",
                String.class,
                username
        );

        jdbc.update("DELETE FROM accounts WHERE user_username = ?", username);
        log.info("Deleted account: username={}, accountNumber={}", username, accountNumber);
        return accountNumber;
    }

    private Account mapAccount(ResultSet rs) throws java.sql.SQLException {
        return Account.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .userUsername(rs.getString("user_username"))
                .userTelegramUsername(rs.getString("user_telegram_username"))
                .accountNumber(rs.getString("account_number"))
                .build();
    }
}
