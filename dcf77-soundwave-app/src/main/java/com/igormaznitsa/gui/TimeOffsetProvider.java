package com.igormaznitsa.gui;

import java.time.ZonedDateTime;

public interface TimeOffsetProvider {

  ZonedDateTime apply(ZonedDateTime dateTime);

  default String getOffsetTimeText() {
    return "";
  }

}
