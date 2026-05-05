package com.combostrap.smtp.sasl;

public class SimpleAuthException extends Exception {
  public SimpleAuthException(String message) {
    super(message);
  }

  public static SimpleAuthException create(String message) {
    return new SimpleAuthException(message);
  }
}
