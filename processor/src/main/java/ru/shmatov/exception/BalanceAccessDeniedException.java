package ru.shmatov.exception;

public class BalanceAccessDeniedException extends BusinessException {
    public BalanceAccessDeniedException(Long balanceId) {
        super("Access denied to balance: " + balanceId);
    }
}
