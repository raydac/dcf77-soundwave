package com.igormaznitsa.soundtime.dcf77;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class Dcf77RecordTest {

  @Test
  void testDcf77FromDateTime() {
    final ZonedDateTime time = ZonedDateTime.now(Dcf77Record.ZONE_CET);
    final Dcf77Record dcf77Record = new Dcf77Record(time);
    assertTrue(dcf77Record.isValid());
    assertEquals(time.getMinute(), dcf77Record.getMinute());
    assertEquals(time.getHour(), dcf77Record.getHour());
    assertEquals(time.getDayOfWeek().getValue(), dcf77Record.getDayOfWeek());
    assertEquals(time.getMonth().getValue(), dcf77Record.getMonth());
    assertEquals(time.getDayOfMonth(), dcf77Record.getDayOfMonth());
  }

  @Test
  void testDcf77RecordDecodedFromRadio() {
    final long radioData =
        0b000000000000000001001100101011110100100000101000011001100100L;
    final Dcf77Record decodedFromRadio = new Dcf77Record(radioData, true);

    assertTrue(decodedFromRadio.isValid());
    assertTrue(decodedFromRadio.isCest());
    assertFalse(decodedFromRadio.isCet());
    assertFalse(decodedFromRadio.isCallBit());
    assertFalse(decodedFromRadio.isSummerTimeAnnouncement());
    assertFalse(decodedFromRadio.isLeapSecondAnnouncement());
    assertEquals(17, decodedFromRadio.getHour());
    assertEquals(29, decodedFromRadio.getMinute());
    assertEquals(1, decodedFromRadio.getDayOfMonth());
    assertEquals(10, decodedFromRadio.getMonth());
    assertEquals(99, decodedFromRadio.getYearWithinCentury());

    final ZonedDateTime zonedDateTime =
        ZonedDateTime.of(LocalDate.of(1999, 10, 1), LocalTime.of(17, 29, 0, 0),
            Dcf77Record.ZONE_CET);
    final Dcf77Record cratedFromDateTime = new Dcf77Record(zonedDateTime);

    assertTrue(cratedFromDateTime.isValid());
    assertTrue(cratedFromDateTime.isCest());
    assertFalse(cratedFromDateTime.isCet());
    assertFalse(cratedFromDateTime.isCallBit());
    assertFalse(cratedFromDateTime.isSummerTimeAnnouncement());
    assertFalse(cratedFromDateTime.isLeapSecondAnnouncement());
    assertEquals(17, cratedFromDateTime.getHour());
    assertEquals(29, cratedFromDateTime.getMinute());
    assertEquals(1, cratedFromDateTime.getDayOfMonth());
    assertEquals(10, cratedFromDateTime.getMonth());
    assertEquals(99, cratedFromDateTime.getYearWithinCentury());

    assertEquals(radioData, cratedFromDateTime.getBitString(true));
  }

  @Test
  void testParseProvidedAsBits() {
    // 2020-11-12 09:58:00
    Dcf77Record record =
        new Dcf77Record(0b001111100001011000101000110111001000010010001100010000010000L, true);
    assertTrue(record.isValid());
    assertEquals(12, record.getDayOfMonth());
    assertEquals(11, record.getMonth());
    assertEquals(20, record.getYearWithinCentury());
    assertEquals(9, record.getHour());
    assertEquals(58, record.getMinute());
    assertFalse(record.isCest());
    assertTrue(record.isCet());

    // So, 26.10.08 01:55:00, SZ
    record = new Dcf77Record(0b000010110001000001001101010101000001011001111000010001000000L, true);
    assertTrue(record.isValid());
    assertEquals(26, record.getDayOfMonth());
    assertEquals(10, record.getMonth());
    assertEquals(8, record.getYearWithinCentury());
    assertEquals(1, record.getHour());
    assertEquals(55, record.getMinute());
    assertTrue(record.isCest());
    assertFalse(record.isCet());
  }

  @Test
  void testDcf77Record() {
    final long etalonMsb0 =
        0b0_00010100101001_00010_1_1100100_1_100000_1_010010_001_10001_00000100_0_0L;

    final Dcf77Record record = new Dcf77Record(
        0b0_00010100101001_00010_1_1100100_1_100000_1_010010_001_10001_00000100_0_0L,
        true
    );

    assertEquals("000010100101001000101110010011000001010010001100010000010000",
        record.toBinaryString(true));
    assertTrue(record.isValid());
    assertEquals(0b100000L, record.getYearWithinCenturyRaw());
    assertEquals(0b10001L, record.getMonthRaw());
    assertEquals(0b100L, record.getDayOfWeekRaw());
    assertEquals(0b010010L, record.getDayOfMonthRaw());
    assertEquals(0b000001L, record.getHourRaw());
    assertEquals(0b0010011L, record.getMinuteRaw());
    assertEquals(0b10010100101000L, record.getCivilWarningBits());
    assertEquals(etalonMsb0, record.getBitString(true));

    final Dcf77Record constructed = new Dcf77Record(true,
        0b00010100101001,
        false,
        false,
        false,
        true,
        false,
        0b1100100,
        0b100000,
        0b010010,
        0b001,
        0b10001,
        0b00000100
    );

    assertEquals(etalonMsb0, constructed.getBitString(true));
  }

}