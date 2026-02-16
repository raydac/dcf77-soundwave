package com.igormaznitsa.soundtime;

import static com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord.fromBCD;
import static com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord.fromBcdPadded5;
import static com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord.toBCD;
import static com.igormaznitsa.soundtime.AbstractMinuteBasedTimeSignalRecord.toBcdPadded5;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AbstractMinuteBasedTimeSignalRecordTest {

  @Test
  void testBcd() {
    assertEquals(97, fromBCD(0b10010111));
    assertEquals(0, toBCD(0));
    assertEquals(0b101_1001, toBCD(59));
    assertEquals(0b10010111, toBCD(97));
    assertEquals(20, fromBCD(0b00100000));
    assertEquals(0b00100000, toBCD(20));
    assertEquals(0b0001_0011, toBCD(13));
    assertEquals(0b0001_0110, toBCD(16));
    assertEquals(13, fromBCD(0b0001_0011));
    assertEquals(24, fromBCD(toBCD(24)));
    assertEquals(59, fromBCD(0b101_1001));
  }

  @Test
  void testBcd5() {
    assertEquals(15, fromBcdPadded5(0b00100101));
    assertEquals(0b00100101, toBcdPadded5(15));
    assertEquals(17, fromBcdPadded5(0b000100111));
    assertEquals(0b000100111, toBcdPadded5(17));
    assertEquals(16, fromBcdPadded5(0b0001_00110));
    assertEquals(0b0001_00110, toBcdPadded5(16));
    assertEquals(162, fromBcdPadded5(0b01_00110_00010));
    assertEquals(0b01_00110_00010, toBcdPadded5(162));
  }

}