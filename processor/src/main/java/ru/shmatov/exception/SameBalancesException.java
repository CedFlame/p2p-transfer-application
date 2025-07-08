package ru.shmatov.exception;

public class SameBalancesException extends BusinessException {
    public SameBalancesException() {
        super("balance sender and balance receiver are same ");
    }
}
