package com.igormaznitsa.soundtime.jjy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class JjyRecordTest {

  @Test
  void testWithCallSignAwareness() {
    final JjyRecord regular = JjyRecord.makeWithAnnounceCallSignAwareness(ZonedDateTime.of(
        2025,
        4,
        17,
        11,
        14,
        32,
        0,
        JjyRecord.ZONE_JST
    ));
    final JjyRecord callSign = JjyRecord.makeWithAnnounceCallSignAwareness(ZonedDateTime.of(
        2025,
        4,
        17,
        11,
        15,
        32,
        0,
        JjyRecord.ZONE_JST
    ));
    assertTrue(regular.isValid());
    assertTrue(callSign.isValid());

    assertFalse(regular.isCallSignAnnouncement());
    assertTrue(callSign.isCallSignAnnouncement());
  }

  @Test
  void testConstructorForBitString() {
    final JjyRecord record = new JjyRecord(
        0b0_00100101_0_000100111_0_000100110_0_001000010_0_000010110_0_101000000_0L, true);

    assertTrue(record.isValid());

    assertEquals(15, record.getMinute());
    assertEquals(17, record.getHour());
    assertEquals(162, record.getDayOfYear());
    assertEquals(16, record.getYearInCentury());
    assertEquals(5, record.getDayOfWeek());
    assertFalse(record.isLeapSecondAtCurrentUtcMonthEnd());
    assertFalse(record.isLeapSecondAdded());
  }

  @Test
  void testConstructorForSeparatedParameters() {
    final JjyRecord record = new JjyRecord(
        15,
        17,
        162,
        16,
        5,
        false,
        false);

    assertEquals(
        0b0_00100101_0_000100111_0_000100110_0_001000010_0_000010110_0_101000000_0L,
        record.getBitString(true),
        record.toBinaryString(true));

    assertTrue(record.isValid());

    assertEquals(15, record.getMinute());
    assertEquals(17, record.getHour());
    assertEquals(162, record.getDayOfYear());
    assertEquals(16, record.getYearInCentury());
    assertEquals(5, record.getDayOfWeek());
    assertFalse(record.isLeapSecondAtCurrentUtcMonthEnd());
    assertFalse(record.isLeapSecondAdded());
  }

}