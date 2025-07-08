package ru.shmatov.request;

import javax.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
