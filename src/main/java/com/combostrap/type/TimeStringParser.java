package com.combostrap.type;


import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * General base class for static method around date
 * This is not a static class for a specific function
 * and therefore is not public but package-private
 */
public class TimeStringParser {


  /**
   * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
   * format is unknown. You can simply extend DateUtil with more formats if needed.
   *
   * @param s The date string to determine the SimpleDateFormat pattern for.
   * @return The matching SimpleDateFormat pattern, or null if format is unknown.
   * @see SimpleDateFormat
   */
  public static String detectFormat(String s) throws TimeStringCastException {
    for (Pattern regexpPattern : DATE_FORMAT_REGEXPS.keySet()) {
      if (regexpPattern.matcher(s).find()) {
        return DATE_FORMAT_REGEXPS.get(regexpPattern);
      }
    }
    throw new TimeStringCastException("The string (" + s + ") has a format that is not recognized as a date");
  }

  /**
   * All regexps date format recognized by the function {@link #detectFormat(String)}
   *
   * <a href=https://stackoverflow.com/questions/3389348/parse-any-date-in-java>Regular expression list source</a>
   * <p>
   * Java 8 uses uuuu for year, not yyyy.
   * In Java 8, yyyy means "year of era" (BC or AD) and the error message complains
   * that MonthOfYear, DayOfMonth and YearOfEra is not enough information to construct the date because era is not known.
   */
  private static final Map<Pattern, String> DATE_FORMAT_REGEXPS = new HashMap<>() {{
      // Iso date, not uuuu in place of yyyy. it does not work with 2020-10-12
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}$"), "yyyy-MM-dd");
      // SQL timestamp with 1, 2 or 3 digits
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{9}$"), "yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{8}$"), "yyyy-MM-dd HH:mm:ss.SSSSSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{7}$"), "yyyy-MM-dd HH:mm:ss.SSSSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{6}$"), "yyyy-MM-dd HH:mm:ss.SSSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{5}$"), "yyyy-MM-dd HH:mm:ss.SSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{4}$"), "yyyy-MM-dd HH:mm:ss.SSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$"), "yyyy-MM-dd HH:mm:ss.SSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{2}$"), "yyyy-MM-dd HH:mm:ss.SS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{1}$"), "yyyy-MM-dd HH:mm:ss.S");
      /**
       * Iso timestamp
       * See {@link LocalDateTime#toString()}
       */
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{9}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{8}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{7}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{6}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{5}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{4}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$"), "yyyy-MM-dd'T'HH:mm:ss.SSS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{2}$"), "yyyy-MM-dd'T'HH:mm:ss.SS");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1}$"), "yyyy-MM-dd'T'HH:mm:ss.S");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}$"), "yyyy-MM-dd'T'HH:mm:ss");
      // others
      put(Pattern.compile("^\\d{8}$"), "yyyyMMdd");
      put(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}$"), "dd-MM-yyyy");
      put(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}$"), "MM/dd/yyyy");
      put(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}$"), "yyyy/MM/dd");
      put(Pattern.compile("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$"), "dd MMM yyyy");
      put(Pattern.compile("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$"), "dd MMMM yyyy");
      put(Pattern.compile("^\\d{12}$"), "yyyyMMddHHmm");
      put(Pattern.compile("^\\d{8}\\s\\d{4}$"), "yyyyMMdd HHmm");
      put(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd-MM-yyyy HH:mm");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$"), "yyyy-MM-dd HH:mm");
      put(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$"), "MM/dd/yyyy HH:mm");
      put(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$"), "yyyy/MM/dd HH:mm");
      put(Pattern.compile("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd MMM yyyy HH:mm");
      put(Pattern.compile("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd MMMM yyyy HH:mm");
      put(Pattern.compile("^\\d{14}$"), "yyyyMMddHHmmss");
      put(Pattern.compile("^\\d{8}\\s\\d{6}$"), "yyyyMMdd HHmmss");
      put(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "dd-MM-yyyy HH:mm:ss");
      put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "yyyy-MM-dd HH:mm:ss");
      put(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "MM/dd/yyyy HH:mm:ss");
      put(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "yyyy/MM/dd HH:mm:ss");
      put(Pattern.compile("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "dd MMM yyyy HH:mm:ss");
      put(Pattern.compile("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "dd MMMM yyyy HH:mm:ss");
  }};


}
