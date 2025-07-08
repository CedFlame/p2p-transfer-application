package ru.shmatov.exception;

public class ReceiverTransactionNotFoundException extends BusinessException {
  public ReceiverTransactionNotFoundException(Long id) {
    super("Receiver transaction not found: " + id);
  }
}
