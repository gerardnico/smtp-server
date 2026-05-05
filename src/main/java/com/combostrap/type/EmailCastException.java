package com.combostrap.type;


public class EmailCastException extends CastException {
  public EmailCastException(String message) {
    super(message);
  }

  public EmailCastException(Exception e) {
    super(e);
  }
}
