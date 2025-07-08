package ru.shmatov.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.request.AuthRequest;
import ru.shmatov.request.RegisterRequest;
import ru.shmatov.response.AuthResponse;
import ru.shmatov.response.RegisterResponse;
import ru.shmatov.service.UserService;
import ru.shmatov.util.JwtUtil;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Регистрация и вход в систему с JWT")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @LogExecutionTime
    @Operation(
            summary = "Регистрация пользователя",
            description = "Создаёт нового пользователя в системе и возвращает его идентификатор",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован",
                            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных"),
                    @ApiResponse(responseCode = "409", description = "Пользователь с таким username уже существует")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody @Valid RegisterRequest request) {
        log.info("Registering new user: {}", request.username());
        String id = userService.registerUser(
                request.username(),
                request.telegramUsername(),
                request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(id));
    }

    @LogExecutionTime
    @Operation(
            summary = "Аутентификация пользователя",
            description = "Выполняет вход пользователя и возвращает JWT-токен",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Аутентификация успешна",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неверный логин или пароль")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid AuthRequest request) {
        log.info("Authenticating user: {}", request.username());
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String jwt = jwtUtil.generateToken((UserDetails) auth.getPrincipal());
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    @LogExecutionTime
    @Operation(
            summary = "Проверка регистрации пользователя",
            description = "Проверяет, существует ли пользователь с указанным username",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь существует или нет",
                            content = @Content(schema = @Schema(implementation = Boolean.class)))
            }
    )
    @GetMapping("/check-registration")
    public ResponseEntity<Boolean> isRegistered(
            @Parameter(description = "Username пользователя") @RequestParam String username) {
        log.info("Checking registration status for user: {}", username);
        return ResponseEntity.ok(userService.existsByUsername(username));
    }
}
