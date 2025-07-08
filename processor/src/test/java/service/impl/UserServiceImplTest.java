package service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.shmatov.exception.UserAlreadyExistsException;
import ru.shmatov.model.User;
import ru.shmatov.repository.UserRepository;
import ru.shmatov.service.impl.UserServiceImpl;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_shouldSaveUser_whenUsernameIsAvailable() {
        String username = "123456789";
        String tgUsername = "testuser";
        String rawPassword = "pass";
        String encodedPassword = "hashed-pass";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        String result = userService.registerUser(username, tgUsername, rawPassword);

        assertThat(result).isEqualTo(username);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo(username);
        assertThat(savedUser.getTelegramUsername()).isEqualTo(tgUsername);
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getRoles()).containsExactly("USER");
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getBalanceCountLimit()).isEqualTo(5);
        assertThat(savedUser.getCreatedAt()).isGreaterThan(0);
    }

    @Test
    void registerUser_shouldThrow_whenUserAlreadyExists() {
        when(userRepository.existsByUsername("123")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.registerUser("123", "tg", "pass")
        ).isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void existsByUsername_shouldReturnTrue_ifUserExists() {
        when(userRepository.existsByUsername("981")).thenReturn(true);

        boolean exists = userService.existsByUsername("981");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_shouldReturnFalse_ifUserDoesNotExist() {
        when(userRepository.existsByUsername("981")).thenReturn(false);

        boolean exists = userService.existsByUsername("981");

        assertThat(exists).isFalse();
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails_ifUserExists() {
        User user = User.builder()
                .username("user1")
                .password("encoded")
                .roles(Set.of("USER"))  // в базе именно "USER"
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("user1");

        assertThat(result.getUsername()).isEqualTo("user1");
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }


    @Test
    void loadUserByUsername_shouldThrow_ifUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}