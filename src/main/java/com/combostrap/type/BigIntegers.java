package com.combostrap.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class BigIntegers {

  private final BigInteger bigInteger;

  public BigIntegers(BigInteger bigInteger) {
    this.bigInteger = bigInteger;
  }

  public static BigIntegers createFromObject(Object o) {
    if (o == null) {
      return new BigIntegers(null);
    } else if (o instanceof BigInteger) {
      return new BigIntegers((BigInteger) o);
    } else if (o instanceof BigDecimal) {
      return createFromBigDecimal((BigDecimal) o);
    } else if (o instanceof Integer) {
      return createFromInteger((Integer) o);
    } else if (o instanceof Double) {
      return createFromDouble((Double) o);
    } else if (o instanceof Long) {
      return createFromLong((Long) o);
    } else if (o instanceof Float) {
      return createFromFloat((Float) o);
    } else if (o instanceof String) {
      return createFromString((String) o);
    } else {
      throw new IllegalArgumentException("The value (" + o + ") has a class (" + o.getClass().getSimpleName() + ") that cannot be transformed to a big integer");
    }
  }

  private static BigIntegers createFromBigDecimal(BigDecimal o) {
    return new BigIntegers(o.toBigInteger());
  }

  /**
   * @param s
   * @return an integer from the string s or a runtime exception
   */
  public static BigIntegers createFromString(String s, NumberFormat numberFormat) {
    try {
      return new BigIntegers(BigInteger.valueOf(numberFormat.parse(s).longValue()));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static BigIntegers createFromString(String s) {
    /**
     * Format for parsing integers. Not thread-safe
     */
    NumberFormat integerInstance = NumberFormat.getIntegerInstance(Locale.ROOT);
    return createFromString(s, integerInstance);
  }


  public static BigIntegers createFromFloat(Float o) {
    return new BigIntegers(BigInteger.valueOf(o.longValue()));
  }

  public static BigIntegers createFromLong(Long sourceObject) {
    return new BigIntegers(BigInteger.valueOf(sourceObject));
  }

  public static BigIntegers createFromDouble(Double sourceObject) {
    return new BigIntegers(BigInteger.valueOf(sourceObject.longValue()));
  }

  public static BigIntegers createFromInteger(Integer integer) {
    return new BigIntegers(BigInteger.valueOf(integer.longValue()));
  }

  public static BigIntegers createFromBigInteger(BigInteger bigInteger) {
    return new BigIntegers(bigInteger);
  }

  public BigInteger toBigInteger() {
    return this.bigInteger;
  }

  public Long toLong() {
    return this.bigInteger.longValue();
  }
}
