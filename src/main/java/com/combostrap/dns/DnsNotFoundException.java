package com.combostrap.dns;

public class DnsNotFoundException extends Exception {

  public DnsNotFoundException() {
  }

  public DnsNotFoundException(String s) {
    super(s);
  }

  public DnsNotFoundException(String s, Exception e) {
    super(s,e);
  }

}
