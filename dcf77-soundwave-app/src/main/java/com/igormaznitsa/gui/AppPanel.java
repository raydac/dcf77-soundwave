package com.igormaznitsa.gui;

import static java.util.Objects.requireNonNull;

import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import com.igormaznitsa.dcf77soundwave.Dcf77Record;
import com.igormaznitsa.dcf77soundwave.Dcf77SignalSoundRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class AppPanel extends JPanel {

  private final TimePanel timePanel;
  private final StartStopButton buttonStartStop;
  private static final Color TIME_PANEL_NORMAL = Color.BLUE.darker().darker().darker();
  private final Component progressBarReplacement;
  private final ControlButton buttonSine;
  private final ControlButton buttonSquare;
  private final ControlButton buttonTriangle;
  private final ControlButton button44100;
  private final ControlButton button48000;
  private final ControlButton button96000;
  private final ControlButton button13700;
  private final ControlButton button15500;
  private final ControlButton button17125;
  private static final Color TIME_PANEL_FIXED = Color.RED.darker().darker().darker().darker();

  private final AtomicReference<Dcf77SignalSoundRenderer> currentRenderer = new AtomicReference<>();
  private final Supplier<AppFrame.OutputLineInfo> mixerSupplier;
  private final SignalProgressBar progressBarTime;
  private final ControlButton buttonCustomTime;
  private Supplier<ZonedDateTime> baseTimeSupplier;
  private Supplier<ZonedDateTime> currentTimeSupplier;

  public AppPanel(
      final Supplier<AppFrame.OutputLineInfo> mixerSupplier,
      final Supplier<ZonedDateTime> timeSupplier
  ) {
    super(new BorderLayout(0, 0));
    this.baseTimeSupplier = requireNonNull(timeSupplier);
    this.currentTimeSupplier = this.baseTimeSupplier;
    this.mixerSupplier = requireNonNull(mixerSupplier);
    this.timePanel = new TimePanel(() -> this.currentTimeSupplier.get());
    this.timePanel.setBackground(TIME_PANEL_NORMAL);
    this.progressBarTime = new SignalProgressBar();

    this.add(this.timePanel, BorderLayout.CENTER);

    this.buttonStartStop = new StartStopButton();
    this.buttonStartStop.setToolTipText("Start/Stop render sound signal");
    final JPanel bottomPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    bottomPanel.add(this.buttonStartStop, gbc);
    gbc.gridx = 1;
    gbc.weightx = 100000;
    bottomPanel.add(this.progressBarTime, gbc);
    this.progressBarTime.setVisible(false);
    gbc.weightx = 10000;
    this.progressBarReplacement = Box.createHorizontalGlue();
    bottomPanel.add(this.progressBarReplacement, gbc);

    this.add(bottomPanel, BorderLayout.SOUTH);

    final JPanel controlPanel = new JPanel(new GridLayout(4, 3));

    final Font defaultButtonFont = UIManager.getFont("Button.font");

    this.buttonSine = new ControlButton("w");
    this.buttonSine.setToolTipText("Sine wave");
    this.buttonSine.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));
    this.buttonSquare = new ControlButton("Q");
    this.buttonSquare.setToolTipText("Square wave");
    this.buttonSquare.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));
    this.buttonTriangle = new ControlButton("T");
    this.buttonTriangle.setToolTipText("Triangular wave");
    this.buttonTriangle.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));

    this.buttonCustomTime = new ControlButton("[");
    this.buttonCustomTime.setToolTipText("Render custom time");
    this.buttonCustomTime.setFont(
        GuiUtils.FONT_SOSA.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));

    this.buttonCustomTime.addActionListener(e -> {
      if (this.buttonCustomTime.isSelected()) {
        final ZonedDateTime selected = this.getSelectedTime();
        if (selected == null) {
          this.buttonCustomTime.setSelected(false);
        } else {
          this.currentTimeSupplier = () -> selected;
          this.timePanel.setShowSecondsChange(false);
          this.timePanel.setBackground(TIME_PANEL_FIXED);
        }
      } else {
        this.currentTimeSupplier = this.baseTimeSupplier;
        this.timePanel.setShowSecondsChange(true);
        this.timePanel.setBackground(TIME_PANEL_NORMAL);
      }
      this.timePanel.refreshTime();
    });

    final ButtonGroup buttonGroupSignal = new ButtonGroup();
    buttonGroupSignal.add(this.buttonSine);
    buttonGroupSignal.add(this.buttonSquare);
    buttonGroupSignal.add(this.buttonTriangle);

    this.button13700 = new ControlButton("13700 Hz");
    this.button13700.setToolTipText("Carrier frequency 13700 Hz");

    this.button15500 = new ControlButton("15500 Hz");
    this.button15500.setToolTipText("Carrier frequency 15500 Hz");

    this.button17125 = new ControlButton("17125 Hz");
    this.button17125.setToolTipText("Carrier frequency 17125 Hz");

    final ButtonGroup buttonGroupCarrier = new ButtonGroup();
    buttonGroupCarrier.add(this.button13700);
    buttonGroupCarrier.add(this.button15500);
    buttonGroupCarrier.add(this.button17125);

    this.button44100 = new ControlButton("44.1 KHz");
    this.button44100.setToolTipText("Sound-card sampling 44100 Hz");

    this.button48000 = new ControlButton("48 KHz");
    this.button48000.setToolTipText("Sound-card sampling 48000 Hz");

    this.button96000 = new ControlButton("96 KHz");
    this.button96000.setToolTipText("Sound-card sampling 96000 Hz");

    final ButtonGroup buttonGroupSampleRate = new ButtonGroup();
    buttonGroupSampleRate.add(this.button44100);
    buttonGroupSampleRate.add(this.button48000);
    buttonGroupSampleRate.add(this.button96000);

    controlPanel.add(this.buttonSine);
    controlPanel.add(this.buttonSquare);
    controlPanel.add(this.buttonTriangle);

    controlPanel.add(this.button13700);
    controlPanel.add(this.button15500);
    controlPanel.add(this.button17125);

    controlPanel.add(this.button44100);
    controlPanel.add(this.button48000);
    controlPanel.add(this.button96000);
    controlPanel.add(Box.createHorizontalGlue());
    controlPanel.add(this.buttonCustomTime);
    controlPanel.add(Box.createHorizontalGlue());

    this.add(controlPanel, BorderLayout.EAST);

    this.buttonSine.setSelected(true);
    this.button44100.setSelected(true);
    this.button15500.setSelected(true);


    this.buttonStartStop.addActionListener(e -> {
      if (this.buttonStartStop.isSelected()) {
        final AppFrame.OutputLineInfo lineInfo = this.mixerSupplier.get();
        if (lineInfo == null) {
          JOptionPane.showMessageDialog(this, "No selected output channel!", "No output",
              JOptionPane.WARNING_MESSAGE);
          this.buttonStartStop.setSelected(false);
        }
        this.startRendering(lineInfo);
      } else {
        this.stopRendering(null);
      }
    });
  }

  public ZonedDateTime getCurrentTime() {
    return this.currentTimeSupplier.get();
  }

  public void dispose() {
    this.buttonStartStop.setSelected(false);
    this.stopRendering(null);
  }

  private void sendTimeData(
      final Dcf77SignalSoundRenderer renderer,
      final int numberOfRenderedMinutes,
      final int freqHz,
      final Dcf77SignalSoundRenderer.SignalShape shape
  ) {
    final Thread thread = new Thread(() -> {
      ZonedDateTime zonedDateTime =
          this.currentTimeSupplier.get().withZoneSameInstant(Dcf77Record.ZONE_CET);
      boolean addedSuccessfully = true;
      for (int i = 0;
           i < numberOfRenderedMinutes && !renderer.isDisposed() &&
               !Thread.currentThread().isInterrupted(); i++) {
        addedSuccessfully &= renderer.offer(i == 0, new Dcf77Record(zonedDateTime), freqHz,
            Dcf77SignalSoundRenderer.DCF77_STANDARD_AMPLITUDE_DEVIATION, shape);
        zonedDateTime = zonedDateTime.plusMinutes(1);
      }
      final Dcf77Record stopRecord = new Dcf77Record(zonedDateTime);
      addedSuccessfully &= renderer.offer(false, stopRecord, freqHz,
          Dcf77SignalSoundRenderer.DCF77_STANDARD_AMPLITUDE_DEVIATION, shape);

      if (!addedSuccessfully) {
        final boolean disposeAsCause = renderer.isDisposed();
        this.stopRendering(
            () -> JOptionPane.showMessageDialog(this, "Internal error! Queue too small!", "Error",
                JOptionPane.ERROR_MESSAGE));
        if (!disposeAsCause) {
          SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Error during queue filling");
          });
        }
        return;
      }
      renderer.addDcf77SignalSoundRendererListener(
          (source, record) -> {
            if (source == renderer
                && !source.isDisposed()
                && stopRecord == record) {
              this.stopRendering(
                  () -> JOptionPane.showMessageDialog(this, "Rendering successfully completed",
                      "Info",
                      JOptionPane.INFORMATION_MESSAGE));
            }
          });
    }, "fill-time");
    thread.setDaemon(true);
    thread.start();
  }

  private ZonedDateTime getSelectedTime() {
    final DatePickerSettings datePickerSettings = new DatePickerSettings(Locale.US);
    datePickerSettings.setVisibleTodayButton(true);
    datePickerSettings.setVisibleNextMonthButton(true);
    datePickerSettings.setVisiblePreviousMonthButton(true);
    datePickerSettings.setAllowEmptyDates(false);
    datePickerSettings.setEnableMonthMenu(true);
    datePickerSettings.setEnableYearMenu(true);

    final TimePickerSettings timePickerSettings = new TimePickerSettings(Locale.US);
    timePickerSettings.setAllowEmptyTimes(false);
    timePickerSettings.setDisplaySpinnerButtons(true);
    timePickerSettings.setDisplayToggleTimeMenuButton(true);
    timePickerSettings.setInitialTimeToNow();

    DateTimePicker dateTimePicker = new DateTimePicker(datePickerSettings, timePickerSettings);

    if (JOptionPane.showConfirmDialog(this, dateTimePicker, "Select date time",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
      final LocalDate localDate = dateTimePicker.datePicker.getDate();
      final LocalTime localTime = dateTimePicker.timePicker.getTime();
      return ZonedDateTime.of(
          localDate.getYear(),
          localDate.getMonthValue(),
          localDate.getDayOfMonth(),
          localTime.getHour(),
          localTime.getMinute(),
          localTime.getSecond(),
          0, Dcf77Record.ZONE_CET);
    }
    return null;
  }

  private void startRendering(final AppFrame.OutputLineInfo output) {
    if (this.currentRenderer.get() == null) {
      this.progressBarTime.setVisible(true);
      this.progressBarReplacement.setVisible(false);
      this.setEnableButtons(false);

      final int sampleRate = this.getSampleRate();

      final Dcf77SignalSoundRenderer renderer = new Dcf77SignalSoundRenderer(
          140,
          sampleRate,
          () -> this.currentTimeSupplier.get().toInstant(),
          audioFormat -> output.line);

      if (this.currentRenderer.compareAndSet(null, renderer)) {
        try {
          renderer.initAudioLine();
          renderer.startAudio();

          final Dcf77SignalSoundRenderer.SignalShape shape = this.getSignalShape();
          final int freq = this.getCarrierFreq();

          this.sendTimeData(renderer, 60, freq, shape);
        } catch (Exception ex) {
          renderer.dispose();
          this.currentRenderer.set(null);
          this.buttonStartStop.setSelected(false);
          this.setEnableButtons(true);
        }
      }
    }
  }

  private void stopRendering(final Runnable nextSwingAction) {
    final Dcf77SignalSoundRenderer renderer = this.currentRenderer.getAndSet(null);
    if (renderer != null) {
      final Thread stopThread = new Thread(renderer::dispose, "stopping");
      stopThread.setDaemon(true);
      stopThread.start();

      final Runnable swingAction = () -> {
        this.progressBarTime.setVisible(false);
        this.progressBarReplacement.setVisible(true);

        this.setEnableButtons(true);
        this.buttonStartStop.setSelected(false);

        if (nextSwingAction != null) {
          nextSwingAction.run();
        }
      };

      if (SwingUtilities.isEventDispatchThread()) {
        swingAction.run();
      } else {
        SwingUtilities.invokeLater(swingAction);
      }
    }
  }

  private void setEnableButtons(final boolean flag) {
    this.buttonSine.setEnabled(flag);
    this.buttonSquare.setEnabled(flag);
    this.buttonTriangle.setEnabled(flag);
    this.button44100.setEnabled(flag);
    this.button48000.setEnabled(flag);
    this.button96000.setEnabled(flag);
    this.button13700.setEnabled(flag);
    this.button15500.setEnabled(flag);
    this.button17125.setEnabled(flag);
    this.buttonCustomTime.setEnabled(flag);
  }

  public TimePanel getTimePanel() {
    return this.timePanel;
  }

  public boolean isInRendering() {
    return this.buttonStartStop.isSelected();
  }

  public int getCarrierFreq() {
    if (this.button13700.isSelected()) {
      return 13700;
    }
    if (this.button15500.isSelected()) {
      return 15500;
    }
    return 17125;
  }

  public Dcf77SignalSoundRenderer.SignalShape getSignalShape() {
    if (this.buttonSine.isSelected()) {
      return Dcf77SignalSoundRenderer.SignalShape.SIN;
    }
    if (this.buttonSquare.isSelected()) {
      return Dcf77SignalSoundRenderer.SignalShape.SQUARE;
    }
    return Dcf77SignalSoundRenderer.SignalShape.TRIANGLE;
  }

  public int getSampleRate() {
    if (this.button44100.isSelected()) {
      return 44100;
    }
    if (this.button48000.isSelected()) {
      return 48000;
    }
    return 96000;
  }
}
