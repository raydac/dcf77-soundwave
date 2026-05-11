package com.igormaznitsa.soundtime.bpc;

import static com.igormaznitsa.soundtime.bpc.BpcMinuteBasedTimeSignalSignalRenderer.ZONE_CHN;

import com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord;
import java.time.ZonedDateTime;

public final class BpcRecord extends AbstractMinuteBasedTimeSignalRecord {

  private final BcpBitString bcpBitString;

  public BpcRecord(final ZonedDateTime time) {
    this(
        time.getHour(),
        time.getMinute(),
        time.getDayOfWeek().getValue(),
        time.getDayOfMonth(),
        time.getMonth().getValue(),
        time.getYear() % 100,
        time.getSecond()
    );
  }

  public BpcRecord(
      final int hour,
      final int minute,
      final int dayOfWeek,
      final int dayOfMonth,
      final int month,
      final int yearWithinCentury,
      final int second
  ) {
    super(-1L, second);
    final int checkedHour = requireInclusiveRange(hour, 0, 23, "Hour");
    final int checkedMinute = requireInclusiveRange(minute, 0, 59, "Minute");
    final int checkedDayOfWeek = requireInclusiveRange(dayOfWeek, 1, 7, "Day of week");
    final int checkedDayOfMonth = requireInclusiveRange(dayOfMonth, 1, 31, "Day of month");
    final int checkedMonth = requireInclusiveRange(month, 1, 12, "Month");
    final int checkedYearWithinCentury = requireInclusiveRange(yearWithinCentury, 0, 99,
        "Year within century");

    this.bcpBitString = new BcpBitString(makeTimePacketVersion(
        checkedHour,
        checkedMinute,
        checkedDayOfWeek,
        checkedDayOfMonth,
        checkedMonth,
        checkedYearWithinCentury
    ));
  }

  public BpcRecord(final String bits) {
    super(-1L, 0);
    this.bcpBitString =
        new BcpBitString(bits);
  }

  private static String makeTimePacketVersion(
      final int hours,
      final int minutes,
      final int dayOfWeek,
      final int dayOfMonth,
      final int month,
      final int year
  ) {
    final StringBuilder buffer = new StringBuilder(120);

    for (int i = 0; i < 3; i++) {
      final int offset = i * 40;

      // seconds
      switch (i) {
        case 0:
          buffer.append("00");
          break;
        case 1:
          buffer.append("01");
          break;
        case 2:
          buffer.append("10");
          break;
        default:
          throw new Error("Unexpected");
      }

      // unused
      buffer.append("00");

      // hours
      writeBinaryInt(buffer, toBpcHour(hours), 4);

      // minute
      writeBinaryInt(buffer, minutes, 6);

      // unused
      buffer.append('0');

      // day of week
      writeBinaryInt(buffer, dayOfWeek, 3);

      // PM
      buffer.append(hours >= 12 ? '1' : '0');

      // P1
      buffer.append(isEven(buffer, offset, offset + 18) ? '1' : '0');

      // unused
      buffer.append('0');

      // day of month
      writeBinaryInt(buffer, dayOfMonth, 5);

      // month
      writeBinaryInt(buffer, month, 4);

      // year
      writeBinaryInt(buffer, year & 0b111111, 6);
      buffer.append((year & 0b1000000) == 0 ? '0' : '1');

      // P2
      buffer.append(isEven(buffer, offset + 20, offset + 36) ? '1' : '0');

      // synchro
      buffer.append("00");
    }

    return buffer.toString();
  }

  private static void writeBinaryInt(final StringBuilder builder, final int value, final int bits) {
    final String bin = Integer.toBinaryString(value);
    int zeros = bits - bin.length();

    if (zeros < 0) {
      throw new IllegalArgumentException("Unexpected value " + value + " for " + bits + " bits(s)");
    }

    while (zeros > 0) {
      builder.append('0');
      zeros--;
    }
    builder.append(bin);
  }

  private static boolean isEven(final CharSequence charSequence, final int from, final int to) {
    int count = 0;
    for (int i = from; i < to; i++) {
      if (charSequence.charAt(i) == '1') {
        count++;
      }
    }
    return count % 2 != 0;
  }

  private static int toBpcHour(final int hours) {
    if (hours < 0 || hours > 23) {
      throw new IllegalArgumentException("Hour must be 0..23: " + hours);
    }
    final int hourIn12 = hours % 12;
    return hourIn12 == 0 ? 12 : hourIn12;
  }

  private static int to24Hour(final int hourIn12, final boolean pm) {
    if (hourIn12 < 0 || hourIn12 > 12) {
      throw new IllegalStateException("Decoded BPC hour must be 0..12: " + hourIn12);
    }
    if (hourIn12 == 12) {
      return pm ? 12 : 0;
    }
    return pm ? hourIn12 + 12 : hourIn12;
  }

  private static int requireInclusiveRange(
      final int value,
      final int min,
      final int max,
      final String fieldName
  ) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(fieldName + " must be " + min + ".." + max + ": " + value);
    }
    return value;
  }

  public long getBitString(final boolean msb0) {
    throw new UnsupportedOperationException("Unsupported for BCP packet");
  }

  public BcpBitString getBcpBitString() {
    return this.bcpBitString;
  }

  @Override
  public boolean isValid() {
    if (!super.isValid()) {
      return false;
    }

    if (this.bcpBitString.getBitPair(0) != 0
        || this.bcpBitString.getBitPair(20) != 1
        || this.bcpBitString.getBitPair(40) != 2
    ) {
      return false;
    }

    for (int i = 0; i < 3; i++) {
      final int base = i * 20;
      final boolean p1 = this.bcpBitString.isEvenDiapason(base, base + 8);
      final boolean p2 = this.bcpBitString.isEvenDiapason(base + 10, base + 17);
      if (p1 != ((this.bcpBitString.getBitPair(base + 9) & 1) == 1)) {
        return false;
      }
      if (p2 != ((this.bcpBitString.getBitPair(base + 18) & 1) == 1)) {
        return false;
      }
    }

    return this.getHours() >= 1 && this.getHours() <= 12
        && this.getMinutes() >= 0 && this.getMinutes() <= 59
        && this.getDayOfWeek() >= 1 && this.getDayOfWeek() <= 7
        && this.getDayOfMonth() >= 1 && this.getDayOfMonth() <= 31
        && this.getMonth() >= 1 && this.getMonth() <= 12
        && this.getYearInCentury() >= 0 && this.getYearInCentury() <= 99;
  }

  public boolean isPM() {
    return (this.bcpBitString.getBitPair(9) & 0b10) != 0;
  }

  public int getMonth() {
    return this.bcpBitString.readDiapason(13, 14);
  }

  public int getMinutes() {
    return this.bcpBitString.readDiapason(4, 6);
  }

  public int getHours() {
    return this.bcpBitString.readDiapason(2, 3);
  }

  public int getDayOfWeek() {
    return this.bcpBitString.readDiapason(7, 8);
  }

  public int getDayOfMonth() {
    return this.bcpBitString.readDiapason(10, 12);
  }

  public int getYearInCentury() {
    int year = this.bcpBitString.readDiapason(15, 17);
    return ((this.bcpBitString.getBitPair(18) >> 1) << 6) | year;
  }

  @Override
  public String toString() {
    final int hour24 = to24Hour(this.getHours(), this.isPM());
    final StringBuilder buffer = new StringBuilder(this.getClass().getSimpleName());
    buffer.append('(')
        .append(",hours=").append(hour24)
        .append(",minutes=").append(this.getMinutes())
        .append(",dayOfWeek=").append(this.getDayOfWeek())
        .append(",dayOfMonth=").append(this.getDayOfMonth())
        .append(",month=").append(this.getMonth())
        .append(",year=").append(this.getYearInCentury());
    buffer.append(')');
    return buffer.toString();
  }

  @Override
  public String toBinaryString(boolean msb0) {
    final StringBuilder builder = new StringBuilder(120);
    for (int i = 0; i < 60; i++) {
      final int value = this.bcpBitString.getBitPair(i);
      if (msb0) {
        switch (value) {
          case 0:
            builder.append("00");
            break;
          case 1:
            builder.append("10");
            break;
          case 2:
            builder.append("01");
            break;
          case 3:
            builder.append("11");
            break;
        }
      } else {
        switch (value) {
          case 0:
            builder.append("00");
            break;
          case 1:
            builder.append("01");
            break;
          case 2:
            builder.append("10");
            break;
          case 3:
            builder.append("11");
            break;
        }
      }
    }
    return builder.toString();
  }

  @Override
  public ZonedDateTime extractSourceTime() {
    final int hour24 = to24Hour(this.getHours(), this.isPM());
    return ZonedDateTime.of(
        this.getYearInCentury() + currentCentury(),
        this.getMonth(),
        this.getDayOfMonth(),
        hour24,
        this.getMinutes(),
        this.getSecond(),
        0,
        ZONE_CHN
    );
  }

}
