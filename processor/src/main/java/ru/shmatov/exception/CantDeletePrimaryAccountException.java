package ru.shmatov.exception;

public class CantDeletePrimaryAccountException extends BusinessException {
    public CantDeletePrimaryAccountException(String number) {
        super("Cant delete primary account: " + number);
    }
}
