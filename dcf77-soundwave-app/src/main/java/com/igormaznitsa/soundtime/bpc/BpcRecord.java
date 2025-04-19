package com.igormaznitsa.soundtime.bpc;

import static com.igormaznitsa.soundtime.bpc.BpcMinuteBasedTimeSignalSignalRenderer.ZONE_CHN;

import com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord;
import java.time.ZonedDateTime;

public final class BpcRecord extends AbstractMinuteBasedTimeSignalRecord {

  private final BcpBitString bcpBitString;

  public BpcRecord(final ZonedDateTime time) {
    this(
        ensureTimezone(time, ZONE_CHN).getHour(),
        ensureTimezone(time, ZONE_CHN).getMinute(),
        ensureTimezone(time, ZONE_CHN).getDayOfWeek().getValue(),
        ensureTimezone(time, ZONE_CHN).getDayOfMonth(),
        ensureTimezone(time, ZONE_CHN).getMonth().getValue(),
        ensureTimezone(time, ZONE_CHN).getYear() % 100
    );
  }

  public BpcRecord(
      final int hour,
      final int minute,
      final int dayOfWeek,
      final int dayOfMonth,
      final int month,
      final int yearWithinCentury
  ) {
    super(-1L);
    this.bcpBitString = new BcpBitString(makeTimePacketVersion(
        hour,
        minute,
        dayOfWeek,
        dayOfMonth,
        month,
        yearWithinCentury
    ));
  }

  public BpcRecord(final String bits) {
    super(-1L);
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
      writeBinaryInt(buffer, hours % 12, 4);

      // minute
      writeBinaryInt(buffer, minutes, 6);

      // unused
      buffer.append('0');

      // day of week
      writeBinaryInt(buffer, dayOfWeek, 3);

      // PM
      buffer.append(hours > 12 ? '1' : '0');

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

  public long getBitString(final boolean msb0) {
    throw new UnsupportedOperationException("Unsupported for BCP packet");
  }

  public BcpBitString getBcpBitString() {
    return this.bcpBitString;
  }

  @Override
  public boolean isValid() {
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

    return true;
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
    final StringBuilder buffer = new StringBuilder(this.getClass().getSimpleName());
    buffer.append('(')
        .append(",hours=").append(this.getHours() + (this.isPM() ? 12 : 0))
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
    return ZonedDateTime.of(
        this.getYearInCentury() + currentCentury(),
        this.getMonth(),
        this.getDayOfMonth(),
        this.getHours() + (this.isPM() ? 12 : 0),
        this.getMinutes(),
        0,
        0,
        ZONE_CHN
    );
  }

}
