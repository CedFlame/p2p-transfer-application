package ru.shmatov.exception;

public class SecurityBalanceNotBelongAccountException extends BusinessException {
    public SecurityBalanceNotBelongAccountException(String message) {
        super(message);
    }
}
