package ru.shmatov.exception;

public class AlreadyPrimaryBalanceException extends BusinessException {
    public AlreadyPrimaryBalanceException(String balanceNumber) {
        super("Balance " + balanceNumber + " is already primary");
    }
}
