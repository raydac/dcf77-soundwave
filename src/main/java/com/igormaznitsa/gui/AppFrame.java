package com.igormaznitsa.gui;

import com.igormaznitsa.dcf77soundwave.Dcf77Record;
import com.igormaznitsa.dcf77soundwave.Dcf77SignalSoundRenderer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class AppFrame extends JFrame {

  private final Timer timer;
  private final AppPanel appPanel;
  private final AtomicReference<OutputLineInfo> currentMixer = new AtomicReference<>();

  public AppFrame() {
    super("Central European Time DCF77 sound generator");

    this.currentMixer.set(findDefaultOutputMixer());
    this.setJMenuBar(this.makeMenuBar());
    this.appPanel = new AppPanel(this.currentMixer::get);

    this.setContentPane(this.appPanel);
    this.setLocationRelativeTo(null);
    this.pack();
    this.setResizable(false);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        AppFrame.this.appPanel.dispose();
        AppFrame.this.timer.stop();
        AppFrame.this.dispose();
      }
    });

    this.timer = new Timer(500, a -> {
      this.appPanel.getTimePanel().setTime(ZonedDateTime.now(Dcf77Record.ZONE_CET));
    });
    this.timer.start();
    this.appPanel.getTimePanel().setTime(ZonedDateTime.now(Dcf77Record.ZONE_CET));
  }

  public static OutputLineInfo findDefaultOutputMixer() {
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
            return new OutputLineInfo(line, mixerInfo);
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

  private static final FileFilter FILE_FILTER_WAV =
      new FileNameExtensionFilter("WAV file (*.wav)", "wav");
  private File lastSavedFile;

  private void saveAs() {
    final JFileChooser fileChooser =
        new JFileChooser(this.lastSavedFile == null ? null : this.lastSavedFile.getParentFile());
    fileChooser.setAcceptAllFileFilterUsed(false);
    fileChooser.addChoosableFileFilter(FILE_FILTER_WAV);
    fileChooser.setDialogTitle("Render signal as a file");

    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".wav")) {
        file = new File(file.getParentFile(), file.getName() + ".wav");
      }
      this.lastSavedFile = file;
      boolean enter = true;
      int minutes = 15;
      while (enter) {
        final String entered =
            JOptionPane.showInputDialog(this, "Number of rendered minutes (1..60)", 15);
        if (entered == null) {
          return;
        } else {
          try {
            minutes = Integer.parseInt(entered.trim());
            if (minutes <= 0 || minutes > 60) {
              throw new NumberFormatException();
            }
            enter = false;
          } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Value must be 1..60", "Wrong value",
                JOptionPane.WARNING_MESSAGE);
          }
        }
      }

      final Dcf77SignalSoundRenderer renderer =
          new Dcf77SignalSoundRenderer(120, this.appPanel.getSampleRate(), a -> null);
      ZonedDateTime time = ZonedDateTime.now(Dcf77Record.ZONE_CET);
      final List<Dcf77Record> recordList = new ArrayList<>();
      for (int i = 0; i < minutes; i++) {
        recordList.add(new Dcf77Record(time));
      }
      try {
        final byte[] wavData = renderer.renderWav(false, recordList, this.appPanel.getCarrierFreq(),
            Dcf77SignalSoundRenderer.DCF77_STANDARD_AMPLITUDE_DEVIATION,
            this.appPanel.getSignalShape());
        Files.write(file.toPath(), wavData);
        JOptionPane.showMessageDialog(this,
            "Successfully saved as a WAV file, length " + wavData.length + " byte(s)", "Completed",
            JOptionPane.INFORMATION_MESSAGE);
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, "IO error: " + ex.getMessage(), "Error",
            JOptionPane.ERROR_MESSAGE);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private JMenuBar makeMenuBar() {
    final JMenuBar menuBar = new JMenuBar();
    final JMenu menuFile = new JMenu("File");

    final JMenu menuSettings = new JMenu("Settings");
    final JMenu menuHelp = new JMenu("Help");

    final JMenuItem menuSaveAs = new JMenuItem("Save as..");
    menuSaveAs.addActionListener(a -> this.saveAs());

    menuFile.add(menuSaveAs);
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
        final OutputLineInfo current = currentMixer.get();
        final Mixer.Info currentMixer = current == null ? null : current.mixerInfo;
        getOutputLinesWithNames().forEach(x -> {
          final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(x.mixerInfo.getName(),
              x.mixerInfo.equals(currentMixer));
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

    menuSettings.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected(MenuEvent e) {
        menuOutputDevices.setEnabled(!appPanel.isInRendering());
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

}
