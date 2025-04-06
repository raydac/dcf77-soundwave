import com.igormaznitsa.dcf77soundwave.Dcf77Record;
import com.igormaznitsa.dcf77soundwave.JavaSoundDcf77SignalRenderer;
import java.io.IOException;
import java.time.ZonedDateTime;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AMSoundGenerator {
  private static final int SAMPLE_RATE = 44100;
  private static final int FREQ = 38750;
  private static final int BIT_DURATION = SAMPLE_RATE / 10; // 100ms per bit
  private static final int FOURIER_TERMS = 10; // Number of terms in square wave approximation

  public static byte[] generateAMSound(long bitSource) {
    int totalSamples = 64 * BIT_DURATION;
    byte[] audioData = new byte[totalSamples];

    for (int bitIndex = 0; bitIndex < 64; bitIndex++) {
      boolean bit = ((bitSource >> bitIndex) & 1) == 1;
      double amplitude = bit ? 1.0 : 0.2; // High for 1, low for 0

      for (int i = 0; i < BIT_DURATION; i++) {
        int sampleIndex = bitIndex * BIT_DURATION + i;
        double t = (double) i / SAMPLE_RATE * 2 * Math.PI * FREQ;
        double value = 0;

        // Compute square wave approximation using Fourier series
        for (int n = 0; n < FOURIER_TERMS; n++) {
          int k = 2 * n + 1;
          value += Math.sin(k * t) / k;
        }
        value *= 4 / Math.PI; // Normalize to approximate a square wave

        audioData[sampleIndex] = (byte) (amplitude * value * 127);
      }
    }
    return audioData;
  }

  public static void writeSound(SourceDataLine line, byte[] soundData) {
    line.write(soundData, 0, soundData.length);
  }

  public static void main(String[] args) throws LineUnavailableException, IOException {
    JavaSoundDcf77SignalRenderer sounder =
        new JavaSoundDcf77SignalRenderer(60, 48000, AudioSystem::getSourceDataLine);
    sounder.initAudioLine();
    sounder.startAudio();

    ZonedDateTime now = ZonedDateTime.now();

//    List<Dcf77Record> recordList = new ArrayList<>();
//    for (int i = 0; i < 3; i++) {
//      recordList.add(new Dcf77Record(now));
//      now = now.plusMinutes(1);
//    }
//
//    byte[] wav = sounder.makeWavFileData(true, recordList, 15500, DCF77_STANDARD_AMPLITUDE_DEVIATION,
//        JavaSoundDcf77SignalRenderer.SignalShape.SIN);
//    Files.write(new File("some.wav").toPath(), wav);
//
//    if (true) {
//      return;
//    }

    boolean secondsAwareness = true;
    while (true) {
      Dcf77Record soundData = new Dcf77Record(now);
      if (!soundData.isValid()) {
        throw new Error("ERROR");
      }
      System.out.println("TIME: " + now + " REC " + soundData + "  " +
          Dcf77Record.toBinaryString(soundData, true));
      now = now.plusMinutes(1L);
      boolean queued = sounder.offer(secondsAwareness, soundData, 15500,
          JavaSoundDcf77SignalRenderer.DCF77_STANDARD_AMPLITUDE_DEVIATION,
          JavaSoundDcf77SignalRenderer.SignalShape.TRIANGLE);
      secondsAwareness = false;
      if (queued) {
        System.out.println("Queued");
      } else {
        break;
      }
    }

    while (!Thread.currentThread().isInterrupted() && sounder.getQueueSize() != 0) {
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        break;
      }
    }

  }
}