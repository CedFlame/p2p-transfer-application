package ru.shmatov.exception;

public class DuplicateBalanceNumberException extends BusinessException {
  public DuplicateBalanceNumberException(String balanceNumber) {
    super("Two or more balances must not have the same number: " + balanceNumber);
  }
}
