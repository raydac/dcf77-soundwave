package com.igormaznitsa.gui;

import java.time.ZonedDateTime;

public interface TimeDateIndicationProvider {
  ZonedDateTime getZonedTimeDateNow();

  String getIndicationText();
}
