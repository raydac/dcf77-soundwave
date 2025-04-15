package com.igormaznitsa.soundtime.dcf77;

import com.igormaznitsa.soundtime.MinuteBasedTimeSignalBits;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Implementation of DCF77 record.
 *
 * @author Igor Maznitsa
 * @see <a href=”https://www.ptb.de/cms/fileadmin/internet/fachabteilungen/abteilung_4/4.4_zeit_und_frequenz/pdf/2004_Piester_-_PTB-Mitteilungen_114.pdf”>DCF77 specification</a>
 */
public final class Dcf77Record implements MinuteBasedTimeSignalBits {
  /**
   * Standard time zone for DCF77 signal.
   */
  public static final ZoneId ZONE_CET = ZoneId.of("CET");

  /**
   * Contains bit data of the record.
   */
  private final long bitString;
  private final int hashCode;

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
        toBCD(ensureCet(time).getMinute()),
        toBCD(ensureCet(time).getHour()),
        toBCD(ensureCet(time).getDayOfMonth()),
        toBCD(ensureCet(time).getDayOfWeek().getValue()),
        toBCD(ensureCet(time).getMonthValue()),
        toBCD(ensureCet(time).getYear() % 100)
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
    long data = setValue(0L, civilWarningBits, 1, 0b11111111111111L, msb0);
    data = setValue(data, callBit ? 1L : 0L, 15, 1L, msb0);
    data = setValue(data, summerTimeAnnouncement ? 1L : 0L, 16, 1L, msb0);
    data = setValue(data, cest ? 1L : 0L, 17, 1L, msb0);
    data = setValue(data, cet ? 1L : 0L, 18, 1L, msb0);
    data = setValue(data, leapSecondAnnouncement ? 1L : 0L, 19, 1L, msb0);
    data = setValue(data, 1L, 20, 1L, msb0);
    data = setValue(data, bcdMinutes, 21, 0b111_111_1L, msb0);
    data = setValue(data, bcdHours, 29, 0b111_111L, msb0);
    data = setValue(data, bcdDayOfMonth, 36, 0b111_111L, msb0);
    data = setValue(data, bcdDayOfWeek, 42, 0b111L, msb0);
    data = setValue(data, bcdMonthNumber, 45, 0b11111L, msb0);
    data = setValue(data, bcdYearWithinCentury, 50, 0b1111_1111L, msb0);

    data = setValue(data, calcEvenParity(data, 21, 28) ? 1L : 0L, 28, 1L, msb0);
    data = setValue(data, calcEvenParity(data, 29, 35) ? 1L : 0L, 35, 1L, msb0);
    data = setValue(data, calcEvenParity(data, 36, 58) ? 1L : 0L, 58, 1L, msb0);

    this.bitString = data;
    this.hashCode = Objects.hashCode(data);
  }

  /**
   * Create from DCF77 bit vector
   *
   * @param dcf77bits bit vector
   * @param msb0      if true then vector in MSB0, LSB0 otherwise
   */
  public Dcf77Record(final long dcf77bits, final boolean msb0) {
    if (msb0) {
      this.bitString = reverseLowestBits(dcf77bits, 60);
    } else {
      this.bitString = dcf77bits;
    }
    this.hashCode = Objects.hashCode(this.bitString);
  }

  public static ZonedDateTime ensureCet(final ZonedDateTime time) {
    if (time.getZone().equals(ZONE_CET)) {
      return time;
    }
    return time.withZoneSameInstant(ZONE_CET);
  }

  /**
   * Encode integer value as BCD.
   *
   * @param value must be in 0..255 diapason.
   * @return encoded BCD value
   */
  public static int toBCD(int value) {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException("Value must be between 0 and 255");
    }
    int bcd = 0;
    int shift = 0;

    while (value > 0) {
      int digit = value % 10;
      bcd |= (digit << (shift * 4));
      value /= 10;
      shift++;
    }
    return bcd;
  }

  /**
   * Decode BCD value as integer.
   *
   * @param bcdValue BCD value.
   * @return decoded value 0..255
   */
  public static int fromBCD(int bcdValue) {
    int value = 0;
    int factor = 1;

    while (bcdValue > 0) {
      int digit = bcdValue & 0xF;
      if (digit > 9) {
        throw new NumberFormatException("Wrong BCD format: " + digit);
      }
      value += digit * factor;
      factor *= 10;
      bcdValue >>= 4;
    }
    return value;
  }

  /**
   * Get reversed bit value.
   *
   * @param value              source value
   * @param numberOfLowestBits number of lowest bits to reverse
   * @return result with reversed lowest bits
   */
  public static long reverseLowestBits(final long value, final int numberOfLowestBits) {
    long acc = 0L;
    long src = value;
    for (int i = 0; i < numberOfLowestBits; i++) {
      acc = (acc << 1) | (src & 1L);
      src >>>= 1L;
    }
    return acc;
  }

  private static long setValue(
      final long data,
      final long value,
      final int shift,
      final long mask,
      final boolean msb0
  ) {
    long x;
    if (mask != 1L && msb0) {
      int maskBitsCount = 0;
      long tempMask = mask;
      while (tempMask != 0L) {
        maskBitsCount++;
        tempMask >>>= 1;
      }
      x = reverseLowestBits(value, maskBitsCount);
    } else {
      x = value;
    }

    final long shiftedMaskedValue = (x & mask) << shift;
    return (data & ~(mask << shift)) | shiftedMaskedValue;
  }

  private static long bits(
      final long data,
      final int shift,
      final long mask
  ) {
    long result = (data >>> shift) & mask;
    if (mask > 1L) {
      long reversed = 0L;
      long restMask = mask;

      while (restMask != 0L) {
        reversed = (reversed << 1L) | (result & 1L);
        result >>>= 1;
        restMask >>>= 1;
      }
      return reversed;
    } else {
      return result;
    }
  }

  /**
   * DCF77 record as bit string, 60 bits.
   *
   * @param msb0 true if required MSB0 result, false if LSB0
   * @return 60 char string contains bit string
   */
  public String toBinaryString(
      final boolean msb0
  ) {
    final long value = this.bitString;
    final String bitString;
    if (msb0) {
      bitString = Long.toBinaryString(reverseLowestBits(value, 60));
    } else {
      bitString = Long.toBinaryString(value);
    }
    final StringBuilder buffer = new StringBuilder();
    int diff = 60 - bitString.length();
    while (diff > 0) {
      buffer.append('0');
      diff--;
    }
    return buffer.append(bitString).toString();
  }

  @Override
  public boolean equals(final Object that) {
    if (that == null || getClass() != that.getClass()) {
      return false;
    }
    Dcf77Record record = (Dcf77Record) that;
    return this.bitString == record.bitString;
  }

  @Override
  public int hashCode() {
    return this.hashCode;
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
      return reverseLowestBits(this.bitString, 60);
    } else {
      return this.bitString;
    }
  }

  /**
   * Get civil warning bits from bit vector in LSB0 format.
   *
   * @return civil warning bits as LSB0
   */
  public long getCivilWarningBits() {
    return reverseLowestBits(bits(this.bitString, 1, 0b11_1111_1111_1111L), 14);
  }

  /**
   * Return call bit
   *
   * @return true or false
   */
  public boolean isCallBit() {
    return bits(this.bitString, 15, 1L) != 0L;
  }

  /**
   * Return summer time announcement.
   *
   * @return summer time announcement
   */
  public boolean isSummerTimeAnnouncement() {
    return bits(this.bitString, 16, 1L) != 0L;
  }

  /**
   * Return CEST flag.
   *
   * @return true or false
   */
  public boolean isCest() {
    return bits(this.bitString, 17, 1L) != 0L;
  }

  /**
   * Return CET flag.
   *
   * @return true or false
   */
  public boolean isCet() {
    return bits(this.bitString, 18, 1L) != 0L;
  }

  /**
   * Return leap second announcement flag.
   *
   * @return true or false
   */
  public boolean isLeapSecondAnnouncement() {
    return bits(this.bitString, 19, 1L) != 0L;
  }

  /**
   * Return minute start bit flag.
   *
   * @return true if valid packet
   */
  public boolean isMinuteStartBit() {
    return bits(this.bitString, 20, 1L) != 0L;
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
    return (int) reverseLowestBits(bits(this.bitString, 21, 0b1111111L), 7);
  }

  public boolean isMinuteEvenParity() {
    return bits(this.bitString, 28, 0b1L) != 0L;
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
    return reverseLowestBits(bits(this.bitString, 29, 0b111111L), 6);
  }

  public boolean isHourEvenParity() {
    return bits(this.bitString, 35, 0b1L) != 0L;
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
    return reverseLowestBits(bits(this.bitString, 36, 0b111111L), 6);
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
    return reverseLowestBits(bits(this.bitString, 42, 0b111L), 3);
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
    return reverseLowestBits(bits(this.bitString, 45, 0b11111L), 5);
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
    return reverseLowestBits(bits(this.bitString, 50, 0b1111_1111L), 8);
  }

  /**
   * Calculate even bit for date.
   *
   * @return true if even and false otherwise
   */
  public boolean isDateEvenParity() {
    return bits(this.bitString, 58, 1L) != 0L;
  }

  /**
   * Check minute mark flag.
   *
   * @return true or false, must be false for valid record.
   */
  public boolean isMinuteMark() {
    return bits(this.bitString, 59, 1L) != 0L;
  }

  /**
   * Decode as zoned date time for CET zone. Year will be since 2000.
   *
   * @return decoded zoned date time in CET zone
   */
  public ZonedDateTime getDateTime() {
    return ZonedDateTime.of(
        this.getYearWithinCentury() + 2000,
        this.getMonth(),
        this.getDayOfMonth(),
        this.getHour(),
        this.getMinute(),
        0,
        0,
        ZONE_CET);
  }

  private boolean calcEvenParity(final long data, final int from, final int to) {
    boolean result = false;
    for (int i = from; i < to; i++) {
      if (bits(data, i, 1L) != 0L) {
        result = !result;
      }
    }
    return result;
  }

  /**
   * Check that the record has valid state of its bit fields. It checks even and synchro flags.
   *
   * @return true if the record has valid data, false otherwise.
   */
  public boolean isValid() {
    if (
        bits(this.bitString, 0, 1L) != 0L
            || bits(this.bitString, 59, 1L) != 0L
            || bits(this.bitString, 20, 1L) == 0L
    ) {
      return false;
    }

    if ((this.calcEvenParity(this.bitString, 21, 28) ? 1L : 0L) != bits(this.bitString, 28, 1L)) {
      return false;
    }
    if ((this.calcEvenParity(this.bitString, 29, 35) ? 1L : 0L) != bits(this.bitString, 35, 1L)) {
      return false;
    }
    return (this.calcEvenParity(this.bitString, 36, 58) ? 1L : 0L) == bits(this.bitString, 58, 1L);
  }


}
