package ru.shmatov.exception;

public class BalanceLimitExceededException extends BusinessException {
    public BalanceLimitExceededException(String username) {
        super("Balance limit exceeded for user: " + username);
    }
}
