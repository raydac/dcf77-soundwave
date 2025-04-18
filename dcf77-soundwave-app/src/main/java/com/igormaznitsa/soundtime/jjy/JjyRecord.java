package com.igormaznitsa.soundtime.jjy;

import com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class JjyRecord extends AbstractMinuteBasedTimeSignalRecord {

  static final ZoneId ZONE_JST = ZoneId.of("Asia/Tokyo");

  private final boolean callSignAnnouncementPacket;

  public JjyRecord(final ZonedDateTime time) {
    this(
        ensureTimezone(time, ZONE_JST).getMinute(),
        ensureTimezone(time, ZONE_JST).getHour(),
        ensureTimezone(time, ZONE_JST).getDayOfYear(),
        ensureTimezone(time, ZONE_JST).getYear() % 100,
        ensureTimezone(time, ZONE_JST).getDayOfWeek().getValue() % 7,
        false,
        false
    );
  }

  public JjyRecord(
      final int minute,
      final int hour,
      final int dayOfYear,
      final int yearWithinCentury,
      final int dayOfWeek,
      final boolean leapSecondAtCurrentUtcMonthEnd,
      final boolean leapSecondAdded
  ) {
    super(makeTimePacketVersion1(minute, hour, dayOfYear, yearWithinCentury, dayOfWeek,
        leapSecondAtCurrentUtcMonthEnd, leapSecondAdded));
    this.callSignAnnouncementPacket = false;
  }

  public JjyRecord(
      final int minute,
      final int hour,
      final int dayOfYear,
      final int callSignAnnouncement,
      final int serviceInterruptionScheduled,
      final boolean serviceInterruptionDaytimeOnly,
      final int serviceInterruptionDuration
  ) {
    super(makeTimePacketVersion2(
            minute,
            hour,
            dayOfYear,
            callSignAnnouncement,
            serviceInterruptionScheduled,
            serviceInterruptionDaytimeOnly,
            serviceInterruptionDuration
        )
    );
    this.callSignAnnouncementPacket = true;
  }

  public JjyRecord(final long jjyBits, final boolean msb0) {
    super(msb0 ? reverseLowestBits(jjyBits, 60) : jjyBits);
    this.callSignAnnouncementPacket = isCallSignAnnouncementMinute(this.getMinutes());
  }

  public static boolean isCallSignAnnouncementMinute(final int minute) {
    return minute == 15 || minute == 45;
  }

  public static JjyRecord makeWithAnnounceCallSignAwareness(final ZonedDateTime zonedDateTime) {
    final ZonedDateTime jstTime = ensureTimezone(zonedDateTime, ZONE_JST);
    if (isCallSignAnnouncementMinute(jstTime.getMinute())) {
      return new JjyRecord(
          jstTime.getMinute(),
          jstTime.getHour(),
          jstTime.getDayOfYear(),
          0,
          0,
          false,
          0
      );
    } else {
      return new JjyRecord(jstTime);
    }
  }

  private static long makeTimePacketVersion1(
      final int minute,
      final int hour,
      final int dayOfYear,
      final int yearWithinCentury,
      final int dayOfWeek,
      final boolean leapSecondAtCurrentUtcMonthEnd,
      final boolean leapSecondAdded
  ) {
    long data = 0L;
    data = setBits(data, toBcdPadded5(minute), 1, 0b11111111, true);
    data = setBits(data, toBcdPadded5(hour), 12, 0b1111111, true);
    data = setBits(data, toBcdPadded5(dayOfYear), 22, 0b111111111111, true);
    data = setBits(data, toBCD(yearWithinCentury), 41, 0b11111111, true);
    data = setBits(data, toBcdPadded5(dayOfWeek), 50, 0b111, true);

    data = setBits(data, calcEvenParityOverBits(data, 12, 18) ? 0L : 1L, 36, 1, true);
    data = setBits(data, calcEvenParityOverBits(data, 1, 8) ? 0L : 1L, 37, 1, true);

    data = setBits(data, leapSecondAtCurrentUtcMonthEnd ? 1L : 0L, 53, 1, true);
    data = setBits(data, leapSecondAdded ? 1L : 0L, 54, 1, true);

    return data;
  }

  private static long makeTimePacketVersion2(
      final int minute,
      final int hour,
      final int dayOfYear,
      final int callSignAnnouncement,
      final int serviceInterruptionScheduled,
      final boolean serviceInterruptionDaytimeOnly,
      final int serviceInterruptionDuration
  ) {
    long data = 0L;
    data = setBits(data, toBcdPadded5(minute), 1, 0b11111111, true);
    data = setBits(data, toBcdPadded5(hour), 12, 0b1111111, true);
    data = setBits(data, toBcdPadded5(dayOfYear), 22, 0b111111111111, true);

    data = setBits(data, calcEvenParityOverBits(data, 12, 18) ? 0L : 1L, 36, 1, true);
    data = setBits(data, calcEvenParityOverBits(data, 1, 8) ? 0L : 1L, 37, 1, true);

    data = setBits(data, toBCD(callSignAnnouncement), 40, 0b111111111, true);
    data = setBits(data, serviceInterruptionScheduled, 50, 0b111, true);
    data = setBits(data, serviceInterruptionDaytimeOnly ? 1L : 0L, 53, 1, true);
    data = setBits(data, serviceInterruptionDuration, 54, 0b11, true);

    return data;
  }

  public boolean isCallSignAnnouncement() {
    return this.callSignAnnouncementPacket;
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
    if (mustBeZero != 0L) {
      return false;
    }

    if (calcEvenParityOverBits(bitString, 12, 18) == this.isPA1()) {
      return false;
    }
    return calcEvenParityOverBits(bitString, 1, 8) != this.isPA2();
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
        bits(bitString, 41, 1) * 80
            + bits(bitString, 42, 1) * 40
            + bits(bitString, 43, 1) * 20
            + bits(bitString, 44, 1) * 10
            + bits(bitString, 45, 1) * 8
            + bits(bitString, 46, 1) * 4
            + bits(bitString, 47, 1) * 2
            + bits(bitString, 48, 1)
    );
  }

  public int getDayOfWeek() {
    final long bitString = this.getRawBitString();
    return (int) (
        bits(bitString, 50, 1) * 4
            + bits(bitString, 51, 1) * 2
            + bits(bitString, 52, 1)
    );
  }

  public boolean isLeapSecondAtCurrentUtcMonthEnd() {
    return bits(this.getRawBitString(), 53, 1) != 0L;
  }

  public boolean isLeapSecondAdded() {
    return bits(this.getRawBitString(), 54, 1) != 0L;
  }

  public boolean isPA1() {
    return bits(this.getRawBitString(), 36, 1) != 0L;
  }

  public boolean isPA2() {
    return bits(this.getRawBitString(), 37, 1) != 0L;
  }

  public int getServiceInterruptionScheduled() {
    return (int) bits(this.getRawBitString(), 50, 0b111);
  }

  public boolean isServiceInterruptionDaytimeOnly() {
    return bits(this.getRawBitString(), 53, 1) != 0L;
  }

  public int getServiceInterruptionDuration() {
    return (int) bits(this.getRawBitString(), 54, 0b11);
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(this.getClass().getSimpleName());
    stringBuilder.append('(')
        .append("minute=").append(this.getMinutes())
        .append(",hour=").append(this.getHours())
        .append(",dayOfYear=").append(this.getDayOfYear())
        .append(",PA1=").append(this.isPA1() ? 1 : 0)
        .append(",PA2=").append(this.isPA2() ? 1 : 0);

    if (this.isCallSignAnnouncement()) {
      stringBuilder
          .append(",siScheduled=").append(this.getServiceInterruptionScheduled())
          .append(",siDaytimeOnly=").append(this.isServiceInterruptionDaytimeOnly() ? 1 : 9)
          .append(",siDuration=").append(this.getServiceInterruptionDuration());
    } else {
      stringBuilder
          .append(",year=").append(this.getYearInCentury())
          .append(",dayOfWeek=").append(this.getDayOfWeek())
          .append(",leapSecond=").append(this.isLeapSecondAtCurrentUtcMonthEnd() ? 1 : 0)
          .append(",leapSecondAdded=").append(this.isLeapSecondAdded() ? 1 : 0);
    }
    stringBuilder.append(')');
    return stringBuilder.toString();
  }

  @Override
  public ZonedDateTime extractSourceTime() {
    final LocalDate localDate =
        LocalDate.ofYearDay(currentCentury() + this.getYearInCentury(), this.getDayOfYear());
    final LocalTime localTime = LocalTime.of(this.getHours(), this.getMinutes(), 0);
    return ZonedDateTime.of(localDate, localTime, ZONE_JST);
  }
}
