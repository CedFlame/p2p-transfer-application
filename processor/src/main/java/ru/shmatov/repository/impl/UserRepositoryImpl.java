package ru.shmatov.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.model.User;
import ru.shmatov.repository.UserRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbc;
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> mapUser(rs);

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(
                            "SELECT * FROM users WHERE username = ?",
                            userRowMapper,
                            username
                    )
            );
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByTelegramUsername(String tgUsername) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(
                            "SELECT * FROM users WHERE telegram_username = ?",
                            userRowMapper,
                            tgUsername
                    )
            );
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        return cnt != null && cnt > 0;
    }

    @LogExecutionTime
    @Override
    public void save(User user) {
        final String sql = """
                INSERT INTO users
                  (username, telegram_username, password,
                   enabled, account_non_expired, account_non_locked, credentials_non_expired,
                   balance_count_limit, created_at)
                VALUES (?,?,?,?,?,?,?,?,?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getTelegramUsername());
            ps.setString(3, user.getPassword());
            ps.setBoolean(4, user.isEnabled());
            ps.setBoolean(5, user.isAccountNonExpired());
            ps.setBoolean(6, user.isAccountNonLocked());
            ps.setBoolean(7, user.isCredentialsNonExpired());
            ps.setObject(8, user.getBalanceCountLimit());
            ps.setLong(9, user.getCreatedAt());
            return ps;
        }, keyHolder);

        Long userId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        if (!user.getRoles().isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO user_roles (user_id, role) VALUES (?,?)",
                    user.getRoles(),
                    user.getRoles().size(),
                    (ps, role) -> {
                        ps.setLong(1, userId);
                        ps.setString(2, role);
                    });
        }

        log.info("User [{}] saved with id={}", user.getUsername(), userId);
    }

    private User mapUser(ResultSet rs) throws SQLException {
        long userId = rs.getLong("id");

        Set<String> roles = new HashSet<>(
                jdbc.queryForList(
                        "SELECT role FROM user_roles WHERE user_id = ?",
                        String.class,
                        userId
                )
        );

        return User.builder()
                .id(userId)
                .username(rs.getString("username"))
                .telegramUsername(rs.getString("telegram_username"))
                .password(rs.getString("password"))
                .enabled(rs.getBoolean("enabled"))
                .accountNonExpired(rs.getBoolean("account_non_expired"))
                .accountNonLocked(rs.getBoolean("account_non_locked"))
                .credentialsNonExpired(rs.getBoolean("credentials_non_expired"))
                .balanceCountLimit(rs.getObject("balance_count_limit", Integer.class))
                .createdAt(rs.getLong("created_at"))
                .roles(roles)
                .build();
    }
}
