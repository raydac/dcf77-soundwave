package com.igormaznitsa.gui;

import com.igormaznitsa.soundtime.DstDetection;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface TimeDateIndicationProvider {
  ZonedDateTime getZonedTimeDateNow(DstDetection dstDetection);

  ZoneId getStandardSignalZoneId();

  String getProtocolId();
}
