package com.igormaznitsa.soundtime.wwvb;

import static com.igormaznitsa.soundtime.wwvb.WwvbRecord.DST_BEGINS_TODAY;
import static com.igormaznitsa.soundtime.wwvb.WwvbRecord.DST_ENDS_TODAY;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Month;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class WwvbRecordTest {
  @Test
  void testConstructorForBitString() {
    final WwvbRecord record = new WwvbRecord(
        0b0_01100000_0_000000111_0_000000110_0_011000010_0_001100000_0_100001000_0L, true);

    assertTrue(record.isValid());

    assertEquals(30, record.getMinutes());
    assertEquals(7, record.getHours());
    assertEquals(66, record.getDayOfYear());
    assertEquals(8, record.getYearInCentury());
    assertEquals(0, record.getSecond());
    assertEquals(0b010, record.getDut1Sign());
    assertEquals(0.3f, record.getDut1(), 0.05f);
    assertTrue(record.isLeapYear());
    assertFalse(record.isLeapSecondAtEndOfMonth());
    assertEquals(0, record.getDst());
  }

  @Test
  void testConstructorFromZonedDateTime() {
    final WwvbRecord record = new WwvbRecord(ZonedDateTime.of(
        2008,
        Month.MARCH.getValue(),
        6,
        7,
        30,
        32,
        0,
        UTC
    ));
    assertTrue(record.isValid());

    assertEquals(30, record.getMinutes());
    assertEquals(7, record.getHours());
    assertEquals(32, record.getSecond());
    assertEquals(66, record.getDayOfYear());
    assertEquals(8, record.getYearInCentury());
    assertEquals(0, record.getDut1Sign());
    assertEquals(0.0f, record.getDut1(), 0.01f);
    assertTrue(record.isLeapYear());
    assertFalse(record.isLeapSecondAtEndOfMonth());
    assertEquals(0, record.getDst());

    assertEquals(0b0_01100000_0_000000111_0_000000110_0_011000000_0_000000000_0_100001000_0L,
        record.getBitString(true));
  }

  @Test
  void testConstructorFromZonedDateTime_NoLeapYear_SummerTime() {
    final WwvbRecord record = new WwvbRecord(ZonedDateTime.of(
        2025,
        Month.APRIL.getValue(),
        18,
        7,
        30,
        44,
        0,
        UTC
    ));
    assertTrue(record.isValid());

    assertEquals(30, record.getMinutes());
    assertEquals(7, record.getHours());
    assertEquals(108, record.getDayOfYear());
    assertEquals(25, record.getYearInCentury());
    assertEquals(0, record.getDut1Sign());
    assertEquals(44, record.getSecond());
    assertEquals(0.0f, record.getDut1(), 0.01f);
    assertFalse(record.isLeapYear());
    assertFalse(record.isLeapSecondAtEndOfMonth());
    assertEquals(WwvbRecord.DST_IN_EFFECT, record.getDst());
  }

  @Test
  void testConstructorFromZonedDateTime_NoLeapYear_DST_starts() {
    final WwvbRecord record = new WwvbRecord(ZonedDateTime.of(
        2025,
        Month.MARCH.getValue(),
        9,
        0,
        10,
        11,
        0,
        UTC
    ));
    assertTrue(record.isValid());

    assertEquals(10, record.getMinutes());
    assertEquals(0, record.getHours());
    assertEquals(68, record.getDayOfYear());
    assertEquals(25, record.getYearInCentury());
    assertEquals(0, record.getDut1Sign());
    assertEquals(11, record.getSecond());
    assertEquals(0.0f, record.getDut1(), 0.01f);
    assertFalse(record.isLeapYear());
    assertFalse(record.isLeapSecondAtEndOfMonth());
    assertEquals(DST_BEGINS_TODAY, record.getDst());
  }

  @Test
  void testConstructorFromZonedDateTime_NoLeapYear_DST_ends() {
    final WwvbRecord record = new WwvbRecord(ZonedDateTime.of(
        2025,
        Month.NOVEMBER.getValue(),
        2,
        0,
        10,
        32,
        0,
        UTC
    ));
    assertTrue(record.isValid());

    assertEquals(10, record.getMinutes());
    assertEquals(0, record.getHours());
    assertEquals(306, record.getDayOfYear());
    assertEquals(25, record.getYearInCentury());
    assertEquals(0, record.getDut1Sign());
    assertEquals(32, record.getSecond());
    assertEquals(0.0f, record.getDut1(), 0.01f);
    assertFalse(record.isLeapYear());
    assertFalse(record.isLeapSecondAtEndOfMonth());
    assertEquals(DST_ENDS_TODAY, record.getDst());
  }

}