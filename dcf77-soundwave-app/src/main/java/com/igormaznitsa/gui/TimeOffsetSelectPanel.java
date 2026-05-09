package com.igormaznitsa.gui;

import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import java.awt.BorderLayout;
import java.time.LocalTime;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class TimeOffsetSelectPanel extends JPanel {

  private static final int MAX_SECONDS = 24 * 60 * 60;

  private final JComboBox<String> signBox;
  private final TimePicker timePicker;

  public TimeOffsetSelectPanel(int initialSignedSeconds) {

    setLayout(new BorderLayout(10, 10));
    setBorder(new EmptyBorder(12, 12, 12, 12));

    JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));

    this.signBox = new JComboBox<>(new String[] {"+", "-"});

    final TimePickerSettings settings = new TimePickerSettings();
    settings.use24HourClockFormat();

    settings.generatePotentialMenuTimes(
        TimePickerSettings.TimeIncrement.FifteenMinutes,
        LocalTime.of(0, 0),
        LocalTime.of(23, 59)
    );

    settings.setDisplaySpinnerButtons(true);
    settings.setFormatForDisplayTime("HH:mm:ss");
    settings.setFormatForMenuTimes("HH:mm:ss");

    this.timePicker = new TimePicker(settings);

    row.add(this.signBox);
    row.add(this.timePicker);

    add(row, BorderLayout.CENTER);

    setSeconds(initialSignedSeconds);
  }

  public int getSeconds() {
    final LocalTime localTime = this.timePicker.getTime();
    if (localTime == null) {
      return 0;
    }
    int seconds = localTime.getHour() * 3600 + localTime.getMinute() * 60 + localTime.getSecond();
    if ("-".equals(this.signBox.getSelectedItem())) {
      seconds = -seconds;
    }
    return seconds;
  }

  public void setSeconds(int signedSeconds) {
    if (signedSeconds > MAX_SECONDS) {
      signedSeconds = MAX_SECONDS;
    }

    if (signedSeconds < -MAX_SECONDS) {
      signedSeconds = -MAX_SECONDS;
    }

    signBox.setSelectedItem(signedSeconds < 0 ? "-" : "+");

    int abs = Math.abs(signedSeconds);

    int h = abs / 3600;
    int m = (abs % 3600) / 60;
    int s = abs % 60;

    if (h == 24) {
      h = 23;
      m = 59;
      s = 59;
    }

    this.timePicker.setTime(LocalTime.of(h, m, s));
  }

}
