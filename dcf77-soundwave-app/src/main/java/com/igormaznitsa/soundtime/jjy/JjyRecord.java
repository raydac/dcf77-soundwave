package com.igormaznitsa.soundtime.jjy;

import com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class JjyRecord extends AbstractMinuteBasedTimeSignalRecord {

  static final ZoneId ZONE_JST = ZoneId.of("Asia/Tokyo");

  public JjyRecord(final ZonedDateTime time) {
    super(0L);
  }

  public JjyRecord(
      final boolean msb0,
      final int bcd5Minute,
      final int bcd5Hour,
      final int bcd5DayOfYear,
      final int bcd5YearWithinCentury,
      final int bcd5DayOfWeek,
      final boolean leapSecondAtCurrentUtcMonthEnd,
      final boolean leapSecondAdded
  ) {
    super(0L);
  }

  public JjyRecord(final long jjyBits, final boolean msb0) {
    super(msb0 ? reverseLowestBits(jjyBits, 60) : jjyBits);
  }

  public static ZonedDateTime ensureJst(final ZonedDateTime time) {
    if (time.getZone().equals(ZONE_JST)) {
      return time;
    }
    return time.withZoneSameInstant(ZONE_JST);
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(this.getClass().getSimpleName());
    stringBuilder.append('(');
    stringBuilder.append(')');
    return stringBuilder.toString();
  }

  public long getBitString(final boolean msb0) {
    if (msb0) {
      return reverseLowestBits(this.getRawBitString(), 60);
    } else {
      return this.getRawBitString();
    }
  }

  @Override
  public boolean isValid() {
    final long bitString = this.getRawBitString();
    if (
        bits(bitString, 0, 1L) != 0L
            || bits(bitString, 59, 1L) != 0L
            || bits(bitString, 20, 1L) == 0L
    ) {
      return false;
    }

    if ((calcEvenParity(bitString, 21, 28) ? 1L : 0L) != bits(bitString, 28, 1L)) {
      return false;
    }
    if ((calcEvenParity(bitString, 29, 35) ? 1L : 0L) != bits(bitString, 35, 1L)) {
      return false;
    }
    return (calcEvenParity(bitString, 36, 58) ? 1L : 0L) == bits(bitString, 58, 1L);
  }

}
