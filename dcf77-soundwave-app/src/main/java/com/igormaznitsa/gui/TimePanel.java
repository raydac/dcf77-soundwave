package com.igormaznitsa.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import javax.swing.JLabel;

public class TimePanel extends javax.swing.JPanel {

  private final JLabel labelCest;
  private final JLabel labelTimeDate;
  private ZonedDateTime time = ZonedDateTime.now();

  public TimePanel() {
    GridBagConstraints gbc;

    labelTimeDate = new JLabel();
    labelCest = new JLabel();

    this.setBackground(new Color(0, 0, 66));
    this.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
    this.setLayout(new java.awt.GridBagLayout());

    this.labelTimeDate.setFont(GuiUtils.FONT_DIGITAL.deriveFont(Font.PLAIN, 50));
    this.labelTimeDate.setForeground(new Color(255, 165, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.ipady = 16;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    add(labelTimeDate, gbc);

    labelCest.setFont(GuiUtils.FONT_DIGITAL.deriveFont(Font.PLAIN, 18));
    labelCest.setForeground(new Color(0, 255, 255));
    labelCest.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelCest.setText("CEST");
    labelCest.setVerticalAlignment(javax.swing.SwingConstants.TOP);
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.ipady = 16;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.insets = new java.awt.Insets(16, 0, 0, 0);
    add(labelCest, gbc);

    this.refreshTimeView();
  }

  public ZonedDateTime getTime() {
    return this.time;
  }

  public void setTime(ZonedDateTime time) {
    this.time = Objects.requireNonNull(time);
    this.refreshTimeView();
  }

  public void refreshTimeView() {
    final int hours = this.time.getHour();
    final int minute = this.time.getMinute();
    final int year = this.time.getYear();
    final String month = this.time.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    final int date = this.time.getDayOfMonth();

    final String sec = LocalTime.now().getSecond() % 2 == 0 ? ":" : " ";
    final String timeText =
        String.format("%02d%s%02d %04d-%s-%02d", hours, sec, minute, year, month, date);

    this.labelTimeDate.setText(timeText);
    this.labelCest.setText(
        this.time.getZone().getRules().isDaylightSavings(this.time.toInstant()) ? "CEST" : "CET");

    this.labelTimeDate.repaint();
    this.labelCest.repaint();
  }
}
