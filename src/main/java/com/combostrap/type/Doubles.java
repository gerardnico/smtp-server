package com.combostrap.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * A {@link Double} wrapper
 */
public class Doubles {

  private final Double doubleValue;

  public Doubles(Double doubleValue) {
    this.doubleValue = doubleValue;
  }

  public static Doubles createFromObject(Object o) {
    if (o == null){
      return new Doubles(null);
    }
    if (o instanceof Double) {
      return createFromDouble((Double) o);
    } else if (o instanceof String) {
      return createFromString((String) o);
    } else if (o instanceof Integer) {
      return createFromInteger((Integer) o);
    } else if (o instanceof Float) {
      return createFromFloat((Float) o);
    } else if (o instanceof BigDecimal) {
      return createFromBigDecimal((BigDecimal) o);
    } else if (o instanceof BigInteger) {
      return  createFromBigInteger((BigInteger) o);
    } else if (o instanceof Long) {
      return  createFromLong((Long) o);
    } else {
      throw new IllegalArgumentException("The transformation of the class ("+o.getClass().getSimpleName()+") to a double is not possible or not yet supported");
    }
  }

  private static Doubles createFromLong(Long o) {
    return new Doubles(o.doubleValue());
  }

  public static Doubles createFromBigInteger(BigInteger o) {
    return new Doubles(o.doubleValue());
  }

  public static Doubles createFromBigDecimal(BigDecimal o) {
    return new Doubles(o.doubleValue());
  }

  public static Doubles createFromFloat(Float o) {
    return new Doubles(o.doubleValue());
  }

  public static Doubles createFromInteger(Integer integer) {
    return new Doubles(integer.doubleValue());
  }

  public static Doubles createFromDouble(Double o) {
    return new Doubles(o);
  }

  /**
   * @param scale - the scale (the number of decimal after the comma)
   * @return a double - half up rounded to the scale
   * TODO: The test utility of Sqlite use a printFormat for double
   */
  public Doubles round(Integer scale) {

    if (scale < 0) throw new IllegalArgumentException();
    BigDecimal bd = new BigDecimal(this.doubleValue);
    bd = bd.setScale(scale, RoundingMode.HALF_UP);
    return new Doubles(bd.doubleValue());

  }

  /**
   * Format for parsing long
   */
  private static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ROOT);


  public static Doubles createFromString(String s) {
    /**
     * See also {@link Double#parseDouble(String)}
     */
    try {
      return new Doubles(numberFormat.parse(s).doubleValue());
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public Float toFloat() {
      return this.doubleValue.floatValue();
  }

  public Double toDouble() {
    return this.doubleValue;
  }
}
