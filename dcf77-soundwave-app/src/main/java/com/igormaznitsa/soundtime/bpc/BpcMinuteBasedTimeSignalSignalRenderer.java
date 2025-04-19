package com.igormaznitsa.soundtime.bpc;

import com.igormaznitsa.soundtime.AmplitudeSoundSignalRenderer;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalBits;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalWavRenderer;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class BpcMinuteBasedTimeSignalSignalRenderer implements MinuteBasedTimeSignalWavRenderer {

  public static final ZoneId ZONE_CHN = ZoneId.of("Asia/Shanghai");

  public static final BpcMinuteBasedTimeSignalSignalRenderer
      INSTANCE = new BpcMinuteBasedTimeSignalSignalRenderer();

  public static final double BPC_STANDARD_AMPLITUDE_DEVIATION = 0.9d;
  private static final List<Integer> ALLOWED_CARRIER_FREQ = List.of(9785, 13700, 24400);

  @Override
  public List<Integer> getAllowedCarrierFrequences() {
    return ALLOWED_CARRIER_FREQ;
  }

  @Override
  public MinuteBasedTimeSignalBits makeTimeSignalBits(final ZonedDateTime zonedDateTime) {
    return new BpcRecord(zonedDateTime);
  }

  @Override
  public double getAmplitudeDeviation() {
    return BPC_STANDARD_AMPLITUDE_DEVIATION;
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

    final int samplesMarker = -1;
    final int samples00 = sampleRate / 10;
    final int samples01 = sampleRate / 5;
    final int samples10 = sampleRate * 3 / 10;
    final int samples11 = sampleRate * 2 / 5;

    final BcpBitString bcpBitString = ((BpcRecord) minuteBitStringProvider).getBcpBitString();

    final int second =
        secondsAwareness ? LocalTime.now().getSecond() : 0;
    final int totalSamples = sampleRate * (60 - second);
    final byte[] wavBuffer = new byte[totalSamples * sampleBytes];

    int sampleIndex = 0;
    for (int i = second; i < 60; i++) {
      final int bitPair = bcpBitString.getBitPair(i);

      final int syncPrefixSamples;
      if (i % 20 == 0) {
        // marker
        syncPrefixSamples = samplesMarker;
      } else {
        switch (bitPair) {
          case 0:
            syncPrefixSamples = samples00;
            break;
          case 1:
            syncPrefixSamples = samples01;
            break;
          case 2:
            syncPrefixSamples = samples10;
            break;
          case 3:
            syncPrefixSamples = samples11;
            break;
          default:
            throw new Error("Unexpected:" + bitPair);
        }
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
    return ZonedDateTime.now(ZONE_CHN);
  }

  @Override
  public String getIndicationText() {
    return "CHN";
  }
}
