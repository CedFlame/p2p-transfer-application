package ru.shmatov.exception;

public class AccountAlreadyExistsException extends BusinessException {
    public AccountAlreadyExistsException(String username) {
        super("Account already exists for user: " + username);
    }
}
