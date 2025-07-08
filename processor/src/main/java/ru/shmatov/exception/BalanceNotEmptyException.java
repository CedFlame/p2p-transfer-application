package ru.shmatov.exception;

public class BalanceNotEmptyException extends BusinessException {
    public BalanceNotEmptyException(String balanceId) {
        super("Balance " + balanceId + " is not empty");
    }
}
