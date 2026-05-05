package com.combostrap.exception;

/**
 * An exception to tell that this should
 * not happen if the code is correctly written
 */
public class InternalException extends RuntimeException {

  public InternalException(String s) {
    super(s);
  }

  public InternalException(Throwable cause) {
    super(cause);
  }

  public InternalException(String s, Throwable e) {
    super(s,e);
  }

}
