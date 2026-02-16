package com.igormaznitsa.soundtime;

import com.igormaznitsa.gui.TimeDateIndicationProvider;
import java.time.ZonedDateTime;
import java.util.List;

public interface MinuteBasedTimeSignalWavRenderer extends TimeDateIndicationProvider {

  byte[] makeMinuteWavData(
      MinuteBasedTimeSignalBits minuteBitStringProvider,
      double freq,
      int sampleRate,
      int sampleBytes,
      AmplitudeSoundSignalRenderer.SignalShape signalShape,
      double amplitudeDeviation);

  double getAmplitudeDeviation();

  MinuteBasedTimeSignalBits makeTimeSignalBits(ZonedDateTime zonedDateTime);

  List<Integer> getAllowedCarrierFrequences();

}
