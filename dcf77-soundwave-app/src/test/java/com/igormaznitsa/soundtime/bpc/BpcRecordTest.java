package com.igormaznitsa.soundtime.bpc;

import static com.igormaznitsa.soundtime.bpc.BpcMinuteBasedTimeSignalSignalRenderer.ZONE_CHN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Month;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class BpcRecordTest {

  @Test
  void testConstructorFromZonedDateTime() {
    BpcRecord record = new BpcRecord(
        ZonedDateTime.of(
            2004,
            Month.MARCH.getValue(),
            9,
            9,
            15,
            0,
            0,
            ZONE_CHN
        )
    );
    assertTrue(record.isValid());
    assertEquals(
        "000010010011110010010010010011000100010001001001001111001000001001001100010001001000100100111100100000100100110001000100",
        record.toBinaryString(false));

    record = new BpcRecord(
        ZonedDateTime.of(
            2010,
            Month.JULY.getValue(),
            2,
            14,
            38,
            0,
            0,
            ZONE_CHN
        )
    );
    assertTrue(record.isValid());
    assertEquals(
        "000000101001100101100000100111001010000001000010100110010111000010011100101000001000001010011001011100001001110010100000",
        record.toBinaryString(false));
  }

  @Test
  void testConstructorFromBitString() {
    BpcRecord record = new BpcRecord(
        "000010010011110010010010010011000100010001001001001111001000001001001100010001001000100100111100100000100100110001000100"
    );
    assertTrue(record.isValid());

    assertEquals(9, record.getHours());
    assertEquals(15, record.getMinutes());
    assertEquals(4, record.getYearInCentury());
    assertEquals(2, record.getDayOfWeek());
    assertFalse(record.isPM());
    assertEquals(9, record.getDayOfMonth());
    assertEquals(3, record.getMonth());

    assertEquals(
        "000010010011110010010010010011000100010001001001001111001000001001001100010001001000100100111100100000100100110001000100",
        record.toBinaryString(false));

    record = new BpcRecord(
        "00_00001010011001011000001001110010100000_01_00001010011001011100001001110010100000_10_00001010011001011100001001110010100000");
    assertTrue(record.isValid());

    assertEquals(2, record.getHours());
    assertEquals(38, record.getMinutes());
    assertEquals(10, record.getYearInCentury());
    assertEquals(5, record.getDayOfWeek());
    assertTrue(record.isPM());
    assertEquals(2, record.getDayOfMonth());
    assertEquals(7, record.getMonth());

    assertEquals(
        "000000101001100101100000100111001010000001000010100110010111000010011100101000001000001010011001011100001001110010100000",
        record.toBinaryString(false));
  }

}