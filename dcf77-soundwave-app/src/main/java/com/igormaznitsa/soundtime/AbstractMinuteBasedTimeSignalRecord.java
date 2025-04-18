package com.igormaznitsa.soundtime;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

public abstract class AbstractMinuteBasedTimeSignalRecord implements MinuteBasedTimeSignalBits {
  /**
   * Contains bit data of the record.
   */
  private final long bitString;
  private final int hashCode;

  public AbstractMinuteBasedTimeSignalRecord(final long bitString) {
    this.bitString = bitString;
    this.hashCode = Objects.hashCode(this.bitString);
  }

  protected static int currentCentury() {
    return (LocalDate.now().getYear() / 100) * 100;
  }

  protected static long bits(
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
   * Make BCD value with padded 5 bit
   *
   * @param value value to be encoded
   * @return encoded value
   */
  public static int toBcdPadded5(int value) {
    return (((value / 100) % 10) << 10) | (((value / 10) % 10) << 5) | (value % 10);
  }

  /**
   * Decode BCD5 value
   *
   * @param bcd5value value to be decoded
   * @return decoded value
   */
  public static int fromBcdPadded5(int bcd5value) {
    final int a = bcd5value & 0xF;
    final int b = (bcd5value >> 5) & 0xF;
    final int c = (bcd5value >> 10) & 0xF;
    return c * 100 + b * 10 + a;
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

  protected static boolean calcEvenParityOverBits(final long data, final int from, final int to) {
    boolean result = false;
    for (int i = from; i < to; i++) {
      if (bits(data, i, 1L) != 0L) {
        result = !result;
      }
    }
    return result;
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

  public static ZonedDateTime ensureTimezone(final ZonedDateTime time, final ZoneId zoneId) {
    if (time.getZone().equals(zoneId)) {
      return time;
    }
    return time.withZoneSameInstant(zoneId);
  }

  protected static long setBits(
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

  protected long getRawBitString() {
    return this.bitString;
  }

  public abstract boolean isValid();

  /**
   * Record as bit string, 60 bits.
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
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractMinuteBasedTimeSignalRecord that = (AbstractMinuteBasedTimeSignalRecord) o;
    return bitString == that.bitString;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bitString, hashCode);
  }

  @Override
  public long getBitString(boolean msb0) {
    return 0;
  }
}
