package com.combostrap.type;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

/**
 * Time part of the day (ie hh:mm:ss)
 */
public class Time {


  private final LocalTime localTime;

  public Time(LocalTime localTime) {
    this.localTime = localTime;
  }

  public static Time createFromString(String s) {
    Time time;
    try {
      time = new Time(LocalTime.parse(s));
    } catch (DateTimeParseException e) {
      /**
       * Trying to correct it
       */
      String[] splits = s.split(":");
      List<String> newStringParts = new ArrayList<>();
      for (String split : splits) {
        /**
         * Hour without the prefix 0 (ie 8 in place of 08)
         */
        if (split.length() == 1) {
          newStringParts.add("0" + split);
        } else {
          newStringParts.add(split);
        }
      }
      /**
       * Do we miss the ms part
       */
      if (splits.length == 1) {
        newStringParts.add("00");
      }
      String newTimeString = String.join(":",newStringParts);
      try {
        time = new Time(LocalTime.parse(newTimeString));
      } catch (DateTimeParseException ese) {
        throw new DateTimeParseException("The time string given (" + s + ") does not follow the time pattern `HH:MM:SS`, we change it to (" + newTimeString + ") but still have a parse error for this time string.", s, 1, ese);
      }
    }
    return time;
  }

  public static Time createFromObject(Object object) {

    if (object == null) {
      return null;
    } else if (object instanceof Time) {
      return (Time) object;
    } else if (object instanceof java.sql.Time) {
      return createFromSqlTime((java.sql.Time) object);
    } else if (object instanceof LocalTime) {
      return new Time((LocalTime) object);
    } else if (object instanceof Long) {
      return createFromEpochSec((Long) object);
    } else if (object instanceof Integer) {
      return createFromEpochSec(((Integer) object).longValue());
    } else if (object instanceof String) {
      return createFromString((String) object);
    } else {
      throw new IllegalArgumentException("The object (" + object + ") has a class (" + object.getClass().getSimpleName() + ") that is not yet supported for a time");
    }

  }

  private static Time createFromSqlTime(java.sql.Time sqlTime) {
    return new Time(sqlTime.toLocalTime());
  }

  public static Time createFromEpochMs(Long epochMs) {
    return new Time(LocalTime.ofSecondOfDay(epochMs / 1000));
  }

  public static Time createFromEpochSec(Long epochSec) {
    return new Time(LocalTime.ofSecondOfDay(epochSec));
  }


  public static Time createFromNow() {
    return new Time(LocalTime.now());
  }

  /**
   * In a SQL Time, the time is represented as the distance,
   * measured in milliseconds, of that time
   * from the epoch (00:00:00 GMT on January 1, 1970).
   * <p>
   * java.sql.time.valueOf("22:10:22") works also
   *
   * @return
   */
  public java.sql.Time toSqlTime() {
    return java.sql.Time.valueOf(this.localTime);
  }

  public LocalTime toLocalTime() {
    return this.localTime;
  }


  @Override
  public String toString() {
    return localTime.toString();
  }

  public Long toEpochMilli() {
    return localTime.getLong(ChronoField.SECOND_OF_DAY) * 1000;
  }

  /**
   * Same as second of day
   *
   * @return
   */
  public Long toEpochSec() {
    return (long) localTime.toSecondOfDay();
  }
}
