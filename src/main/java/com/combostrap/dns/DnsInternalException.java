package com.combostrap.dns;

/**
 * An exception that should not occur
 * because:
 * * we check the data quality or the state before
 * * or we wrote the literal code
 */
public class DnsInternalException extends RuntimeException {
  public DnsInternalException(Exception e) {
    super(e);
  }

  public DnsInternalException(String message) {
    super(message);
  }

  public DnsInternalException(String message, Exception e) {
    super(message,e);
  }
}
