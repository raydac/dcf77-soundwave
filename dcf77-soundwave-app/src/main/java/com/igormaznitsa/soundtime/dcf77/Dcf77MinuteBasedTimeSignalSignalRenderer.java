package com.igormaznitsa.soundtime.dcf77;

import com.igormaznitsa.soundtime.AmplitudeSoundSignalRenderer;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalBits;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalWavRenderer;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

public class Dcf77MinuteBasedTimeSignalSignalRenderer implements MinuteBasedTimeSignalWavRenderer {

  public static final Dcf77MinuteBasedTimeSignalSignalRenderer
      INSTANCE = new Dcf77MinuteBasedTimeSignalSignalRenderer();
  /**
   * Standard amplitude deviation for DCF77 amplitude modulation.
   */
  public static final double DCF77_STANDARD_AMPLITUDE_DEVIATION = 0.85d;
  private static final List<Integer> ALLOWED_CARRIER_FREQ = List.of(13700, 15500, 17125);

  @Override
  public List<Integer> getAllowedCarrierFrequences() {
    return ALLOWED_CARRIER_FREQ;
  }

  @Override
  public MinuteBasedTimeSignalBits makeTimeSignalBits(final ZonedDateTime zonedDateTime) {
    return new Dcf77Record(zonedDateTime);
  }

  @Override
  public double getAmplitudeDeviation() {
    return DCF77_STANDARD_AMPLITUDE_DEVIATION;
  }

  @Override
  public byte[] makeMinuteWavData(
      final boolean secondsAwareness,
      final MinuteBasedTimeSignalBits minuteBitStringProvider,
      final double freq,
      final int sampleRate,
      final int sampleBytes,
      final AmplitudeSoundSignalRenderer.SignalShape signalShape,
      final double amplitudeDeviation) {
    final int samplesPerSet = sampleRate / 5;
    final int samplesPerReset = sampleRate / 10;

    long data = minuteBitStringProvider.getBitString(false);

    final int second =
        secondsAwareness ? LocalTime.now().getSecond() : 0;
    final int totalSamples = sampleRate * (60 - second);
    final byte[] wavBuffer = new byte[totalSamples * sampleBytes];

    int sampleIndex = 0;
    for (int i = second; i < 60; i++) {
      final boolean bitState = ((data >>> i) & 1L) != 0L;
      final int syncPrefixSamples;
      if (i == 59) {
        syncPrefixSamples = -1;
      } else {
        syncPrefixSamples = bitState ? samplesPerSet : samplesPerReset;
      }

      int sampleCounter = 0;
      while (sampleCounter < sampleRate) {
        final double amplitude;
        if (sampleCounter <= syncPrefixSamples) {
          amplitude = 1.0d - amplitudeDeviation;
        } else {
          amplitude = 1.0d;
        }
        final long volume = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate);
        wavBuffer[sampleIndex++] = (byte) (volume & 0xFF);
        wavBuffer[sampleIndex++] = (byte) ((volume >>> 8) & 0xFF);

        sampleCounter++;
      }
    }
    return wavBuffer;
  }


}
