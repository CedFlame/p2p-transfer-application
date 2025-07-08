package ru.shmatov.exception;

public class AccountNotEmptyException extends BusinessException {
  public AccountNotEmptyException(String accountNumber) {
    super("Account " + accountNumber + " is not empty");
  }
}
