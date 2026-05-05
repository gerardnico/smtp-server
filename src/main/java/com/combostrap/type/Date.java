package com.combostrap.type;



import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


import static com.combostrap.type.TimeStringParser.detectFormat;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 * A date class on day precision
 * ie a wrapper around LocalDate
 * <p>
 * A wrapper around all date format where Date means day precision
 * ie YYYY-MM-DD
 */
@SuppressWarnings("unused")
public class Date {


    private final LocalDate localDate;

    /**
     * @return a local date on the gmt zone
     * same as {@link LocalDate#now()} but more explicit
     */
    public static Date createFromNow() {

        return new Date(LocalDate.now());

    }

    public Date(LocalDate localDate) {

        this.localDate = localDate;

    }

    /**
     * Throw only at runtime
     * Used when we don't know what to do and that we are responsible for the data
     * therefore we know with great certainty that we will not have any problem
     *
     * @param sourceObject - the source
     */
    @SuppressWarnings("unused")
    public static Date createFromObjectSafeCast(Object sourceObject) {
        try {
            return createFromObject(sourceObject);
        } catch (CastException e) {
            throw new RuntimeException(e);
        }
    }

    public static Date createFromEpochMilli(Long epochMilli) {

        return new Date((new java.sql.Date(epochMilli)).toLocalDate());

    }

    public static Date createFromSqlDate(java.sql.Date date) {
        return new Date(date.toLocalDate());
    }

    public static Date createFromEpochDay(Long epochDay) {
        return new Date(LocalDate.ofEpochDay(epochDay));
    }

    public static Date createFromObject(Object o) throws CastException {
        if (o instanceof Date) {
            return (Date) o;
        } else if (o instanceof LocalDate) {
            return new Date((LocalDate) o);
        } else if (o instanceof Long) {
            return createFromEpochDay((Long) o);
        } else if (o instanceof java.sql.Date) {
            return createFromSqlDate((java.sql.Date) o);
        } else if (o instanceof java.util.Date) {
      /*
        Date Util may have time component, we set them to zero
        May be done during casting?
       */
            java.util.Date dateUtil = (java.util.Date) o;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateUtil);
            // Set time components to zero
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dateUtil = calendar.getTime();
            return createFromDate(dateUtil);
        } else if (o instanceof String) {
            return createFromString((String) o);
        } else if (o instanceof Integer) {
            return createFromEpochDay(((Integer) o).longValue());
        } else {
            throw new CastException("The object (" + o + ") has an class (" + o.getClass().getSimpleName() + ") that is not yet seen as a date.");
        }
    }

    public static Date createFromEpochSec(Long epochSec) {
        return createFromEpochMilli(epochSec * 1000);
    }

    @SuppressWarnings("unused")
    public static Date createFromFileTime(FileTime time) {
        return new Date(LocalDate.from(time.toInstant()));
    }


    /**
     * Return a {@link java.sql.Date} YYYY-MM-DD
     *
     * @return the date in SQL format
     */
    public java.sql.Date toSqlDate() {

        return java.sql.Date.valueOf(localDate);

    }


    /**
     * @param to the date
     * @return the number of days to
     * <p>
     * Positive if {@code from < to}
     * and that the date is not below 1970 (new Date(0L) = 1970-01-01
     * Otherwise negative
     */
    public long daysTo(Date to) {
        return DAYS.between(localDate, to.toLocalDate());
    }

    /**
     * @param from the date
     * @return the number of days from the
     * <p>
     * Positive if {@code from < to}
     * and that the date is not below 1970 (new Date(0L) = 1970-01-01
     * Otherwise negative
     */
    public long daysFrom(Date from) {
        return DAYS.between(from.toLocalDate(), localDate);
    }

    public LocalDate toLocalDate() {
        return this.localDate;
    }


    /**
     * @param s any date string - This function will perform a format detection. If you know the format use the {@link #createFromStringWithFormat(String, String)}
     */
    public static Date createFromString(String s) throws CastException {

        String pattern = detectFormat(s);
        return createFromStringWithFormat(s, pattern);

    }

    /**
     * @param s      a date in string format
     * @param format the format for SimpleDateFormat. Example for Iso: yyyy-DD-mm
     */
    public static Date createFromStringWithFormat(String s, String format) {

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            simpleDateFormat.setTimeZone(TimeZone.getDefault());
            java.util.Date date = simpleDateFormat.parse(s);
            return createFromDate(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * A java.util.date is a time millisecond  precision with time zone
     *
     * @param date - from a date util
     * @return a date
     */
    public static Date createFromDate(java.util.Date date) {
        LocalDate localDate = date.toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate();
        return new Date(localDate);
    }

    public String toString(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(toDate());
    }

    @Override
    public String toString() {
        return toIsoString();
    }

    public Instant toInstant() {

        return localDate.atStartOfDay(TimeZone.getDefault().toZoneId()).toInstant();

    }

    /**
     * @return the date with the default timezone
     */
    public java.util.Date toDate() {

        return java.util.Date.from(toInstant());

    }


    public String toIsoString() {
        return localDate.toString();
    }

    public Date plusDays(long daysToAdd) {
        return new Date(localDate.plusDays(daysToAdd));
    }

    public Date minusDays(long daysToAdd) {
        return new Date(localDate.minusDays(daysToAdd));
    }


    public Long toEpochDay() {
        return localDate.toEpochDay();
    }

    public Long toEpochMillis() {
        return toEpochDay() * 1000 * 60 * 60 * 24;
    }


    /**
     * Epoch in second
     *
     * @return the second from epoch
     */
    public Long toEpochSec() {
        return localDate.toEpochDay() * 24 * 60 * 60;
    }


}
