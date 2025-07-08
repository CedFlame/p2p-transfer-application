package ru.shmatov.exception;

public class PrimaryBalanceDeletionException extends BusinessException {
    public PrimaryBalanceDeletionException(Long balanceId) {
        super("Cannot delete primary balance: " + balanceId);
    }
}
