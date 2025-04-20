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
  void test() {
    BpcRecord record = new BpcRecord(
        "0000001101110001100101111000010100000000_0100001101110001100001111000010100000000_1000001101110001100001111000010100000000");
    assertTrue(record.isValid());
    assertEquals(3, record.getHours());
    assertEquals(28, record.getMinutes());
    assertEquals(6, record.getDayOfWeek());
    assertFalse(record.isPM());
    assertEquals(30, record.getDayOfMonth());
    assertEquals(1, record.getMonth());
    assertEquals(16, record.getYearInCentury());

    BpcRecord newRecord = new BpcRecord(record.extractSourceTime());
    assertEquals(
        "000000110111000110010111100001010000000001000011011100011000011110000101000000001000001101110001100001111000010100000000",
        newRecord.toBinaryString(false));

    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(0));
    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(1));
    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(2));
    assertEquals(0b11, newRecord.getBcpBitString().getBitPair(3));
    assertEquals(0b01, newRecord.getBcpBitString().getBitPair(4));
    assertEquals(0b11, newRecord.getBcpBitString().getBitPair(5));
    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(6));
    assertEquals(0b01, newRecord.getBcpBitString().getBitPair(7));
    assertEquals(0b10, newRecord.getBcpBitString().getBitPair(8));
    assertEquals(0b01, newRecord.getBcpBitString().getBitPair(9));
    assertEquals(0b01, newRecord.getBcpBitString().getBitPair(10));
    assertEquals(0b11, newRecord.getBcpBitString().getBitPair(11));
    assertEquals(0b10, newRecord.getBcpBitString().getBitPair(12));
    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(13));
    assertEquals(0b01, newRecord.getBcpBitString().getBitPair(14));
    assertEquals(0b01, newRecord.getBcpBitString().getBitPair(15));
    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(16));
    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(17));
    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(18));
    assertEquals(0b00, newRecord.getBcpBitString().getBitPair(19));
    assertEquals(0b01, newRecord.getBcpBitString().getBitPair(20));
  }

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