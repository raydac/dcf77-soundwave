package com.igormaznitsa.gui;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface TimeDateIndicationProvider {
  ZonedDateTime getZonedTimeDateNow();

  ZoneId getProtocolZoneId();

  String getProtocolId();
}
