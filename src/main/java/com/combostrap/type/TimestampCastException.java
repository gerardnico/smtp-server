package com.combostrap.type;

/**
 * An exception that wrap {@link ClassCastException}
 * that is a runtime exception and is therefore not advertised
 */
public class TimestampCastException extends CastException {


  public TimestampCastException() {
  }

  public TimestampCastException(String message) {
    super(message);
  }

  public TimestampCastException(Throwable cause) {
    super(cause);
  }

  public TimestampCastException(String message, Throwable cause) {
    super(message, cause);
  }


}
