package com.igormaznitsa.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class TimePanel extends javax.swing.JPanel {

  private final JLabel labelCest;
  private final JLabel labelTimeDate;
  private final Supplier<TimeDateIndicationProvider> timeSupplier;
  private boolean showSecondsChange = true;

  public TimePanel(final Supplier<TimeDateIndicationProvider> timeSupplier) {
    this.timeSupplier = timeSupplier == null ? () -> new TimeDateIndicationProvider() {
      @Override
      public ZonedDateTime getZonedTimeDateNow() {
        return ZonedDateTime.now();
      }

      @Override
      public String getIndicationText() {
        return "NULL";
      }
    } : timeSupplier;

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

    this.refreshTime();
  }

  public boolean isShowSecondsChange() {
    return this.showSecondsChange;
  }

  public void setShowSecondsChange(final boolean flag) {
    this.showSecondsChange = flag;
  }

  public void refreshTime() {
    final Runnable runnable = () -> {
      final TimeDateIndicationProvider provider = this.timeSupplier.get();
      if (provider == null) {
        this.labelTimeDate.setText("--:-- ------------");
        this.labelCest.setText("....");
        return;
      }
      final ZonedDateTime time = provider.getZonedTimeDateNow();

      final int hours = time.getHour();
      final int minute = time.getMinute();
      final int year = time.getYear();
      final String month = time.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
      final int date = time.getDayOfMonth();

      final String sec;
      if (this.showSecondsChange) {
        sec = time.getSecond() % 2 == 0 ? ":" : " ";
      } else {
        sec = ":";
      }
      final String timeText =
          String.format("%02d%s%02d %04d-%s-%02d", hours, sec, minute, year, month, date);

      this.labelTimeDate.setText(timeText);
      this.labelCest.setText(provider.getIndicationText());

      this.labelTimeDate.repaint();
      this.labelCest.repaint();
    };
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }
}
