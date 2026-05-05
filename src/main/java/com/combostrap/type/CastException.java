package com.combostrap.type;

/**
 * An exception that wrap {@link ClassCastException}
 * that is a runtime exception and is therefore not advertised
 */
public class CastException extends Exception {


  public CastException() {
  }

  public CastException(String message) {
    super(message);
  }

  public CastException(Throwable cause) {
    super(cause);
  }

  public CastException(String message, Throwable cause) {
    super(message, cause);
  }


}
