package com.igormaznitsa.soundtime;

import java.time.ZonedDateTime;

public interface MinuteBasedTimeSignalBits {
  long getBitString(boolean msb0);

  ZonedDateTime extractSourceTime();
}
