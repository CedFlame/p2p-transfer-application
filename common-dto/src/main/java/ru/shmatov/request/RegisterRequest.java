package ru.shmatov.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username не может быть пустым")
        String username,

        @NotBlank(message = "telegramUsername не может быть пустым")
        String telegramUsername,

        @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
        String password,

        String confirmedPassword
) {}