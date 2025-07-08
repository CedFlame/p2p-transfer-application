package ru.shmatov.exception;

public class BalanceNumberGenerationException extends BusinessException {
    public BalanceNumberGenerationException() {
        super("Failed to generate balance number");
    }
}
