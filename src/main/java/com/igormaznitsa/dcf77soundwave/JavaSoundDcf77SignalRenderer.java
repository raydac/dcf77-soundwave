package com.igormaznitsa.dcf77soundwave;

import static java.lang.Math.PI;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class JavaSoundDcf77SignalRenderer {

  public static final double DCF77_STANDARD_AMPLITUDE_DEVIATION = 0.85d;
  private static final int SAMPLE_BYTES = 2;
  private final int samplesPerReset;
  private final int samplesPerSet;
  private final AudioFormat audioFormat;
  private final AtomicBoolean disposed = new AtomicBoolean();
  private final int sampleRate;
  private final AtomicReference<Thread> thread = new AtomicReference<>();
  private final BlockingQueue<RenderedRecord> renderQueue;
  private final SourceDataLineSupplier sourceDataLineSupplier;
  private final AtomicReference<SourceDataLine> sourceDataLine = new AtomicReference<>();

  public JavaSoundDcf77SignalRenderer(
      final int queueCapacity,
      final int sampleRate,
      final SourceDataLineSupplier sourceDataLineSupplier) {
    this.renderQueue = new ArrayBlockingQueue<>(queueCapacity);
    this.sampleRate = sampleRate;
    this.samplesPerSet = this.sampleRate / 5;
    this.samplesPerReset = this.sampleRate / 10;
    this.audioFormat = new AudioFormat(sampleRate, 8 * SAMPLE_BYTES, 1, true, false);
    this.sourceDataLineSupplier = sourceDataLineSupplier;
  }

  public boolean isEmpty() {
    this.assertNotDisposed();
    return this.renderQueue.isEmpty();
  }

  public int getQueueSize() {
    this.assertNotDisposed();
    return this.renderQueue.size();
  }

  public void resetQueue() {
    this.assertNotDisposed();
    this.renderQueue.clear();
  }

  public byte[] makeWavFileData(
      final boolean secondsAwareness,
      final List<Dcf77Record> recordList,
      final int freqHz,
      final double amplitudeDeviation,
      final SignalShape signalShape) throws IOException {

    boolean seconds = secondsAwareness;

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    for (final Dcf77Record record : recordList) {
      buffer.writeBytes(
          this.makeWavDataAlignedForMinute(seconds, record, freqHz, signalShape,
              amplitudeDeviation));
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
      final Dcf77Record record,
      final int freqHz,
      final double amplitudeDeviation,
      final SignalShape signalShape) {
    this.assertNotDisposed();

    final byte[] rendered = makeWavDataAlignedForMinute(
        secondsAwareness,
        requireNonNull(record),
        freqHz,
        signalShape,
        amplitudeDeviation);
    // 11071 , 15500, 12700
    return this.renderQueue.offer(
        new RenderedRecord(record, rendered, amplitudeDeviation, signalShape));
  }

  private byte[] makeWavDataAlignedForMinute(
      final boolean secondsAwareness,
      final Dcf77Record record,
      final double freq,
      final SignalShape signalShape,
      final double amplitudeDeviation) {
    long data = record.getBitString(false);

    final LocalTime now = LocalTime.now();
    final int second = secondsAwareness ? now.getSecond() : 0;
    final int totalSamples = this.sampleRate * (60 - second);
    final byte[] wavBuffer = new byte[totalSamples * SAMPLE_BYTES];

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
      while (sampleCounter < this.sampleRate) {
        final double amplitude;
        if (sampleCounter <= syncPrefixSamples) {
          amplitude = 1.0d - amplitudeDeviation;
        } else {
          amplitude = 1.0d;
        }
        final long volume = signalShape.calculate(sampleIndex, freq, amplitude, this.sampleRate);
        wavBuffer[sampleIndex++] = (byte) (volume & 0xFF);
        wavBuffer[sampleIndex++] = (byte) ((volume >>> 8) & 0xFF);

        sampleCounter++;
      }
    }
    return wavBuffer;
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
      final SourceDataLine line = this.sourceDataLine.get();
      if (line == null) {
        return;
      }
      if (line.isOpen()) {
        int length = nextRenderedRecord.soundData.length;
        int offset = 0;
        while (length > 0 && !Thread.currentThread().isInterrupted()) {
          final int written =
              line.write(nextRenderedRecord.soundData, offset, length);
          length -= written;
          offset += written;
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
        this.sourceDataLine.get().open(this.audioFormat, SAMPLE_BYTES * this.sampleRate * 2);
      } catch (LineUnavailableException ex) {
        throw new IllegalStateException("Line unavailable", ex);
      }

      final Thread thread =
          new Thread(null, this::runnable, "dcf77-sounder-" + System.identityHashCode(this), 16384);
      thread.setPriority(Thread.MAX_PRIORITY);
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
      line.start();
    }
  }

  public void stopAudio() {
    this.assertNotDisposed();
    this.renderQueue.clear();
    final SourceDataLine line = this.sourceDataLine.get();
    if (line != null) {
      line.stop();
    }
  }

  public void dispose() {
    if (this.disposed.compareAndExchange(false, true)) {
      final SourceDataLine line = this.sourceDataLine.getAndSet(null);
      if (line != null) {
        final Thread thread = this.thread.getAndSet(null);
        if (thread != null) {
          thread.interrupt();
        }
        try {
          line.stop();
        } finally {
          line.close();
        }
        if (thread != null) {
          try {
            thread.join(5000L);
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
  private interface VolumeCalculator {
    long apply(int sampleIndex, double freq, double amplitude, int sampeRate);
  }

  @FunctionalInterface
  public interface SourceDataLineSupplier {
    SourceDataLine apply(AudioFormat audioFormat) throws Exception;
  }

  private static final class RenderedRecord {
    private final Dcf77Record dcf77Record;
    private final byte[] soundData;
    private final double amplitudeDeviation;
    private final SignalShape signalShape;

    private RenderedRecord(final Dcf77Record record,
                           final byte[] soundData,
                           final double amplitudeDeviation,
                           final SignalShape signalShape) {
      this.dcf77Record = record;
      this.soundData = soundData;
      this.amplitudeDeviation = amplitudeDeviation;
      this.signalShape = signalShape;
    }
  }
}
