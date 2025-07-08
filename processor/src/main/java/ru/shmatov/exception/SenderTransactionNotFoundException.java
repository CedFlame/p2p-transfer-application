package ru.shmatov.exception;

public class SenderTransactionNotFoundException extends BusinessException {
    public SenderTransactionNotFoundException(Long id) {
        super("Sender transaction not found: " + id);
    }
}
