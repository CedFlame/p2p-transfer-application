package ru.shmatov.exception;

public class InvalidConfirmationCodeException extends BusinessException {
    public InvalidConfirmationCodeException(String message) {
        super("Invalid conforming confirmation code or code was expired: " + message);
    }
}
