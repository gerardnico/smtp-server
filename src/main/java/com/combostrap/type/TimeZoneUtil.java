package com.combostrap.type;

import java.time.ZoneId;
import java.util.TimeZone;

public class TimeZoneUtil {


  /**
   * This utility throws if the time zone string is not correct.
   * It's an addition to {@link TimeZone#getTimeZone(String)}
   * that does not throw but returns the default zone
   *
   * @param timeZoneString - the time zone id in string format
   * @return a time zone
   * @throws TimeZoneCast - if the time zone is not valid
   */
  public static TimeZone getTimeZoneWithValidation(String timeZoneString) throws TimeZoneCast {

    ZoneId zoneId;
    try {
      /**
       * Zone id throw if it can not find the zone
       *
       */
      zoneId = ZoneId.of(timeZoneString);
    } catch (Exception e) {
      // bad zone id
      throw new TimeZoneCast();
    }
    return TimeZone.getTimeZone(zoneId);
  }

  public static TimeZone getTimeZoneFailSafe(String zoneId) {
    try {
      return getTimeZoneWithValidation(zoneId);
    } catch (TimeZoneCast e) {
      throw new RuntimeException("The timezone (" + zoneId + ") is not valid");
    }
  }
}
