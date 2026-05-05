package com.combostrap.dns;

public class DnsException extends Exception {

  public DnsException(Exception e) {
    super(e);
  }

  public DnsException(String message, Throwable e) {
    super(message, e);
  }

  public DnsException(String s) {
    super(s);
  }
}
