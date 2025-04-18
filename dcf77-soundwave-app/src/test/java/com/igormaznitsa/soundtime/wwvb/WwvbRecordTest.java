package com.igormaznitsa.soundtime.wwvb;

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
        0,
        0,
        UTC
    ));
    assertTrue(record.isValid());

    assertEquals(30, record.getMinutes());
    assertEquals(7, record.getHours());
    assertEquals(66, record.getDayOfYear());
    assertEquals(8, record.getYearInCentury());
    assertEquals(0, record.getDut1Sign());
    assertEquals(0.0f, record.getDut1(), 0.01f);
    assertTrue(record.isLeapYear());
    assertFalse(record.isLeapSecondAtEndOfMonth());
    assertEquals(0, record.getDst());
  }

  @Test
  void testConstructorFromZonedDateTime_NoLeapYear_SummerTime() {
    final WwvbRecord record = new WwvbRecord(ZonedDateTime.of(
        2025,
        Month.APRIL.getValue(),
        18,
        7,
        30,
        0,
        0,
        UTC
    ));
    assertTrue(record.isValid());

    assertEquals(30, record.getMinutes());
    assertEquals(7, record.getHours());
    assertEquals(108, record.getDayOfYear());
    assertEquals(25, record.getYearInCentury());
    assertEquals(0, record.getDut1Sign());
    assertEquals(0.0f, record.getDut1(), 0.01f);
    assertFalse(record.isLeapYear());
    assertFalse(record.isLeapSecondAtEndOfMonth());
    assertEquals(0b11, record.getDst());
  }

}