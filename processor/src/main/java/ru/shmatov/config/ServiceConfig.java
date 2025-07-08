package ru.shmatov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.shmatov.repository.*;
import ru.shmatov.service.*;
import ru.shmatov.service.impl.*;

@Configuration
public class ServiceConfig {

    @Bean
    public AccountService accountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            AccountBalanceRepository accountBalanceRepository,
            TransactionRepository transactionRepository
    ) {
        return new AccountServiceImpl(
                accountRepository,
                userRepository,
                accountBalanceRepository,
                transactionRepository
        );
    }

    @Bean
    public RedisService redisService(StringRedisTemplate redisTemplate) {
        return new RedisServiceImpl(redisTemplate);
    }

    @Bean
    public TransactionService transactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository,
            UserRepository userRepository
    ) {
        return new TransactionServiceImpl(
                transactionRepository,
                accountRepository,
                accountBalanceRepository,
                userRepository
        );
    }

    @Bean
    public TransferService transferService(
            UserRepository userRepository,
            TransactionService transactionService,
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository,
            RedisService redisService,
            TransactionRepository transactionRepository
    ) {
        return new TransferServiceImpl(
                userRepository,
                transactionService,
                accountRepository,
                accountBalanceRepository,
                redisService,
                transactionRepository
        );
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new UserServiceImpl(userRepository, passwordEncoder);
    }
}
