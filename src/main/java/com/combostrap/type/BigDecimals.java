package com.combostrap.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class BigDecimals {


  private final BigDecimal bigDecimal;

  public BigDecimals(BigDecimal bigDecimal) {
    this.bigDecimal = bigDecimal;
  }

  public static BigDecimals createFromObject(Object o) {
    if (o == null) {
      return new BigDecimals(null);
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
      throw new IllegalStateException("The value (" + o.toString() + ") has a class (" + o.getClass() + ") that is not supported for a BigDecimal transformation");
    }
  }

  private static BigDecimals createFromBigDecimal(BigDecimal o) {
    return new BigDecimals(o);
  }

  /**
   * @param s
   * @return an integer from the string s or a runtime exception
   */
  public static BigDecimals createFromString(String s, NumberFormat numberFormat) {
    try {
      return new BigDecimals(BigDecimal.valueOf(numberFormat.parse(s).longValue()));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static BigDecimals createFromString(String s) {
    /**
     * Format for parsing integers. Not thread-safe
     */
    NumberFormat integerInstance = NumberFormat.getIntegerInstance(Locale.ROOT);
    return createFromString(s, integerInstance);
  }

  private static BigDecimals createFromFloat(Float o) {
    return new BigDecimals(BigDecimal.valueOf(o.longValue()));
  }

  private static BigDecimals createFromBigInteger(BigInteger o) {
    return new BigDecimals(BigDecimal.valueOf(o.longValue()));
  }

  private static BigDecimals createFromInteger(Integer o) {
    return new BigDecimals(BigDecimal.valueOf(o));
  }

  private static BigDecimals createFromLong(Long o) {
    return new BigDecimals(BigDecimal.valueOf(o));
  }

  private static BigDecimals createFromDouble(Double o) {
    return new BigDecimals(BigDecimal.valueOf(o));
  }

  public BigDecimal toBigDecimal() {
    return this.bigDecimal;
  }
}
