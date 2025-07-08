package ru.shmatov.exception;

import java.security.InvalidKeyException;

public class ConfirmationCodeExpiredException extends InvalidKeyException {
    public ConfirmationCodeExpiredException(String code) {
        super(code);
    }
}
