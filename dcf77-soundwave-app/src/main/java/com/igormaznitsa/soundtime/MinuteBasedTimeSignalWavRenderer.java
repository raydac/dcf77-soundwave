package com.igormaznitsa.soundtime;

import java.time.ZonedDateTime;
import java.util.List;

public interface MinuteBasedTimeSignalWavRenderer {

  byte[] makeMinuteWavData(
      boolean secondsAwareness,
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
