package ru.shmatov.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import ru.shmatov.exception.UserAlreadyExistsException;

public interface UserService extends UserDetailsService {

    String registerUser(String username, String telegramUsername, String rawPassword) throws UserAlreadyExistsException;
    boolean existsByUsername(String username);
}