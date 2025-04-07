package com.igormaznitsa.gui;

import com.igormaznitsa.dcf77soundwave.Dcf77Record;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public final class AppFrame extends JFrame {

  private final Timer timer;
  private final AppPanel appPanel;
  private final AtomicReference<Mixer.Info> currentMixer = new AtomicReference<>();

  public AppFrame() {
    super("Central European Time DCF77 sound generator");

    this.currentMixer.set(findDefaultOutputMixer());

    this.setJMenuBar(this.makeMenuBar());

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

  public static Mixer.Info findDefaultOutputMixer() {
    try {
      AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
      DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

      // Get the default line (from the default mixer)
      SourceDataLine defaultLine = (SourceDataLine) AudioSystem.getLine(info);

      // Now find the mixer that provides this line
      for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        if (!mixer.isLineSupported(info)) {
          continue;
        }

        try {
          SourceDataLine line = (SourceDataLine) mixer.getLine(info);
          // Compare classes and hashcodes to find a match
          if (line.getClass().equals(defaultLine.getClass())) {
            return mixerInfo;
          }
        } catch (LineUnavailableException | IllegalArgumentException e) {
          // Skip unavailable mixers
        }
      }
    } catch (LineUnavailableException e) {
      System.err.println("Failed to get default output line: " + e.getMessage());
    }
    return null;
  }

  public static List<OutputLineInfo> getOutputLinesWithNames() {
    List<OutputLineInfo> outputLines = new ArrayList<>();
    Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

    for (final Mixer.Info mixerInfo : mixerInfos) {
      final Mixer mixer = AudioSystem.getMixer(mixerInfo);
      final Line.Info[] sourceLineInfos = mixer.getSourceLineInfo();

      for (final Line.Info lineInfo : sourceLineInfos) {
        if (lineInfo instanceof DataLine.Info) {
          final DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;

          if (SourceDataLine.class.isAssignableFrom(dataLineInfo.getLineClass())) {
            try {
              Line line = mixer.getLine(dataLineInfo);
              if (line instanceof SourceDataLine) {
                outputLines.add(new OutputLineInfo((SourceDataLine) line, mixerInfo));
              }
            } catch (LineUnavailableException | IllegalArgumentException e) {
              // Skip unavailable lines
            }
          }
        }
      }
    }

    return outputLines;
  }

  private JMenuBar makeMenuBar() {
    final JMenuBar menuBar = new JMenuBar();
    final JMenu menuFile = new JMenu("File");
    final JMenu menuSettings = new JMenu("Settings");
    final JMenu menuHelp = new JMenu("Help");

    menuFile.add(new JSeparator());
    final JMenuItem menuExit = new JMenuItem("Exit");
    menuExit.addActionListener(
        x -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
    menuFile.add(menuExit);

    final JMenuItem menuShowHelp = new JMenuItem("Help");
    final JMenuItem menuDoDonate = new JMenuItem("Donate");
    final JMenuItem menuShowAbout = new JMenuItem("About");

    menuHelp.add(menuShowHelp);
    menuHelp.add(menuDoDonate);
    menuHelp.add(menuShowAbout);

    final JMenu menuOutputDevices = new JMenu("Output device");
    menuSettings.add(menuOutputDevices);
    menuOutputDevices.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected(MenuEvent e) {
        menuOutputDevices.removeAll();
        getOutputLinesWithNames().forEach(x -> {
          final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(x.mixerInfo.getName(),
              x.mixerInfo.equals(currentMixer.get()));
          menuOutputDevices.add(menuItem);
        });

      }

      @Override
      public void menuDeselected(MenuEvent e) {

      }

      @Override
      public void menuCanceled(MenuEvent e) {

      }
    });

    menuBar.add(menuFile);
    menuBar.add(menuSettings);
    menuBar.add(menuHelp);

    return menuBar;
  }

  public static class OutputLineInfo {
    public final SourceDataLine line;
    public final Mixer.Info mixerInfo;

    public OutputLineInfo(SourceDataLine line, Mixer.Info mixerInfo) {
      this.line = line;
      this.mixerInfo = mixerInfo;
    }

    @Override
    public String toString() {
      return mixerInfo.getName() + " - " + mixerInfo.getDescription();
    }
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
