package com.igormaznitsa.soundtime;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TimeUtils {
  private TimeUtils() {

  }

  public static ZonedDateTime ensureTimezone(final ZonedDateTime time, final ZoneId zoneId) {
    if (time.getZone().equals(zoneId)) {
      return time;
    }
    return time.withZoneSameInstant(zoneId);
  }

}
