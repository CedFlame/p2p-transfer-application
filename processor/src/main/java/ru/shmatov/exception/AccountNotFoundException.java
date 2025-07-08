package ru.shmatov.exception;

public class AccountNotFoundException extends BusinessException {
    public AccountNotFoundException(String username) {
        super("Account not found for user: " + username);
    }
}
