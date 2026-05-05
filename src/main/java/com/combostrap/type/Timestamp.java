package com.combostrap.type;


import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * A wrapper around a {@link LocalDateTime local date time}
 * to represent a date with time (ie SQL Timestamp definition)
 * The now local date time is created in the UTC timezone (Z)
 */
public class Timestamp {


  LocalDateTime localDateTime;


  public Timestamp(LocalDateTime localDateTime) {
    this.localDateTime = localDateTime;
  }


  public static Timestamp createFromString(String s) throws TimestampCastException {

      String pattern;
      try {
          pattern = TimeStringParser.detectFormat(s);
      } catch (TimeStringCastException e) {
          throw new TimestampCastException(e);
      }
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    LocalDateTime dateTime;

    // No Time
    if (s.length() == 10) {
      // no time
      LocalTime localTime = LocalTime.of(0, 0, 0);
      LocalDate localDate = LocalDate.parse(s, formatter);
      dateTime = LocalDateTime.of(localDate, localTime);
      return createFromLocalDateTime(dateTime);
    }

    // With time
    try {
      dateTime = LocalDateTime.parse(s, formatter);
    } catch (Exception e) {
      throw new TimestampCastException("The string (" + s + ") is not a date time", e);
    }
    return createFromLocalDateTime(dateTime);

    /**
     * Deleted 2022-10-18 to be able to handle more than millis that you can found in {@link LocalDateTime#toString()}
     * `
     * SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
     * java.util.Date date = simpleDateFormat.parse(s);
     * `
     */


  }

  public static Timestamp createFromDate(Date date) {

    LocalDateTime localDateTime = Instant.ofEpochMilli(date.getTime()).atZone(TimeZone.getDefault().toZoneId()).toLocalDateTime();
    return new Timestamp(localDateTime);

  }

  public static Timestamp createFromNowLocalSystem() {
    return createFromLocalDateTime(LocalDateTime.now(ZoneId.systemDefault()));
  }


  /**
   *
   * @throws CastException
   * Note: We consider a number to be representing the timestamp, in milliseconds, of the current time.
   * As Javascript does <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/now">...</a>
   */
  public static Timestamp createFromObject(Object sourceObject) throws CastException {

    if (sourceObject == null) {
      return new Timestamp(null);
    } else if (sourceObject instanceof Timestamp) {
      return (Timestamp) sourceObject;
    } else if (sourceObject instanceof java.sql.Timestamp) {
      return createFromSqlTimestamp((java.sql.Timestamp) sourceObject);
    } else if (sourceObject instanceof LocalDateTime) {
      return createFromLocalDateTime((LocalDateTime) sourceObject);
    } else if (sourceObject instanceof LocalDate) {
      return createFromLocalDate((LocalDate) sourceObject);
    } else if (sourceObject instanceof Long) {
      return createFromEpochMilli((Long) sourceObject);
    } else if (sourceObject instanceof Double) {
      return createFromEpochMilli(((Double) sourceObject).longValue());
    } else if (sourceObject instanceof String) {
      return createFromString((String) sourceObject);
    } else if (sourceObject instanceof Date) {
      return createFromDate((Date) sourceObject);
    } else if (sourceObject instanceof Integer) {
      return createFromEpochMilli(((Integer) sourceObject).longValue());
    } else {
      throw new CastException("The value (" + sourceObject + ") with the class (" + sourceObject.getClass().getSimpleName() + ") cannot be transformed to a timestamp");
    }
  }

  /**
   * Throw only at runtime
   * Used when we don't know what to do and that we are responsible for the data
   * therefore we know with great certainty that we will not have any problem
   * @param sourceObject - the source
   */
  @SuppressWarnings("unused")
  public static Timestamp createFromObjectSafeCast(Object sourceObject)  {
      try {
          return createFromObject(sourceObject);
      } catch (CastException e) {
          throw new RuntimeException(e);
      }
  }


  private static Timestamp createFromLocalDate(LocalDate sourceObject) {
    return new Timestamp(LocalDateTime.of(sourceObject, LocalTime.MIN));
  }

  public static Timestamp createFromSqlTimestamp(java.sql.Timestamp sourceObject) {
    return new Timestamp(sourceObject.toLocalDateTime());
  }

  public static Timestamp createFromLocalDateTime(LocalDateTime sourceObject) {
    return new Timestamp(sourceObject);
  }

  public static Timestamp createFromTimestamp(Timestamp timestamp) {
    return new Timestamp(timestamp.toLocalDateTime());
  }

  public static Timestamp createFromEpochSec(Long epochSec) {
    return createFromEpochMilli(epochSec * 1000);
  }

  public static Timestamp createFromInstant(Instant instant) {
    return new Timestamp(LocalDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId()));
  }

  public static Timestamp createFromNowUtc() {

    return createFromLocalDateTime(LocalDateTime.now(ZoneOffset.UTC));
  }

  public java.sql.Timestamp toSqlTimestamp() {

    return java.sql.Timestamp.valueOf(localDateTime);

  }

  public static Timestamp createFromEpochMilli(long epochMilli) {

    return new Timestamp(
      Instant
        .ofEpochMilli(epochMilli)
        .atZone(TimeZone.getDefault().toZoneId())
        .toLocalDateTime()
    );

  }


  public Long toEpochMilli() {

    return localDateTime.atZone(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli();

  }

  public Date toDate() {
    return Date.from(localDateTime.atZone(TimeZone.getDefault().toZoneId()).toInstant());
  }


  public String toIsoString() {
    return localDateTime.toString();
  }


  public String toString(String format) {
    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
    return sdf.format(toDate());
  }

  public String toString(String format, TimeZone timeZone) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    sdf.setTimeZone(timeZone);
    return sdf.format(toDate());
  }

  public LocalDateTime toLocalDateTime() {
    return this.localDateTime;
  }

  public Timestamp afterMillis(long l) {
    return new Timestamp(this.localDateTime.plus(l, ChronoUnit.MILLIS));
  }

  public Timestamp beforeMillis(long l) {
    return new Timestamp(this.localDateTime.minus(l, ChronoUnit.MILLIS));
  }

  public Boolean isAfterThan(Timestamp timestamp) {
    return this.localDateTime.isAfter(timestamp.toLocalDateTime());
  }


  public Boolean isBeforeThan(Timestamp timestamp) {
    return this.localDateTime.isBefore(timestamp.toLocalDateTime());
  }

  @Override
  public String toString() {
    return this.localDateTime.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Timestamp timestamp = (Timestamp) o;
    return Objects.equals(localDateTime, timestamp.localDateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localDateTime);
  }

  /**
   * @param timestamp the external timestamp
   * @return The minus result in millisecond (ie between value)
   */
  public long minus(Timestamp timestamp) {
    return between(timestamp);
  }

  public long between(Timestamp timestamp) {
    return ChronoUnit.MILLIS.between(this.localDateTime, timestamp.toLocalDateTime());
  }

  public Long toEpochSec() {
    return toEpochMilli() / 1000;
  }

  /**
   * @return A sql timestamp truncated to the second
   */
  public java.sql.Timestamp toSqlTimestampSec() {
    return java.sql.Timestamp.valueOf(localDateTime.truncatedTo(ChronoUnit.SECONDS));
  }

  public Timestamp truncate(TemporalUnit temporalUnit) {
    this.localDateTime = this.localDateTime.truncatedTo(temporalUnit);
    return this;
  }

  public java.sql.Timestamp toSqlTimestampMillis() {
    return java.sql.Timestamp.valueOf(localDateTime.truncatedTo(ChronoUnit.MILLIS));
  }

  public LocalDateTime toLocalDateTimeMillis() {
    return this.localDateTime.truncatedTo(ChronoUnit.MILLIS);
  }

  @SuppressWarnings("unused")
  public OffsetDateTime toOffsetDateTime() {
    return this.localDateTime.atOffset(OffsetDateTime.now().getOffset());
  }

  /**
   * @return a timestamp "YYYYMMDDHHMMSS" suitable for the file system
   */
  public String toFileSystemString() {
    /**
     * An ISO string has `:` that is not permitted
     */
    return toString("YYYYMMDDHHMMSS");
  }
}
