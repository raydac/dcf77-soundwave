package com.igormaznitsa.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AboutPanel extends JPanel {

  public AboutPanel() {
    super(new BorderLayout(5, 5));

    final JLabel titleLabel =
        new JLabel("<html><h2>CET DCF77 sound wave</h2></html>", JLabel.CENTER);
    final JLabel infoLabel =
        new JLabel(
            "<html>Version 1.0.0<hr>The application simulates a Central European Time station<br>using the DCF77 protocol over the audio spectrum.<hr>©2025 Igor Maznitsa<br><a href=\"https://github.com/raydac/dcf77-soundwave\">https://github.com/raydac/dcf77-soundwave</a></html>",
            JLabel.CENTER);
    infoLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
          if (Desktop.isDesktopSupported() &&
              Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
              GuiUtils.browseURI(new URI("https://github.com/raydac/dcf77-soundwave"), true);
            } catch (Exception ex) {
              // nothing
            }
          }
        }
      }
    });

    this.add(titleLabel, BorderLayout.NORTH);
    this.add(infoLabel, BorderLayout.CENTER);
  }
}