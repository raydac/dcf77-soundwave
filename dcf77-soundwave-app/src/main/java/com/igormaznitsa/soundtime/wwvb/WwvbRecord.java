package com.igormaznitsa.soundtime.wwvb;

import static java.time.ZoneOffset.UTC;

import com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRules;

public final class WwvbRecord extends AbstractMinuteBasedTimeSignalRecord {

  private static final ZoneId ZONE_MOUNTAIN = ZoneId.of("US/Mountain");

  public WwvbRecord(final ZonedDateTime time) {
    this(
        ensureTimezone(time, UTC).getMinute(),
        ensureTimezone(time, UTC).getHour(),
        ensureTimezone(time, UTC).getDayOfYear(),
        0,
        0.0f,
        ensureTimezone(time, UTC).getYear() % 100,
        ensureTimezone(time, UTC).toLocalDate().isLeapYear(),
        isLeapSecondAddedDuringTheMonth(ensureTimezone(time, UTC)),
        calculateDst(ensureTimezone(time, UTC))
    );
  }

  public WwvbRecord(
      final int minute,
      final int hour,
      final int dayOfYear,
      final int dut1sign,
      final float dut1,
      final int yearWithinCentury,
      final boolean leapYear,
      final boolean leapSecondAtEndOfMonth,
      final int dstStatus
  ) {
    super(
        makeTimePacketVersion(
            minute,
            hour,
            dayOfYear,
            dut1sign,
            dut1,
            yearWithinCentury,
            leapYear,
            leapSecondAtEndOfMonth,
            dstStatus
        )
    );
  }

  public WwvbRecord(final long wwvbBits, final boolean msb0) {
    super(msb0 ? reverseLowestBits(wwvbBits, 60) : wwvbBits);
  }

  public static int calculateDst(ZonedDateTime time) {
    final ZonedDateTime zonedDateTime = time.withZoneSameInstant(ZONE_MOUNTAIN);

    final LocalDate today = zonedDateTime.toLocalDate();

    final ZonedDateTime startOfToday = today.atStartOfDay(ZONE_MOUNTAIN);
    final ZonedDateTime endOfToday = today.plusDays(1).atStartOfDay(ZONE_MOUNTAIN).minusNanos(1);

    final ZoneRules rules = ZONE_MOUNTAIN.getRules();
    final boolean isDST = rules.isDaylightSavings(zonedDateTime.toInstant());
    final boolean wasDST = rules.isDaylightSavings(startOfToday.toInstant());
    final boolean isDSTAtEnd = rules.isDaylightSavings(endOfToday.toInstant());

    if (isDST) {
      return 0b11;
    } else if (wasDST && !isDSTAtEnd) {
      return 0b01;
    } else if (!wasDST && isDSTAtEnd) {
      return 0b10;
    } else {
      return 0b00;
    }
  }

  private static boolean isLeapSecondAddedDuringTheMonth(final ZonedDateTime time) {
    final Month month = time.getMonth();
    return month == Month.DECEMBER || month == Month.JUNE;
  }

  private static int toDut1(final float dut1) {
    return toBCD(Math.max(0, Math.min(9, Math.round(dut1 * 10))));
  }

  private static long makeTimePacketVersion(
      final int minutes,
      final int hours,
      final int dayOfYear,
      final int dut1sign,
      final float dut1,
      final int yearWithinCentury,
      final boolean leapYear,
      final boolean leapSecondAtEndOfMonth,
      final int dstStatus
  ) {
    long data = 0L;
    data = setBits(data, toBcdPadded5(minutes), 1, 0b11111111, true);
    data = setBits(data, toBcdPadded5(hours), 12, 0b1111111, true);
    data = setBits(data, toBcdPadded5(dayOfYear), 22, 0b111111111111, true);
    data = setBits(data, dut1sign, 36, 0b111, true);
    data = setBits(data, toDut1(dut1), 40, 0b1111, true);
    data = setBits(data, toBcdPadded5(yearWithinCentury), 45, 0b111111111, true);

    data = setBits(data, leapYear ? 1L : 0L, 55, 1, true);
    data = setBits(data, leapSecondAtEndOfMonth ? 1L : 0L, 56, 1, true);

    data = setBits(data, dstStatus, 57, 0b11, true);

    return data;
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
    final long mustBeZero = bits(bitString, 0, 1L)
        | bits(bitString, 4, 1L)
        | bits(bitString, 9, 1L)
        | bits(bitString, 14, 1L)
        | bits(bitString, 19, 1L)
        | bits(bitString, 24, 1L)
        | bits(bitString, 29, 1L)
        | bits(bitString, 39, 1L)
        | bits(bitString, 49, 1L)
        | bits(bitString, 59, 1L);
    return mustBeZero == 0L;
  }

  public int getMinutes() {
    final long bitString = this.getRawBitString();
    return (int) (
        bits(bitString, 1, 1) * 40
            + bits(bitString, 2, 1) * 20
            + bits(bitString, 3, 1) * 10
            + bits(bitString, 5, 1) * 8
            + bits(bitString, 6, 1) * 4
            + bits(bitString, 7, 1) * 2
            + bits(bitString, 8, 1)
    );
  }

  public int getHours() {
    final long bitString = this.getRawBitString();
    return (int) (
        bits(bitString, 12, 1) * 20
            + bits(bitString, 13, 1) * 10
            + bits(bitString, 15, 1) * 8
            + bits(bitString, 16, 1) * 4
            + bits(bitString, 17, 1) * 2
            + bits(bitString, 18, 1)
    );
  }

  public int getDayOfYear() {
    final long bitString = this.getRawBitString();
    return (int) (
        bits(bitString, 22, 1) * 200
            + bits(bitString, 23, 1) * 100
            + bits(bitString, 25, 1) * 80
            + bits(bitString, 26, 1) * 40
            + bits(bitString, 27, 1) * 20
            + bits(bitString, 28, 1) * 10
            + bits(bitString, 30, 1) * 8
            + bits(bitString, 31, 1) * 4
            + bits(bitString, 32, 1) * 2
            + bits(bitString, 33, 1)
    );
  }

  public int getYearInCentury() {
    final long bitString = this.getRawBitString();
    return (int) (
        bits(bitString, 45, 1) * 80
            + bits(bitString, 46, 1) * 40
            + bits(bitString, 47, 1) * 20
            + bits(bitString, 48, 1) * 10
            + bits(bitString, 50, 1) * 8
            + bits(bitString, 51, 1) * 4
            + bits(bitString, 52, 1) * 2
            + bits(bitString, 53, 1)
    );
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(this.getClass().getSimpleName());
    stringBuilder.append('(')
        .append("minute=").append(this.getMinutes())
        .append(",hour=").append(this.getHours())
        .append(",dayOfYear=").append(this.getDayOfYear())
        .append(",DUT1sign=")
        .append(String.format("%3s", Integer.toBinaryString(this.getDut1Sign())).replace(' ', '0'))
        .append(",DUT1=").append(this.getDut1())
        .append(",year=").append(this.getYearInCentury())
        .append(",leapYear=").append(this.isLeapYear() ? 1 : 0)
        .append(",leapSecondAtEndOfMonth=").append(this.isLeapSecondAtEndOfMonth() ? 1 : 0)
        .append(",DST=")
        .append(String.format("%2s", Integer.toBinaryString(this.getDst())).replace(' ', '0'));

    stringBuilder.append(')');
    return stringBuilder.toString();
  }

  public int getDst() {
    final long bitString = this.getRawBitString();
    return (int) bits(bitString, 57, 0b11);
  }

  public boolean isLeapSecondAtEndOfMonth() {
    final long bitString = this.getRawBitString();
    return bits(bitString, 56, 0b1) != 0L;
  }

  public boolean isLeapYear() {
    final long bitString = this.getRawBitString();
    return bits(bitString, 55, 0b1) != 0L;
  }

  public int getDut1Sign() {
    final long bitString = this.getRawBitString();
    return (int) bits(bitString, 36, 0b111);
  }

  public float getDut1() {
    final long bitString = this.getRawBitString();
    return bits(bitString, 40, 1) * 0.8f
        + bits(bitString, 41, 1) * 0.4f
        + bits(bitString, 42, 1) * 0.2f
        + bits(bitString, 43, 1) * 0.1f;
  }

  @Override
  public ZonedDateTime extractSourceTime() {
    final LocalDate localDate =
        LocalDate.ofYearDay(currentCentury() + this.getYearInCentury(), this.getDayOfYear());
    final LocalTime localTime = LocalTime.of(this.getHours(), this.getMinutes(), 0);
    return ZonedDateTime.of(localDate, localTime, UTC);
  }
}
