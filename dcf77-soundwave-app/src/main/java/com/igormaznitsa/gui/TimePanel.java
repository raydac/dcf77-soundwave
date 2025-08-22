package com.igormaznitsa.gui;

import static javax.swing.BorderFactory.createBevelBorder;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

public class TimePanel extends JPanel {

  private final JLabel labelCest;
  private final JLabel labelTimeDate;
  private final Supplier<TimeDateIndicationProvider> timeSupplier;
  private boolean showSecondsChange = true;

  public TimePanel(final Supplier<TimeDateIndicationProvider> timeSupplier) {
    super(new GridBagLayout());
    this.setBorder(
        createCompoundBorder(
            createBevelBorder(BevelBorder.LOWERED),
            createEmptyBorder(16, 16, 16, 16)
        )
    );

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
    this.add(labelCest, gbc);

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
        this.labelTimeDate.setText("00:00 0000-AAA-00");
        this.labelCest.setText("....");
        return;
      }
      final ZonedDateTime time = provider.getZonedTimeDateNow();

      final int hours = time.getHour();
      final int minute = time.getMinute();
      final int year = time.getYear();
      final String month = time.getMonth().getDisplayName(TextStyle.SHORT, Locale.ROOT);
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
