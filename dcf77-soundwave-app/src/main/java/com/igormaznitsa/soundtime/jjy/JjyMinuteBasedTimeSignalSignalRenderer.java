package com.igormaznitsa.soundtime.jjy;

import com.igormaznitsa.soundtime.AmplitudeSoundSignalRenderer;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalBits;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalWavRenderer;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

public class JjyMinuteBasedTimeSignalSignalRenderer implements MinuteBasedTimeSignalWavRenderer {

  public static final JjyMinuteBasedTimeSignalSignalRenderer
      INSTANCE = new JjyMinuteBasedTimeSignalSignalRenderer();
  /**
   * Standard amplitude deviation for DCF77 amplitude modulation.
   */
  public static final double JJY_STANDARD_AMPLITUDE_DEVIATION = 0.90d;
  private static final List<Integer> ALLOWED_CARRIER_FREQ = List.of(8000, 13333, 20000);

  @Override
  public List<Integer> getAllowedCarrierFrequences() {
    return ALLOWED_CARRIER_FREQ;
  }

  @Override
  public MinuteBasedTimeSignalBits makeTimeSignalBits(final ZonedDateTime zonedDateTime) {
    return JjyRecord.makeWithAnnounceCallSignAwareness(zonedDateTime);
  }

  @Override
  public double getAmplitudeDeviation() {
    return JJY_STANDARD_AMPLITUDE_DEVIATION;
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
    final int samplesPerMarker = sampleRate / 5;
    final int samplesPerSet = sampleRate / 2;
    final int samplesPerReset = (sampleRate << 2) / 5;

    long data = minuteBitStringProvider.getBitString(false);

    final int second =
        secondsAwareness ? LocalTime.now().getSecond() : 0;
    final int totalSamples = sampleRate * (60 - second);
    final byte[] wavBuffer = new byte[totalSamples * sampleBytes];

    int sampleIndex = 0;
    for (int i = second; i < 60; i++) {
      final boolean bitState = ((data >>> i) & 1L) != 0L;
      final int syncPrefixSamples;
      if (i == 0 || i % 10 == 9) {
        // marker
        syncPrefixSamples = samplesPerMarker;
      } else {
        syncPrefixSamples = bitState ? samplesPerSet : samplesPerReset;
      }

      int sampleCounter = 0;
      while (sampleCounter < sampleRate) {
        final double amplitude;
        if (sampleCounter <= syncPrefixSamples) {
          amplitude = 1.0d;
        } else {
          amplitude = 1.0d - amplitudeDeviation;
        }
        final long volume = signalShape.calculate(sampleIndex, freq, amplitude, sampleRate);
        wavBuffer[sampleIndex++] = (byte) (volume & 0xFF);
        wavBuffer[sampleIndex++] = (byte) ((volume >>> 8) & 0xFF);

        sampleCounter++;
      }
    }
    return wavBuffer;
  }

  @Override
  public ZonedDateTime getZonedTimeDateNow() {
    return ZonedDateTime.now(JjyRecord.ZONE_JST);
  }

  @Override
  public String getIndicationText() {
    return "JST";
  }
}
