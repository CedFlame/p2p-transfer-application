package ru.shmatov.exception;

public class UserAlreadyExistsException extends BusinessException {
    public UserAlreadyExistsException(String message) {
        super("User " + message + " already exists, just login");
    }
}
