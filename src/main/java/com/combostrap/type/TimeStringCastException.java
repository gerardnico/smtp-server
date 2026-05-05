package com.combostrap.type;

/**
 * An exception that wrap {@link ClassCastException}
 * that is a runtime exception and is therefore not advertised
 */
public class TimeStringCastException extends CastException {


  public TimeStringCastException() {
  }

  public TimeStringCastException(String message) {
    super(message);
  }

  public TimeStringCastException(Throwable cause) {
    super(cause);
  }

  public TimeStringCastException(String message, Throwable cause) {
    super(message, cause);
  }


}
