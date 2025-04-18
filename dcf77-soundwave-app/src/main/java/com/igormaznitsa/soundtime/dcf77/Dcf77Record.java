package com.igormaznitsa.soundtime.dcf77;

import com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Implementation of DCF77 record.
 *
 * @author Igor Maznitsa
 * @see <a href=”https://www.ptb.de/cms/fileadmin/internet/fachabteilungen/abteilung_4/4.4_zeit_und_frequenz/pdf/2004_Piester_-_PTB-Mitteilungen_114.pdf”>DCF77 specification</a>
 */
public final class Dcf77Record extends AbstractMinuteBasedTimeSignalRecord {
  /**
   * Standard time zone for DCF77 signal.
   */
  static final ZoneId ZONE_CET = ZoneId.of("CET");

  /**
   * Create record for now.
   */
  public Dcf77Record() {
    this(ZonedDateTime.now(ZONE_CET));
  }

  /**
   * Create for provided zoned data time.
   *
   * @param time base time to create record
   * @throws NullPointerException thrown if time is null
   */
  public Dcf77Record(final ZonedDateTime time) {
    this(
        false,
        0,
        false,
        false,
        ZONE_CET.getRules().isDaylightSavings(time.toInstant()),
        !ZONE_CET.getRules().isDaylightSavings(time.toInstant()),
        false,
        toBCD(ensureTimezone(time, ZONE_CET).getMinute()),
        toBCD(ensureTimezone(time, ZONE_CET).getHour()),
        toBCD(ensureTimezone(time, ZONE_CET).getDayOfMonth()),
        toBCD(ensureTimezone(time, ZONE_CET).getDayOfWeek().getValue()),
        toBCD(ensureTimezone(time, ZONE_CET).getMonthValue()),
        toBCD(ensureTimezone(time, ZONE_CET).getYear() % 100)
    );
  }

  /**
   * Create record for fields.
   *
   * @param msb0                   if true then data provided in msb0 format.
   * @param civilWarningBits       Civil warning bits.
   * @param callBit                Call bit, abnormal transmitter operation.
   * @param summerTimeAnnouncement Summer/standard time changeover announcement. Set during hour before change.
   * @param cest                   Set to 1 when CEST is in effect.
   * @param cet                    Set to 1 when CET is in effect.
   * @param leapSecondAnnouncement Leap second announcement. Set during hour before leap second.
   * @param bcdMinutes             BCD encoded minutes (00-59)
   * @param bcdHours               BCD encoded hours (0-23)
   * @param bcdDayOfMonth          BCD encoded day of month (01-31)
   * @param bcdDayOfWeek           BCD encoded day of week (1-7)
   * @param bcdMonthNumber         BCD encoded month number (01-12)
   * @param bcdYearWithinCentury   BCD year encoded within century (00-99)
   */
  public Dcf77Record(
      final boolean msb0,
      final int civilWarningBits,
      final boolean callBit,
      final boolean summerTimeAnnouncement,
      final boolean cest,
      final boolean cet,
      final boolean leapSecondAnnouncement,
      final int bcdMinutes,
      final int bcdHours,
      final int bcdDayOfMonth,
      final int bcdDayOfWeek,
      final int bcdMonthNumber,
      final int bcdYearWithinCentury
  ) {
    super(makeData(
        msb0,
        civilWarningBits,
        callBit,
        summerTimeAnnouncement,
        cest,
        cet,
        leapSecondAnnouncement,
        bcdMinutes,
        bcdHours,
        bcdDayOfMonth,
        bcdDayOfWeek,
        bcdMonthNumber,
        bcdYearWithinCentury
    ));
  }

  /**
   * Create from DCF77 bit vector
   *
   * @param dcf77bits bit vector
   * @param msb0      if true then vector in MSB0, LSB0 otherwise
   */
  public Dcf77Record(final long dcf77bits, final boolean msb0) {
    super(msb0 ? reverseLowestBits(dcf77bits, 60) : dcf77bits);
  }

  private static long makeData(
      final boolean msb0,
      final int civilWarningBits,
      final boolean callBit,
      final boolean summerTimeAnnouncement,
      final boolean cest,
      final boolean cet,
      final boolean leapSecondAnnouncement,
      final int bcdMinutes,
      final int bcdHours,
      final int bcdDayOfMonth,
      final int bcdDayOfWeek,
      final int bcdMonthNumber,
      final int bcdYearWithinCentury
  ) {
    long data = setBits(0L, civilWarningBits, 1, 0b11111111111111L, msb0);
    data = setBits(data, callBit ? 1L : 0L, 15, 1L, msb0);
    data = setBits(data, summerTimeAnnouncement ? 1L : 0L, 16, 1L, msb0);
    data = setBits(data, cest ? 1L : 0L, 17, 1L, msb0);
    data = setBits(data, cet ? 1L : 0L, 18, 1L, msb0);
    data = setBits(data, leapSecondAnnouncement ? 1L : 0L, 19, 1L, msb0);
    data = setBits(data, 1L, 20, 1L, msb0);
    data = setBits(data, bcdMinutes, 21, 0b111_111_1L, msb0);
    data = setBits(data, bcdHours, 29, 0b111_111L, msb0);
    data = setBits(data, bcdDayOfMonth, 36, 0b111_111L, msb0);
    data = setBits(data, bcdDayOfWeek, 42, 0b111L, msb0);
    data = setBits(data, bcdMonthNumber, 45, 0b11111L, msb0);
    data = setBits(data, bcdYearWithinCentury, 50, 0b1111_1111L, msb0);

    data = setBits(data, calcEvenParityOverBits(data, 21, 28) ? 1L : 0L, 28, 1L, msb0);
    data = setBits(data, calcEvenParityOverBits(data, 29, 35) ? 1L : 0L, 35, 1L, msb0);
    data = setBits(data, calcEvenParityOverBits(data, 36, 58) ? 1L : 0L, 58, 1L, msb0);

    return data;
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(this.getClass().getSimpleName());
    stringBuilder.append('(')
        .append("CWB=").append(this.getCivilWarningBits())
        .append(",R=").append(this.isCallBit() ? 1 : 0)
        .append(",A1=").append(this.isSummerTimeAnnouncement() ? 1 : 0)
        .append(",Z1=").append(this.isCest() ? 1 : 0)
        .append(",Z2=").append(this.isCet() ? 1 : 0)
        .append(",A2=").append(this.isLeapSecondAnnouncement() ? 1 : 0)
        .append(",Minute=").append(this.getMinute())
        .append(",Hour=").append(this.getHour())
        .append(",DayOfMonth=").append(this.getDayOfMonth())
        .append(",DayOfWeek=").append(this.getDayOfWeek())
        .append(",Month=").append(this.getMonth())
        .append(",Year=").append(this.getYearWithinCentury());
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

  /**
   * Get civil warning bits from bit vector in LSB0 format.
   *
   * @return civil warning bits as LSB0
   */
  public long getCivilWarningBits() {
    return reverseLowestBits(bits(this.getRawBitString(), 1, 0b11_1111_1111_1111L), 14);
  }

  /**
   * Return call bit
   *
   * @return true or false
   */
  public boolean isCallBit() {
    return bits(this.getRawBitString(), 15, 1L) != 0L;
  }

  /**
   * Return summer announcement.
   *
   * @return summer announcement
   */
  public boolean isSummerTimeAnnouncement() {
    return bits(this.getRawBitString(), 16, 1L) != 0L;
  }

  /**
   * Return CEST flag.
   *
   * @return true or false
   */
  public boolean isCest() {
    return bits(this.getRawBitString(), 17, 1L) != 0L;
  }

  /**
   * Return CET flag.
   *
   * @return true or false
   */
  public boolean isCet() {
    return bits(this.getRawBitString(), 18, 1L) != 0L;
  }

  /**
   * Return leap second announcement flag.
   *
   * @return true or false
   */
  public boolean isLeapSecondAnnouncement() {
    return bits(this.getRawBitString(), 19, 1L) != 0L;
  }

  /**
   * Return minute start bit flag.
   *
   * @return true if valid packet
   */
  public boolean isMinuteStartBit() {
    return bits(this.getRawBitString(), 20, 1L) != 0L;
  }

  /**
   * Get decoded minute value
   *
   * @return the decoded minute value 00-59
   */
  public int getMinute() {
    return fromBCD((int) this.getMinuteRaw());
  }

  /**
   * Get raw LSB0 minute value
   *
   * @return the LSB0 raw minute value
   */
  public long getMinuteRaw() {
    return (int) reverseLowestBits(bits(this.getRawBitString(), 21, 0b1111111L), 7);
  }

  public boolean isMinuteEvenParity() {
    return bits(this.getRawBitString(), 28, 0b1L) != 0L;
  }

  /**
   * Get decoded hour value.
   *
   * @return the decoded hour value 00-23
   */
  public int getHour() {
    return fromBCD((int) this.getHourRaw());
  }

  /**
   * Get raw LSB0 hour value
   *
   * @return the LSB0 raw hour value
   */
  public long getHourRaw() {
    return reverseLowestBits(bits(this.getRawBitString(), 29, 0b111111L), 6);
  }

  public boolean isHourEvenParity() {
    return bits(this.getRawBitString(), 35, 0b1L) != 0L;
  }

  /**
   * Get decoded day of month value.
   *
   * @return the decoded day of month value 1-31
   */
  public int getDayOfMonth() {
    return fromBCD((int) this.getDayOfMonthRaw());
  }

  /**
   * Get raw LSB0 day of month value
   *
   * @return the LSB0 raw day of month value
   */
  public long getDayOfMonthRaw() {
    return reverseLowestBits(bits(this.getRawBitString(), 36, 0b111111L), 6);
  }

  /**
   * Get decoded day of week value.
   *
   * @return the decoded day of week value 1-7
   */
  public int getDayOfWeek() {
    return fromBCD((int) this.getDayOfWeekRaw());
  }

  /**
   * Get raw LSB0 day of week value
   *
   * @return the LSB0 raw day of week value
   */
  public long getDayOfWeekRaw() {
    return reverseLowestBits(bits(this.getRawBitString(), 42, 0b111L), 3);
  }

  /**
   * Get decoded month value.
   *
   * @return the decoded month value 1-12
   */
  public int getMonth() {
    return fromBCD((int) this.getMonthRaw());
  }

  /**
   * Get raw LSB0 month value
   *
   * @return the LSB0 raw month value
   */
  public long getMonthRaw() {
    return reverseLowestBits(bits(this.getRawBitString(), 45, 0b11111L), 5);
  }

  /**
   * Get decoded year within century.
   *
   * @return the decoded year within century 00-99.
   */
  public int getYearWithinCentury() {
    return fromBCD((int) this.getYearWithinCenturyRaw());
  }

  /**
   * Get raw LSB0 year within century value
   *
   * @return the LSB0 raw year within century value
   */
  public long getYearWithinCenturyRaw() {
    return reverseLowestBits(bits(this.getRawBitString(), 50, 0b1111_1111L), 8);
  }

  /**
   * Calculate even bit for date.
   *
   * @return true if even and false otherwise
   */
  public boolean isDateEvenParity() {
    return bits(this.getRawBitString(), 58, 1L) != 0L;
  }

  /**
   * Check minute mark flag.
   *
   * @return true or false, must be false for valid record.
   */
  public boolean isMinuteMark() {
    return bits(this.getRawBitString(), 59, 1L) != 0L;
  }

  @Override
  public ZonedDateTime extractSourceTime() {
    return ZonedDateTime.of(
        this.getYearWithinCentury() + currentCentury(),
        this.getMonth(),
        this.getDayOfMonth(),
        this.getHour(),
        this.getMinute(),
        0,
        0,
        ZONE_CET);
  }

  /**
   * Check that the record has valid state of its bit fields. It checks even and synchro flags.
   *
   * @return true if the record has valid data, false otherwise.
   */
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

    if ((calcEvenParityOverBits(bitString, 21, 28) ? 1L : 0L) != bits(bitString, 28, 1L)) {
      return false;
    }
    if ((calcEvenParityOverBits(bitString, 29, 35) ? 1L : 0L) != bits(bitString, 35, 1L)) {
      return false;
    }
    return (calcEvenParityOverBits(bitString, 36, 58) ? 1L : 0L) == bits(bitString, 58, 1L);
  }

}
