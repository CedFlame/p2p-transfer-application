package ru.shmatov.exception;

public class OnlyOneBalanceException extends BusinessException {
  public OnlyOneBalanceException(String balanceNumber) {
    super("You have only one balance: " + balanceNumber);
  }
}
