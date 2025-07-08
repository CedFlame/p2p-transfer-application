package ru.shmatov.exception;

public class SecurityBalanceNotBelongTransactionException extends BusinessException {
    public SecurityBalanceNotBelongTransactionException(String message) {
        super(message);
    }
}
