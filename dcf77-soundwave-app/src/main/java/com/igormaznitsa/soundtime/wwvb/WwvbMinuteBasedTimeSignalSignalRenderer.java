package com.igormaznitsa.soundtime.wwvb;

import static java.time.ZoneOffset.UTC;

import com.igormaznitsa.soundtime.AmplitudeSoundSignalRenderer;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalBits;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalWavRenderer;
import java.time.ZonedDateTime;
import java.util.List;

public class WwvbMinuteBasedTimeSignalSignalRenderer implements MinuteBasedTimeSignalWavRenderer {

  public static final WwvbMinuteBasedTimeSignalSignalRenderer
      INSTANCE = new WwvbMinuteBasedTimeSignalSignalRenderer();

  public static final double WWVB_STANDARD_AMPLITUDE_DEVIATION = 0.9d;
  private static final List<Integer> ALLOWED_CARRIER_FREQ = List.of(8571, 12000, 15000);

  @Override
  public List<Integer> getAllowedCarrierFrequences() {
    return ALLOWED_CARRIER_FREQ;
  }

  @Override
  public MinuteBasedTimeSignalBits makeTimeSignalBits(final ZonedDateTime zonedDateTime) {
    return new WwvbRecord(zonedDateTime);
  }

  @Override
  public double getAmplitudeDeviation() {
    return WWVB_STANDARD_AMPLITUDE_DEVIATION;
  }

  @Override
  public byte[] makeMinuteWavData(
      final MinuteBasedTimeSignalBits minuteBitStringProvider,
      final double freq,
      final int sampleRate,
      final int sampleBytes,
      final AmplitudeSoundSignalRenderer.SignalShape signalShape,
      final double amplitudeDeviation) {
    final int samplesPerMarker = (sampleRate << 2) / 5;
    final int samplesPerSetBit = sampleRate / 2;
    final int samplesPerResetBit = sampleRate / 5;

    long data = minuteBitStringProvider.getBitString(false);

    final int second = minuteBitStringProvider.getSecond();
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
        syncPrefixSamples = bitState ? samplesPerSetBit : samplesPerResetBit;
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

  @Override
  public ZonedDateTime getZonedTimeDateNow() {
    return ZonedDateTime.now(UTC);
  }

  @Override
  public String getIndicationText() {
    return "UTC";
  }
}
