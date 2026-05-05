package com.combostrap.type;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A wrapper around an {@link Integer}
 * to cast it around
 */
public class Integers {


  protected static Set<String> NULL_STRINGS = new HashSet<>(Arrays.asList("", "null", "na"));

  /**
   * The integer value
   */
  private final Integer integer;

  public Integers(Integer integer) {
    this.integer = integer;
  }

  /**
   * @param s the input string
   * @return an integer from the string s or a runtime exception
   */
  static Integers createFromString(String s, NumberFormat numberFormat, Set<String> nullString) throws CastException {
    try {
      if (s==null || nullString.contains(s)){
        return new Integers(null);
      } else {
        return new Integers(numberFormat.parse(s).intValue());
      }
    } catch (ParseException e) {
      throw new CastException(e);
    }
  }

  /**
   * @param s the input string
   * @return an integer from the string s or a runtime exception
   */
  static Integers createFromString(String s, NumberFormat numberFormat) throws CastException {
    return createFromString(s, numberFormat, NULL_STRINGS);
  }

  static Integers createFromString(String s) throws CastException {
    /**
     * Format for parsing integers. Not thread-safe
     */
    NumberFormat integerInstance = NumberFormat.getIntegerInstance(Locale.ROOT);
    return createFromString(s, integerInstance);
  }

  public static Integers createFromInteger(int i) {
    return new Integers(i);
  }

  public Double toDouble() {
    if (this.integer != null) {
      return this.integer.doubleValue();
    } else {
      return null;
    }
  }

  public static Integers createFromObject(Object o) throws CastException {
    if (o == null) {
      return new Integers(null);
    }
    if (o instanceof Integer) {
      return createFromInteger((Integer) o);
    } else if (o instanceof String) {
      return createFromString((String) o);
    } else if (o instanceof Double) {
      return createFromDouble((Double) o);
    } else if (o instanceof Short) {
      return createFromShort((Short) o);
    } else if (o instanceof Float) {
      return createFromFloat((Float) o);
    } else if (o instanceof BigDecimal) {
      return createFromBigDecimal((BigDecimal) o);
    } else if (o instanceof BigInteger) {
      return createFromBigInteger((BigInteger) o);
    } else if (o instanceof Long) {
      return createFromLong((Long) o);
    } else {
      throw new CastException("The transformation of the class (" + o.getClass().getSimpleName() + ") to a integer is not possible or not yet supported");
    }

  }

  private static Integers createFromShort(Short o) {
    return new Integers(o.intValue());
  }

  private static Integers createFromLong(Long o) {
    return new Integers(o.intValue());
  }

  private static Integers createFromBigInteger(BigInteger o) {
    return new Integers(o.intValue());
  }

  private static Integers createFromBigDecimal(BigDecimal o) {
    return new Integers(o.intValue());
  }

  private static Integers createFromFloat(Float o) {
    return new Integers(o.intValue());
  }

  private static Integers createFromDouble(Double aDouble) {
    return new Integers(aDouble.intValue());
  }

  public Integer toInteger() {
    return this.integer;
  }

  public long toBase(int base) {
    long ret = 0, factor = 1;
    int num = this.integer;
    while (num > 0) {
      ret += num % base * factor;
      num /= base;
      factor *= 10;
    }
    return ret;
  }

  @Override
  public String toString() {
    return integer.toString();
  }

  public BigDecimal toBigDecimal() {
    return new BigDecimal(integer);
  }

  public Short toShort() {
    return integer.shortValue();
  }

  public Long toLong() {
    return integer.longValue();
  }

  public BigInteger toBigInteger() {
    return BigInteger.valueOf(toLong());
  }

  public Float toFloat() {
    return integer.floatValue();
  }


}
