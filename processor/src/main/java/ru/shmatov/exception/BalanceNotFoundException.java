package ru.shmatov.exception;

public class BalanceNotFoundException extends BusinessException {
    public BalanceNotFoundException(String balanceNumber) {
        super("Balance not found: " + balanceNumber);
    }
}
