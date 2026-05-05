package com.combostrap.type;


public class MediaTypePathNotAbsoluteException extends Exception {
  public MediaTypePathNotAbsoluteException(String message) {
    super(message);
  }

  public MediaTypePathNotAbsoluteException(Exception e) {
    super(e);
  }
}
