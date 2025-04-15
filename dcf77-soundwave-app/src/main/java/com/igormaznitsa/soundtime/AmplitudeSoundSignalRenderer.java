package com.igormaznitsa.soundtime;

import static java.lang.Math.PI;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Renderer amplitude modulated signal through Java Sound API or write it as a WAV file.
 *
 * @author Igor Maznitsa
 */
public class AmplitudeSoundSignalRenderer {

  private static final int SAMPLE_BYTES = 2;
  private final AudioFormat audioFormat;
  private final AtomicBoolean disposed = new AtomicBoolean();
  private final int sampleRate;
  private final AtomicReference<Thread> thread = new AtomicReference<>();
  private final BlockingQueue<RenderedRecord> renderQueue;
  private final SourceDataLineSupplier sourceDataLineSupplier;
  private final AtomicReference<SourceDataLine> sourceDataLine = new AtomicReference<>();
  private final List<AmplitudeSoundSignalRendererListener> listeners = new CopyOnWriteArrayList<>();
  private final MinuteBasedTimeSignalWavRenderer minuteBasedTimeSignalWavRenderer;

  /**
   * Constructor.
   *
   * @param queueCapacity          capacity of internal queue for records
   * @param sampleRate             sample rate for audio signal, usually 44100 or 48000.
   * @param sourceDataLineSupplier supplier of SourceDataLine for AudioFormat
   */
  public AmplitudeSoundSignalRenderer(
      final MinuteBasedTimeSignalWavRenderer minuteBasedTimeSignalWavRenderer,
      final int queueCapacity,
      final int sampleRate,
      final SourceDataLineSupplier sourceDataLineSupplier) {
    this.minuteBasedTimeSignalWavRenderer = minuteBasedTimeSignalWavRenderer;
    this.renderQueue = new ArrayBlockingQueue<>(queueCapacity);
    this.sampleRate = sampleRate;
    this.audioFormat = new AudioFormat(sampleRate, 8 * SAMPLE_BYTES, 1, true, false);
    this.sourceDataLineSupplier = sourceDataLineSupplier;
  }

  public void addAmplitudeSoundSignalRendererListener(
      final AmplitudeSoundSignalRendererListener listener) {
    this.listeners.add(Objects.requireNonNull(listener));
  }

  public void removeAmplitudeSoundSignalRendererListener(
      final AmplitudeSoundSignalRendererListener listener) {
    this.listeners.remove(listener);
  }

  /**
   * Check that internal queue is empty.
   *
   * @return true if empty, false otherwise
   */
  public boolean isEmpty() {
    this.assertNotDisposed();
    return this.renderQueue.isEmpty();
  }

  /**
   * Get current internal queue size.
   *
   * @return size of internal queue
   */
  public int getQueueSize() {
    this.assertNotDisposed();
    return this.renderQueue.size();
  }

  /**
   * Reset internal queue.
   */
  public void resetQueue() {
    this.assertNotDisposed();
    this.renderQueue.clear();
  }

  /**
   * Render WAV file from record list.
   *
   * @param secondsAwareness   if true then number of seconds will be calculated and packet won't be started from first one.
   * @param recordList         list of records
   * @param freqHz             carrier wave frequency in Hz.
   * @param amplitudeDeviation AM amplitude deviation.
   * @param signalShape        signal shape to form audio signal.
   * @return formed WAV byte array.
   * @throws IOException if any problem during io operations.
   */
  public byte[] renderWav(
      final boolean secondsAwareness,
      final List<MinuteBasedTimeSignalBits> recordList,
      final int freqHz,
      final double amplitudeDeviation,
      final SignalShape signalShape) throws IOException {

    boolean seconds = secondsAwareness;

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    for (final MinuteBasedTimeSignalBits record : recordList) {
      buffer.writeBytes(
          this.minuteBasedTimeSignalWavRenderer.makeMinuteWavData(
              seconds,
              record,
              freqHz,
              this.sampleRate,
              SAMPLE_BYTES,
              signalShape,
              amplitudeDeviation
          ));
      seconds = false;
    }
    buffer.close();
    final byte[] wavData = buffer.toByteArray();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(wavData);
    final AudioInputStream audioInputStream =
        new AudioInputStream(inputStream, this.audioFormat, wavData.length / SAMPLE_BYTES);
    final ByteArrayOutputStream resultFile = new ByteArrayOutputStream();
    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, resultFile);
    resultFile.close();
    return resultFile.toByteArray();
  }

  public boolean offer(
      final boolean secondsAwareness,
      final MinuteBasedTimeSignalBits dataProvider,
      final int freqHz,
      final double amplitudeDeviation,
      final SignalShape signalShape) {
    if (this.disposed.get()) {
      return false;
    }

    final byte[] rendered = minuteBasedTimeSignalWavRenderer.makeMinuteWavData(
        secondsAwareness,
        requireNonNull(dataProvider),
        freqHz,
        this.sampleRate,
        SAMPLE_BYTES,
        signalShape,
        amplitudeDeviation);
    // 11071 , 15500, 12700
    return this.renderQueue.offer(
        new RenderedRecord(dataProvider, rendered));
  }

  private void runnable() {
    while (!Thread.currentThread().isInterrupted()) {
      final RenderedRecord nextRenderedRecord;
      try {
        nextRenderedRecord = this.renderQueue.take();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        break;
      }

      this.listeners.forEach(x ->
          x.onNextRecord(this, nextRenderedRecord.bitStringProvider));

      final SourceDataLine line = this.sourceDataLine.get();
      if (line == null) {
        return;
      }
      if (line.isOpen()) {
        int length = nextRenderedRecord.soundData.length;
        int offset = 0;
        while (length > 0 && !Thread.currentThread().isInterrupted() && !this.disposed.get()) {
          try {
            final int written =
                line.write(nextRenderedRecord.soundData, offset, length);
            length -= written;
            offset += written;
          } catch (IllegalArgumentException ex) {
            // looks like interrupted
          }
        }
      }
    }
  }

  private void assertNotDisposed() {
    if (this.disposed.get()) {
      throw new IllegalStateException("Disposed");
    }
  }

  public boolean isDisposed() {
    return this.disposed.get();
  }

  public void initAudioLine() {
    this.assertNotDisposed();
    if (this.thread.get() == null) {
      try {
        this.sourceDataLine.set(this.sourceDataLineSupplier.apply(this.audioFormat));
      } catch (Exception ex) {
        if (ex instanceof RuntimeException) {
          throw (RuntimeException) ex;
        }
        throw new RuntimeException(ex);
      }
      if (this.sourceDataLine.get() == null) {
        throw new IllegalStateException("Can't find source data line for " + this.audioFormat);
      }
      try {
        this.sourceDataLine.get().open(this.audioFormat, this.sampleRate);
      } catch (LineUnavailableException ex) {
        throw new IllegalStateException("Line unavailable", ex);
      }

      final Thread thread =
          new Thread(null, this::runnable, "minute-sound-renderer-" + System.identityHashCode(this),
              16384);
      thread.setPriority(Thread.NORM_PRIORITY);
      thread.setDaemon(true);
      if (this.thread.compareAndSet(null, thread)) {
        thread.start();
      } else {
        throw new IllegalStateException("Detected already existed thread");
      }
    } else {
      throw new IllegalStateException("Detected already existed thread");
    }
  }

  public void startAudio() {
    this.assertNotDisposed();
    this.renderQueue.clear();
    final SourceDataLine line = this.sourceDataLine.get();
    if (line != null) {
      synchronized (line) {
        line.start();
        line.drain();
      }
    }
  }

  public void stopAudio() {
    this.assertNotDisposed();
    this.renderQueue.clear();
    final SourceDataLine line = this.sourceDataLine.get();
    if (line != null) {
      synchronized (line) {
        try {
          line.stop();
        } catch (Exception ignored) {
        }
        try {
          line.flush();
        } catch (Exception ignored) {
        }
        try {
          line.close();
        } catch (Exception ignored) {
        }
      }
    }
  }

  public void dispose() {
    if (this.disposed.compareAndSet(false, true)) {
      this.renderQueue.clear();
      final SourceDataLine line = this.sourceDataLine.getAndSet(null);
      if (line != null) {
        final Thread thread = this.thread.getAndSet(null);
        if (thread != null) {
          thread.interrupt();
        }
        synchronized (line) {
          try {
            try {
              line.stop();
            } catch (Exception ignored) {
            }
            try {
              line.flush();
            } catch (Exception ignored) {
            }

            try {
              line.close();
            } catch (Exception ignored) {
            }
          } finally {
            line.close();
          }
        }
        if (thread != null) {
          try {
            thread.join(3000L);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }

  public enum SignalShape {
    SIN((i, f, a, r) -> {
      final double time = (double) i / r;
      return Math.round((Math.sin(PI * f * time) * Short.MAX_VALUE) * a);
    }),
    SQUARE((i, f, a, r) -> {
      final double time = (double) i / r;
      final double sineValue = Math.sin(PI * f * time);
      return Math.round((sineValue >= 0.0d ? Short.MAX_VALUE : Short.MIN_VALUE) * a);
    }),
    TRIANGLE((i, f, a, r) -> {
      final double time = (double) i / r;
      final double sine = Math.sin(Math.PI * f * time);
      return Math.round((2 / Math.PI) * Math.asin(sine) * Short.MAX_VALUE * a);
    });

    private final VolumeCalculator calculator;

    SignalShape(VolumeCalculator calculator) {
      this.calculator = calculator;
    }

    public long calculate(int sampleIndex, double freq, double amplitude, final int sampleRate) {
      return this.calculator.apply(sampleIndex, freq, amplitude, sampleRate);
    }

  }

  @FunctionalInterface
  public interface AmplitudeSoundSignalRendererListener {
    void onNextRecord(AmplitudeSoundSignalRenderer source,
                      MinuteBasedTimeSignalBits minuteBitStringProvider);
  }

  @FunctionalInterface
  private interface VolumeCalculator {
    long apply(int sampleIndex, double freq, double amplitude, int sampeRate);
  }

  @FunctionalInterface
  public interface SourceDataLineSupplier {
    SourceDataLine apply(AudioFormat audioFormat) throws Exception;
  }

  private static final class RenderedRecord {
    private final MinuteBasedTimeSignalBits bitStringProvider;
    private final byte[] soundData;

    private RenderedRecord(final MinuteBasedTimeSignalBits bitStringProvider,
                           final byte[] soundData
    ) {
      this.bitStringProvider = bitStringProvider;
      this.soundData = soundData;
    }
  }
}
