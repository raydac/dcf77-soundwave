package com.igormaznitsa.gui;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createEtchedBorder;

import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import com.igormaznitsa.soundtime.AmplitudeSoundSignalRenderer;
import com.igormaznitsa.soundtime.DstDetection;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalBits;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalWavRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class AppPanel extends JPanel {

  private static final Supplier<? extends TimeOffsetProvider>
      SUPPLIER_NO_OFFSET_PROVIDER = () -> dateTime -> dateTime;

  private static final Color TIME_PANEL_NORMAL = Color.BLUE.darker().darker().darker();
  private static final Color TIME_PANEL_FIXED = Color.RED.darker().darker().darker().darker();
  private final TimePanel timePanel;
  private final StartStopButton buttonStartStop;
  private final Component progressBarReplacement;
  private final AppPanelToggleButton buttonSine;
  private final AppPanelToggleButton buttonSquare;
  private final AppPanelToggleButton buttonTriangle;
  private final AppPanelToggleButton button44100;
  private final AppPanelToggleButton button48000;
  private final AppPanelToggleButton button96000;
  private final AppPanelToggleButton buttonFreq1;
  private final AppPanelToggleButton buttonFreq2;
  private final AppPanelToggleButton buttonFreq3;
  private final AtomicReference<AmplitudeSoundSignalRenderer> currentRenderer =
      new AtomicReference<>();
  private final Supplier<AppFrame.OutputLineInfo> mixerSupplier;
  private final SignalProgressBar progressBarTime;
  private final AppPanelToggleButton buttonCustomTime;
  private final AppPanelToggleButton buttonSignalTimeShift;
  private final AppPanelToggleButton buttonForceUTCTimeZone;
  private final Supplier<? extends TimeDateIndicationProvider>
      baseTimeDateIndicationProviderSupplier;
  private final Supplier<MinuteBasedTimeSignalWavRenderer> minuteWavDataRendererSupplier;
  private final TimeDateIndicationProvider UTC_BASE_WRAPPER = new TimeDateIndicationProvider() {
    @Override
    public ZonedDateTime getZonedTimeDateNow(DstDetection dstDetection) {
      return baseTimeDateIndicationProviderSupplier.get().getZonedTimeDateNow(dstDetection)
          .withZoneSameInstant(UTC);
    }

    @Override
    public ZoneId getStandardSignalZoneId() {
      return UTC;
    }

    @Override
    public String getProtocolId() {
      return baseTimeDateIndicationProviderSupplier.get().getProtocolId();
    }
  };
  private volatile Supplier<? extends TimeOffsetProvider> currentTimeOffsetProvider;
  private volatile Supplier<? extends TimeDateIndicationProvider>
      currentTimeDateIndicationProviderSupplier;
  private int lastSelectedTimeOffsetInSeconds = 0;

  public AppPanel(
      final Supplier<AppFrame.OutputLineInfo> mixerSupplier,
      final Supplier<MinuteBasedTimeSignalWavRenderer> minuteWavDataRendererSupplier,
      final Supplier<DstDetection> dstDetectionSupplier
  ) {
    super(new BorderLayout(0, 0));
    this.setBorder(createEmptyBorder(8, 8, 8, 8));

    this.minuteWavDataRendererSupplier = requireNonNull(minuteWavDataRendererSupplier);
    this.baseTimeDateIndicationProviderSupplier = minuteWavDataRendererSupplier;
    this.currentTimeDateIndicationProviderSupplier = this.baseTimeDateIndicationProviderSupplier;

    this.currentTimeOffsetProvider = SUPPLIER_NO_OFFSET_PROVIDER;
    this.mixerSupplier = requireNonNull(mixerSupplier);
    this.timePanel = new TimePanel(() -> this.currentTimeDateIndicationProviderSupplier.get(),
        () -> this.currentTimeOffsetProvider.get(), dstDetectionSupplier);
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
    controlPanel.setBorder(BorderFactory.createCompoundBorder(
        createEmptyBorder(0, 8, 0, 0),
        createEtchedBorder()
    ));

    final Font defaultButtonFont = UIManager.getFont("Button.font");

    this.buttonSine = new AppPanelToggleButton("w");
    this.buttonSine.setToolTipText("Sine wave");
    this.buttonSine.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));
    this.buttonSquare = new AppPanelToggleButton("Q");
    this.buttonSquare.setToolTipText("Square wave");
    this.buttonSquare.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));
    this.buttonTriangle = new AppPanelToggleButton("T");
    this.buttonTriangle.setToolTipText("Triangular wave");
    this.buttonTriangle.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));

    this.buttonCustomTime = new AppPanelToggleButton("[");
    this.buttonCustomTime.setToolTipText("Render custom time");
    this.buttonCustomTime.setFont(
        GuiUtils.FONT_SOSA.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));

    this.buttonSignalTimeShift = new AppPanelToggleButton("➕/➖");
    this.buttonSignalTimeShift.setToolTipText("Time shift applied to translated signal");
    this.buttonSignalTimeShift.setFont(
        GuiUtils.FONT_JULIA.deriveFont(defaultButtonFont.getStyle(),
            defaultButtonFont.getSize2D()));

    this.buttonForceUTCTimeZone = new AppPanelToggleButton("UTC");
    this.buttonForceUTCTimeZone.setToolTipText("Force use UTC time zone for signal");
    this.buttonForceUTCTimeZone.setFont(
        GuiUtils.FONT_JULIA.deriveFont(defaultButtonFont.getStyle(),
            defaultButtonFont.getSize2D()));

    this.buttonSignalTimeShift.addActionListener(e -> {
      if (this.buttonSignalTimeShift.isSelected()) {
        final TimeOffsetSelectPanel panel =
            new TimeOffsetSelectPanel(this.lastSelectedTimeOffsetInSeconds);
        if (JOptionPane.showConfirmDialog(this, panel, "Translated Signal Time Shift",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION &&
            panel.getSeconds() != 0) {
          final int selectedSeconds = panel.getSeconds();
          this.lastSelectedTimeOffsetInSeconds = selectedSeconds;

          final int absSeconds = Math.abs(selectedSeconds);
          final Function<ZonedDateTime, ZonedDateTime> processor =
              selectedSeconds < 0 ? z -> z.minusSeconds(absSeconds) :
                  z -> z.plusSeconds(absSeconds);

          final String offsetText = String.format(
              "SHFT:%s%02d:%02d ",
              selectedSeconds < 0 ? "-" : "+",
              absSeconds / 3600,
              (absSeconds % 3600) / 60
          );

          final TimeOffsetProvider timeOffsetProvider = new TimeOffsetProvider() {
            @Override
            public ZonedDateTime apply(final ZonedDateTime dateTime) {
              return processor.apply(dateTime);
            }

            @Override
            public String getOffsetTimeText() {
              return offsetText;
            }
          };
          this.currentTimeOffsetProvider = () -> timeOffsetProvider;
        } else {
          this.buttonSignalTimeShift.setSelected(false);
          this.currentTimeOffsetProvider = SUPPLIER_NO_OFFSET_PROVIDER;
        }
      } else {
        this.currentTimeOffsetProvider = SUPPLIER_NO_OFFSET_PROVIDER;
      }
    });

    this.buttonForceUTCTimeZone.addActionListener(e -> {
      final boolean customTimeActivated = this.buttonCustomTime.isSelected();
      if (this.buttonForceUTCTimeZone.isSelected()) {
        if (!customTimeActivated) {
          this.currentTimeDateIndicationProviderSupplier =
              () -> UTC_BASE_WRAPPER;
        }
      } else {
        if (!customTimeActivated) {
          this.currentTimeDateIndicationProviderSupplier =
              this.baseTimeDateIndicationProviderSupplier;
        }
      }
    });

    this.buttonCustomTime.addActionListener(e -> {
      if (this.buttonCustomTime.isSelected()) {
        ZonedDateTime selected = this.getSelectedTime(
            this.baseTimeDateIndicationProviderSupplier.get()
                .getZonedTimeDateNow(dstDetectionSupplier.get()));

        if (selected == null) {
          this.buttonCustomTime.setSelected(false);
        } else {
          final TimeDateIndicationProvider customProvider = new TimeDateIndicationProvider() {
            @Override
            public ZonedDateTime getZonedTimeDateNow(final DstDetection dstDetection) {
              return buttonForceUTCTimeZone.isSelected() ? selected.withZoneSameInstant(UTC) :
                  selected;
            }

            @Override
            public ZoneId getStandardSignalZoneId() {
              return buttonForceUTCTimeZone.isSelected() ? UTC :
                  AppPanel.this.baseTimeDateIndicationProviderSupplier.get()
                  .getStandardSignalZoneId();
            }

            @Override
            public String getProtocolId() {
              return AppPanel.this.baseTimeDateIndicationProviderSupplier.get().getProtocolId();
            }
          };

          this.currentTimeDateIndicationProviderSupplier = () -> customProvider;
          this.timePanel.setShowSecondsChange(false);
          this.timePanel.setBackground(TIME_PANEL_FIXED);
        }
      } else {
        if (this.buttonForceUTCTimeZone.isSelected()) {
          this.currentTimeDateIndicationProviderSupplier = () -> UTC_BASE_WRAPPER;
        } else {
          this.currentTimeDateIndicationProviderSupplier =
              this.baseTimeDateIndicationProviderSupplier;
        }
        this.timePanel.setShowSecondsChange(true);
        this.timePanel.setBackground(TIME_PANEL_NORMAL);
      }
      this.timePanel.refreshTime();
    });

    final ButtonGroup buttonGroupSignal = new ButtonGroup();
    buttonGroupSignal.add(this.buttonSine);
    buttonGroupSignal.add(this.buttonSquare);
    buttonGroupSignal.add(this.buttonTriangle);

    this.buttonFreq1 = new AppPanelToggleButton("");
    this.buttonFreq2 = new AppPanelToggleButton("");
    this.buttonFreq3 = new AppPanelToggleButton("");

    final ButtonGroup buttonGroupCarrier = new ButtonGroup();
    buttonGroupCarrier.add(this.buttonFreq1);
    buttonGroupCarrier.add(this.buttonFreq2);
    buttonGroupCarrier.add(this.buttonFreq3);

    this.button44100 = new AppPanelToggleButton("44.1 KHz");
    this.button44100.setToolTipText("Sound-card sampling 44100 Hz");

    this.button48000 = new AppPanelToggleButton("48 KHz");
    this.button48000.setToolTipText("Sound-card sampling 48000 Hz");

    this.button96000 = new AppPanelToggleButton("96 KHz");
    this.button96000.setToolTipText("Sound-card sampling 96000 Hz");

    final ButtonGroup buttonGroupSampleRate = new ButtonGroup();
    buttonGroupSampleRate.add(this.button44100);
    buttonGroupSampleRate.add(this.button48000);
    buttonGroupSampleRate.add(this.button96000);

    controlPanel.add(this.buttonSine);
    controlPanel.add(this.buttonSquare);
    controlPanel.add(this.buttonTriangle);

    controlPanel.add(this.buttonFreq1);
    controlPanel.add(this.buttonFreq2);
    controlPanel.add(this.buttonFreq3);

    controlPanel.add(this.button44100);
    controlPanel.add(this.button48000);
    controlPanel.add(this.button96000);
    controlPanel.add(this.buttonSignalTimeShift);
    controlPanel.add(this.buttonCustomTime);
    controlPanel.add(this.buttonForceUTCTimeZone);

    this.add(controlPanel, BorderLayout.EAST);

    this.buttonSine.setSelected(true);
    this.button44100.setSelected(true);
    this.buttonFreq2.setSelected(true);

    this.buttonStartStop.addActionListener(e -> {
      if (this.buttonStartStop.isSelected()) {
        final AppFrame.OutputLineInfo lineInfo = this.mixerSupplier.get();
        if (lineInfo == null) {
          JOptionPane.showMessageDialog(this, "No selected output channel!", "No output",
              JOptionPane.WARNING_MESSAGE);
          this.buttonStartStop.setSelected(false);
          return;
        }
        this.startRendering(lineInfo, dstDetectionSupplier.get());
      } else {
        this.stopRendering(null);
      }
    });
  }

  public void refreshFreqButtons() {
    this.buttonFreq1.setText("");
    this.buttonFreq1.setToolTipText(null);
    this.buttonFreq2.setText("");
    this.buttonFreq2.setToolTipText(null);
    this.buttonFreq3.setText("");
    this.buttonFreq3.setToolTipText(null);

    final List<Integer> allowedCarrierFreq =
        this.minuteWavDataRendererSupplier.get().getAllowedCarrierFrequencies();
    if (!allowedCarrierFreq.isEmpty()) {
      this.buttonFreq1.setText(allowedCarrierFreq.get(0) + " Hz");
      this.buttonFreq1.setToolTipText("Carrier frequency " + allowedCarrierFreq.get(0) + " Hz");
    }
    if (allowedCarrierFreq.size() > 1) {
      this.buttonFreq2.setText(allowedCarrierFreq.get(1) + " Hz");
      this.buttonFreq2.setToolTipText("Carrier frequency " + allowedCarrierFreq.get(1) + " Hz");
    }
    if (allowedCarrierFreq.size() > 2) {
      this.buttonFreq3.setText(allowedCarrierFreq.get(2) + " Hz");
      this.buttonFreq3.setToolTipText("Carrier frequency " + allowedCarrierFreq.get(2) + " Hz");
    }
  }

  public ZonedDateTime getCurrentTimeWithShiftAwareness(final DstDetection dstDetection) {
    if (this.buttonForceUTCTimeZone.isSelected()) {
      return this.currentTimeOffsetProvider.get()
          .apply(
              this.currentTimeDateIndicationProviderSupplier.get().getZonedTimeDateNow(dstDetection)
                  .withZoneSameInstant(UTC));
    } else {
      return this.currentTimeOffsetProvider.get()
          .apply(this.currentTimeDateIndicationProviderSupplier.get()
              .getZonedTimeDateNow(dstDetection));
    }
  }

  public void dispose() {
    this.buttonStartStop.setSelected(false);
    this.stopRendering(null);
  }

  private void sendTimeData(
      final AmplitudeSoundSignalRenderer renderer,
      final int numberOfRenderedMinutes,
      final int freqHz,
      final AmplitudeSoundSignalRenderer.SignalShape shape,
      final DstDetection dstDetection
  ) {
    final MinuteBasedTimeSignalWavRenderer minuteRenderer =
        this.minuteWavDataRendererSupplier.get();

    final Thread thread = new Thread(() -> {
      ZonedDateTime zonedDateTime = this.getCurrentTimeWithShiftAwareness(dstDetection)
          .plusMinutes(1); // ensure upcoming minute

      System.out.println(
          String.format("Rendering start time (%s): %s", dstDetection, zonedDateTime));

      boolean addedSuccessfully = true;

      for (int i = 0;
           i < numberOfRenderedMinutes && !renderer.isDisposed() &&
               !Thread.currentThread().isInterrupted(); i++) {
        addedSuccessfully &=
            renderer.offer(minuteRenderer.makeTimeSignalBits(zonedDateTime, dstDetection), freqHz,
                this.minuteWavDataRendererSupplier.get().getAmplitudeDeviation(), shape);

        zonedDateTime = zonedDateTime.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
      }

      final MinuteBasedTimeSignalBits stopRecord =
          minuteRenderer.makeTimeSignalBits(zonedDateTime, dstDetection);
      addedSuccessfully &= renderer.offer(stopRecord, freqHz,
          this.minuteWavDataRendererSupplier.get().getAmplitudeDeviation(), shape);

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
      renderer.addAmplitudeSoundSignalRendererListener(
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

  private ZonedDateTime getSelectedTime(final ZonedDateTime now) {
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

    dateTimePicker.getDatePicker().setDate(now.toLocalDate());
    dateTimePicker.getTimePicker().setTime(now.toLocalTime());

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
          0, now.getZone());
    }
    return null;
  }

  private void startRendering(final AppFrame.OutputLineInfo output,
                              final DstDetection dstDetection) {
    if (this.currentRenderer.get() == null) {
      this.progressBarTime.setVisible(true);
      this.progressBarReplacement.setVisible(false);
      this.setEnableButtons(false);

      final int sampleRate = this.getSampleRate();

      final AmplitudeSoundSignalRenderer renderer = new AmplitudeSoundSignalRenderer(
          this.minuteWavDataRendererSupplier.get(),
          140,
          sampleRate,
          audioFormat -> output.line);

      if (this.currentRenderer.compareAndSet(null, renderer)) {
        try {
          renderer.initAudioLine();
          renderer.startAudio();

          final AmplitudeSoundSignalRenderer.SignalShape shape = this.getSignalShape();
          final int freq = this.getCarrierFreq();

          this.sendTimeData(renderer, 60, freq, shape, dstDetection);
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
    final AmplitudeSoundSignalRenderer renderer = this.currentRenderer.getAndSet(null);
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
    this.buttonFreq1.setEnabled(flag);
    this.buttonFreq2.setEnabled(flag);
    this.buttonFreq3.setEnabled(flag);
    this.buttonCustomTime.setEnabled(flag);
    this.buttonSignalTimeShift.setEnabled(flag);
    this.buttonForceUTCTimeZone.setEnabled(flag);
  }

  public TimePanel getTimePanel() {
    return this.timePanel;
  }

  public boolean isInRendering() {
    return this.buttonStartStop.isSelected();
  }

  public int getCarrierFreq() {
    final MinuteBasedTimeSignalWavRenderer renderer = this.minuteWavDataRendererSupplier.get();
    if (renderer == null) {
      return -1;
    }
    final List<Integer> freq = renderer.getAllowedCarrierFrequencies();
    if (this.buttonFreq1.isSelected()) {
      return freq.get(0);
    }
    if (this.buttonFreq2.isSelected()) {
      return freq.get(1);
    }
    return freq.get(2);
  }

  public AmplitudeSoundSignalRenderer.SignalShape getSignalShape() {
    if (this.buttonSine.isSelected()) {
      return AmplitudeSoundSignalRenderer.SignalShape.SIN;
    }
    if (this.buttonSquare.isSelected()) {
      return AmplitudeSoundSignalRenderer.SignalShape.SQUARE;
    }
    return AmplitudeSoundSignalRenderer.SignalShape.TRIANGLE;
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
