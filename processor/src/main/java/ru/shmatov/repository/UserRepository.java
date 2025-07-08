package ru.shmatov.repository;

import org.springframework.security.core.userdetails.UserDetailsService;
import ru.shmatov.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String  username);

    Optional<User> findByTelegramUsername(String telegramUsername);

    void save(User user);

    boolean existsByUsername(String username);
}

