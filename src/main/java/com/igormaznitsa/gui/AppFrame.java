package com.igormaznitsa.gui;

import com.igormaznitsa.dcf77soundwave.Dcf77Record;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.ZonedDateTime;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

public class AppFrame extends JFrame {

  private final Timer timer;
  private final AppPanel appPanel;

  public AppFrame() {
    super("Central European Time DCF77 sound emulator");

    this.appPanel = new AppPanel();

    this.setContentPane(this.appPanel);
    this.setLocationRelativeTo(null);
    this.pack();
    this.setResizable(false);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        timer.stop();
        AppFrame.this.dispose();
      }
    });

    this.timer = new Timer(500, a -> {
      this.appPanel.getTimePanel().setTime(ZonedDateTime.now(Dcf77Record.ZONE_CET));
    });
    this.timer.start();

  }

  public static void main(String... args) {
    SwingUtilities.invokeLater(() -> {
      try {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
      } catch (Exception e) {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
          // nnnn
        }
      }


      final AppFrame f = new AppFrame();
      f.setVisible(true);
    });
  }

}
