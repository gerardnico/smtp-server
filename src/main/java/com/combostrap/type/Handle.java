package com.combostrap.type;

import java.util.Objects;

/**
 * A handle is a unique value.
 * If the empty string is passed, the value become null
 * otherwise the uniqueness is broken
 * <p>
 * <a href="https://tools.ietf.org/html/rfc1123">RFC 1123 DNS labels</a>
 * This means the name must:
 * * contain at most 63 characters
 * * contain only lowercase alphanumeric characters or '-'
 * * start with an alphanumeric character
 * * end with an alphanumeric character
 * <p>
 * The only difference between the RFC 1035
 * and RFC 1123 label standards is that
 * RFC 1123 labels are allowed to start with a digit, whereas RFC 1035 labels
 * can start with a lowercase alphabetic character only.
 */
public class Handle {


  private static final int MAX_LENGTH = 32;
  private final String handle;

  private Handle(String s) throws HandleCastException {
    /**
     * The handle object should be null, not the value
     */
    if (s == null) {
      throw new HandleCastException("A handle value cannot be null. Set the handle object to null instead.");
    }
    /**
     * Forms may send empty string
     * We don't allow as it means that this is also null
     * <p>
     * Putting the value to null does not help as it's
     * used to select rows. You don't want to select all null value.
     */
    if (s.isBlank()) {
      throw new HandleCastException("A handle cannot be the empty string or contains only whitespace");
    }
    try {
      handle = DnsName.create(s).toStringWithoutRoot();
    } catch (DnsCastException e) {
      throw new HandleCastException("The value (" + s + ") is a not a valid handle", e);
    }
    if (handle.length() > MAX_LENGTH) {
      throw new HandleCastException("The value (" + s + ") is too long (max "+MAX_LENGTH+")");
    }

  }

  public static Handle ofFailSafe(String handle) {
      try {
          return new Handle(handle);
      } catch (HandleCastException e) {
          throw new RuntimeException(e);
      }
  }

  public static Handle of(String string) throws HandleCastException {
    return new Handle(string);
  }

  public String getValue() {
    return handle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Handle handle1 = (Handle) o;
    return Objects.equals(handle, handle1.handle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(handle);
  }

  @Override
  public String toString() {
    return handle;
  }

}
