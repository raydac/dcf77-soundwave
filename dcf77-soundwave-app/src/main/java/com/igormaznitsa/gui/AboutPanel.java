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
            "<html>Version 1.0.0" +
                "<hr>" +
                "The application simulates a Central European Time station using the DCF77 protocol over the audio spectrum." +
                "<hr>©2025 Igor Maznitsa<br>" +
                "Apache License 2.0<br>" +
                "<a href=\"https://github.com/raydac/dcf77-soundwave\">https://github.com/raydac/dcf77-soundwave</a>" +
                "<hr>" +
                "<p><b>Third-party components and references to them</b>" +
                "<table border=1>" +
                "<li>" +
                "<tr><td><b>LGoodDatePicker</b></td><td>MIT license</td><td>https://github.com/LGoodDatePicker/LGoodDatePicker</td></tr>" +
                "<tr><td><b>Free FatCow-Farm Fresh Icons</b></td><td>CCA 3.0 license</td><td>http://www.fatcow.com/free-icons</td></tr>" +
                "<tr><td><b>Font SOSA</b></td><td>Freeware license</td><td>https://www.fontspace.com/sosa-font-f14893</td></tr>" +
                "<tr><td><b>Font ADSR</b></td><td>Unknown license</td><td>https://discourse.zynthian.org/t/a-synth-symbol-ttf-font/4324</td></tr>" +
                "<tr><td><b>Font Digital 7</b></td><td>Freeware license</td><td>http://www.styleseven.com</td></tr>" +
                "</table>" +
                "</p" +
                "</html>",
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