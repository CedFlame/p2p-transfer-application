package ru.shmatov.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.exception.UserAlreadyExistsException;
import ru.shmatov.model.User;
import ru.shmatov.repository.UserRepository;
import ru.shmatov.service.UserService;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.account-max-balance}")
    private int USER_BALANCE_COUNT_LIMIT;

    @Override
    @Transactional
    @LogExecutionTime
    public String registerUser(String username, String telegramUsername, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(username);
        }

        User user = User.builder()
                .username(username)
                .telegramUsername(telegramUsername)
                .password(passwordEncoder.encode(rawPassword))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .balanceCountLimit(USER_BALANCE_COUNT_LIMIT)
                .createdAt(System.currentTimeMillis())
                .roles(Set.of("USER"))
                .build();

        userRepository.save(user);
        log.info("User [{}] has been successfully registered", username);
        return user.getUsername();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserDetails userDetails = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        log.debug("Loaded user [{}]", username);
        return userDetails;
    }
}
