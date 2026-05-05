package com.combostrap.type;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class Longs {


  private final Long aLong;

  public Longs(Long aLong) {
    this.aLong = aLong;
  }

  public static Longs createFromObject(Object o) throws CastException {
    if (o == null) {
      return new Longs(null);
    } else if (o instanceof Float) {
      return createFromFloat((Float) o);
    } else if (o instanceof Double) {
      return createFromDouble((Double) o);
    } else if (o instanceof Long) {
      return createFromLong((Long) o);
    } else if (o instanceof Integer) {
      return createFromInteger((Integer) o);
    } else if (o instanceof BigInteger) {
      return createFromBigInteger((BigInteger) o);
    } else if (o instanceof BigDecimal) {
      return createFromBigDecimal((BigDecimal) o);
    } else if (o instanceof String) {
      return createFromString((String) o);
    }  else {
      throw new CastException("The value (" + o + ") has a class (" + o.getClass() + ") that is not supported for a Long transformation");
    }
  }

  private static Longs createFromLong(Long o) {
    return new Longs(o);
  }

  /**
   * @param s
   * @return an integer from the string s or a runtime exception
   */
  public static Longs createFromString(String s, NumberFormat numberFormat) throws CastException {
    try {
      return new Longs(numberFormat.parse(s).longValue());
    } catch (ParseException e) {
      throw new CastException(e);
    }
  }

  public static Longs createFromString(String s) throws CastException {
    /**
     * Format for parsing integers. Not thread-safe
     */
    NumberFormat integerInstance = NumberFormat.getIntegerInstance(Locale.ROOT);
    return createFromString(s, integerInstance);
  }

  private static Longs createFromFloat(Float o) {
    return new Longs(o.longValue());
  }

  private static Longs createFromBigInteger(BigInteger o) {
    return new Longs(o.longValue());
  }

  private static Longs createFromInteger(Integer o) {
    return new Longs(Long.valueOf(o));
  }

  private static Longs createFromBigDecimal(BigDecimal o) {
    return new Longs(o.longValue());
  }

  private static Longs createFromDouble(Double o) {
    return new Longs(o.longValue());
  }

  public Long toLong() {
    return this.aLong;
  }
}
