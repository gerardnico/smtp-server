package com.combostrap.type;


import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("unused")
public class DurationShort {


  private final DurationShortBuilder builder;

  private DurationShort(DurationShortBuilder shortBuilder) {
    this.builder = shortBuilder;
  }


  public static DurationShort create(String s) throws CastException {
    return builder().build(s);
  }

  public static DurationShort create(Long amount, TimeUnit timeunit) {
    return builder().build(amount, timeunit);
  }

  public static DurationShort create(Duration duration) {
    return builder().build(duration);
  }

  /**
   * A create function that does not throw
   */
  public static DurationShort createSafe(String s) {
    try {
      return builder().build(s);
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
  }

  static public DurationShortBuilder builder() {
    return new DurationShortBuilder();
  }


  public Duration getDuration() {
    return this.builder.duration;
  }

  @Override
  public String toString() {
    if (this.builder.string != null) {
      return this.builder.string;
    }
    return toIsoDuration();
  }


  public DurationHumanStringBuilder toFormatBuilder() {
    return new DurationHumanStringBuilder(this);
  }

  /**
   * A duration in duration iso format
   * @return (n)d(n)h(n)m(n)s.sss
   */
  public String toIsoDuration() {
    return toFormatBuilder()
      .setFormat(DurationFormat.DURATION_ISO)
      .build();
  }

  /**
   * A duration in time iso format where hhhh is the number of hour
   * that can span on more than a day
   * @return hhhh:mm:ss.sss
   */
  public String toIsoTime() {
    return toFormatBuilder()
      .setFormat(DurationFormat.TIME_ISO)
      .build();
  }

  static public class DurationShortBuilder {
    private String string;
    // Pattern to match date components (Y, M, W, D)
    private static final Pattern DATE_PATTERN = Pattern.compile("([0-9]*\\.?[0-9]+[YMWD])+");

    // Pattern to match time components (H, M, S)
    private static final Pattern TIME_PATTERN = Pattern.compile("([0-9]*\\.?[0-9]+[HMS])+");
    private Duration duration;

    public DurationShort build(String s) throws CastException {
      this.string = s;
      this.duration = parseDuration(s);
      return new DurationShort(this);
    }

    /**
     * Parses a duration string by splitting date and time components and
     * reconstructing proper ISO-8601 format with P and T separators.
     *
     * @param durationStr the duration string (e.g., "3S", "1D2H", "5M")
     * @return parsed Duration object
     * @throws java.time.format.DateTimeParseException if the string cannot be parsed
     */
    private Duration parseDuration(String durationStr) throws CastException {
      if (durationStr == null || durationStr.isEmpty()) {
        throw new CastException("Duration string cannot be null or empty");
      }

      // Trim whitespace
      durationStr = durationStr.trim();

      // If already starts with P, assume it's properly formatted
      if (durationStr.startsWith("P")) {
        try {
          return Duration.parse(durationStr);
        } catch (Exception e) {
          throw new CastException("The duration " + durationStr + " starts with P but is not a valid ISO duration. Error: " + e.getMessage(), e);
        }
      }

      durationStr = durationStr.toUpperCase();

      // Extract date and time components using regex
      String dateComponents = "";
      String timeComponents = "";

      Matcher dateMatcher = DATE_PATTERN.matcher(durationStr);
      if (dateMatcher.find()) {
        dateComponents = dateMatcher.group();
      }

      Matcher timeMatcher = TIME_PATTERN.matcher(durationStr);
      if (timeMatcher.find()) {
        timeComponents = timeMatcher.group();
      }

      if (dateComponents.isEmpty() && timeComponents.isEmpty()) {
        throw new CastException("No date or time unit was found. The value (" + durationStr + ") does not contain any of the following time units: d, h, m, s");
      }

      // Reconstruct ISO-8601 format
      StringBuilder result = new StringBuilder("P");


      if (!dateComponents.isEmpty()) {
        // 5M may be 5 minutes or 5 month
        // We go with minutes
        if (!dateComponents.contains("M")) {
          result.append(dateComponents);
        }
      }

      if (!timeComponents.isEmpty()) {
        result.append("T").append(timeComponents);
      }

      try {
        return Duration.parse(result);
      } catch (Exception e) {
        throw new CastException("The duration " + durationStr + " was normalized to the iso duration " + result + " but is not iso conform. Error: " + e.getMessage(), e);
      }
    }

    public DurationShort build(long amount, TimeUnit timeUnit) {
      this.duration = Duration.of(amount, timeUnit.toChronoUnit());
      return new DurationShort(this);
    }

    public DurationShort build(Duration duration) {
      this.duration = duration;
      return new DurationShort(this);
    }
  }

  public enum DurationFormat {
    /**
     * hhhh:mm:ss.sss
     * where hhhh can span more than 24 hours
     */
    TIME_ISO,
    /**
     * (n)h(n)m(n)s.sss
     */
    DURATION_ISO,
    /**
     * n days n hours n minutes n seconds ss n milliseconds
     */
    DURATION_LONG,
  }

  public static class DurationHumanStringBuilder {

    private final DurationShort durationShort;

    private DurationFormat durationFormat = DurationFormat.TIME_ISO;

    public DurationHumanStringBuilder(DurationShort durationShort) {
      this.durationShort = durationShort;
    }


    public DurationHumanStringBuilder setFormat(DurationFormat durationFormat) {
      this.durationFormat = durationFormat;
      return this;
    }

    public String build() {
      Duration duration = this.durationShort.builder.duration;

      long minutesPart = duration.toMinutesPart();
      long secondsPart = duration.toSecondsPart();
      long millisPart = duration.toMillisPart();
      long days = duration.toDays();
      long hoursPart = duration.toHoursPart();
      long hours = duration.toHours();

      StringBuilder sb = new StringBuilder();
      switch (durationFormat) {
        case TIME_ISO:

          sb.append(hours).append(":");
          sb.append(minutesPart).append(":");
          sb.append(secondsPart);
          if (millisPart > 0) sb.append(".").append(millisPart);
          break;
        case DURATION_ISO:
          if (days > 0) sb.append(days).append("d");
          if (hoursPart > 0) sb.append(hoursPart).append("h");
          if (minutesPart > 0) sb.append(minutesPart).append("m");
          if (secondsPart > 0 || millisPart > 0) {
            sb.append(secondsPart);
            if (millisPart > 0) sb.append(".").append(millisPart);
            sb.append("s");
          }
          break;
        case DURATION_LONG:

          if (days > 0) sb.append(days).append(days == 1 ? " day " : " days ");
          if (hoursPart > 0) sb.append(hoursPart).append(hoursPart == 1 ? " hour " : " hours ");
          if (minutesPart > 0) sb.append(minutesPart).append(minutesPart == 1 ? " minute " : " minutes ");
          if (secondsPart > 0) sb.append(secondsPart).append(secondsPart == 1 ? " second " : " seconds ");
          if (millisPart > 0) sb.append(millisPart).append(millisPart == 1 ? " millisecond" : " milliseconds");
          break;
      }


      return sb.toString().trim();
    }
  }
}
