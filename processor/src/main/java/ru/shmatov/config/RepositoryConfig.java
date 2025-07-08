package ru.shmatov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.shmatov.repository.*;
import ru.shmatov.repository.impl.*;

@Configuration
public class RepositoryConfig {

    @Bean
    public AccountRepository accountRepository(JdbcTemplate jdbcTemplate) {
        return new AccountRepositoryImpl(jdbcTemplate);
    }

    @Bean
    public AccountBalanceRepository accountBalanceRepository(JdbcTemplate jdbcTemplate) {
        return new AccountBalanceRepositoryImpl(jdbcTemplate);
    }

    @Bean
    public TransactionRepository transactionRepository(JdbcTemplate jdbcTemplate) {
        return new TransactionRepositoryImpl(jdbcTemplate);
    }

    @Bean
    public UserRepository userRepository(JdbcTemplate jdbcTemplate) {
        return new UserRepositoryImpl(jdbcTemplate);
    }
}
