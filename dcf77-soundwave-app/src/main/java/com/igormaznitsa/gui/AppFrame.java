package com.igormaznitsa.gui;

import static java.time.ZoneOffset.UTC;

import com.igormaznitsa.soundtime.AmplitudeSoundSignalRenderer;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalBits;
import com.igormaznitsa.soundtime.MinuteBasedTimeSignalWavRenderer;
import com.igormaznitsa.soundtime.bpc.BpcMinuteBasedTimeSignalSignalRenderer;
import com.igormaznitsa.soundtime.dcf77.Dcf77MinuteBasedTimeSignalSignalRenderer;
import com.igormaznitsa.soundtime.jjy.JjyMinuteBasedTimeSignalSignalRenderer;
import com.igormaznitsa.soundtime.wwvb.WwvbMinuteBasedTimeSignalSignalRenderer;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

public final class AppFrame extends JFrame {

  private static final String TITLE = "DCF77 sound generator";

  private static final List<NtpTimeSource> NTP_TIME_SOURCES = List.of(
      NtpTimeSource.timeSourceOf("Localhost timer", "localhost"),
      NtpTimeSource.timeSourceSectionOf("European Pool Zones"),
      NtpTimeSource.timeSourceOf("Estonia", "ee.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Ukraine", "ua.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("UK", "uk.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Germany", "de.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("France", "fr.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Spain", "es.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Italy", "it.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Netherlands", "nl.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Norway", "no.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Portugal", "pt.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Sweden", "se.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Russia", "ru.pool.ntp.org"),
      NtpTimeSource.timeSourceSectionOf("North American Pool Zones"),
      NtpTimeSource.timeSourceOf("USA", "us.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Canada", "ca.pool.ntp.org"),
      NtpTimeSource.timeSourceSectionOf("Asian Pool Zones"),
      NtpTimeSource.timeSourceOf("United Arab Emirates", "ae.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("China", "cn.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("India", "in.pool.ntp.org"),
      NtpTimeSource.timeSourceOf("Saudi Arabia", "sa.pool.ntp.org"),
      NtpTimeSource.timeSourceSectionOf("Public servers"),
      NtpTimeSource.timeSourceOf("Apple", "time.apple.com"),
      NtpTimeSource.timeSourceOf("Facebook", "time.facebook.com"),
      NtpTimeSource.timeSourceOf("Microsoft", "time.windows.com")
  );

  private static final List<Mode> MODES = List.of(
      new Mode("DCF77 (Germany)",
          "German long-wave time signal and standard-frequency radio station, CET time zone",
          Dcf77MinuteBasedTimeSignalSignalRenderer.INSTANCE),
      new Mode("JJY (Japan)", "Japan low frequency time signal radio station, JST time zone",
          JjyMinuteBasedTimeSignalSignalRenderer.INSTANCE),
      new Mode("WWVB (USA)", "North America low frequency time signal radio station, UTC time zone",
          WwvbMinuteBasedTimeSignalSignalRenderer.INSTANCE),
      new Mode("BPC (China)",
          " BPC Shangqiu low frequency time signal radio station, CHN time zone",
          BpcMinuteBasedTimeSignalSignalRenderer.INSTANCE)
  );

  private static final FileFilter FILE_FILTER_WAV =
      new FileNameExtensionFilter("WAV file (*.wav)", "wav");
  private static final int NTP_REFRESH_DELAY_MS = 333;
  private static final int NTP_TIMEOUT_MS = 600;
  private static final int MAX_NTP_ERROR_COUNTER = 5;
  private final Timer timer;
  private final AppPanel appPanel;
  private final AtomicReference<OutputLineInfo> currentMixer = new AtomicReference<>();
  private final AtomicReference<Instant> currentTime = new AtomicReference<>(Instant.now());
  private final AtomicReference<NTPUDPClient> currentNtpUDpClient = new AtomicReference<>();
  private final ScheduledExecutorService timerNtpRefresh = new ScheduledThreadPoolExecutor(1,
      r -> {
        final Thread thread = new Thread(r, "timer-ntp-refresh");
        thread.setDaemon(true);
        return thread;
      }, (r, executor) -> System.err.println("Detected rejected ntp task"));
  private final AtomicReference<ScheduledFuture<?>> ntpScheduledFuture = new AtomicReference<>();
  private final AtomicReference<MinuteBasedTimeSignalWavRenderer> currentTimeSignalRenderer =
      new AtomicReference<>();
  private File lastSavedFile;

  public AppFrame() {
    super(TITLE);
    this.setIconImages(loadAppIcons());

    this.currentMixer.set(findDefaultOutputMixer());
    this.appPanel = new AppPanel(
        this.currentMixer::get,
        this::getCurrentMinuteWavDataRenderer
    );
    this.setJMenuBar(this.makeMenuBar());
    this.setContentPane(this.appPanel);
    this.pack();
    this.setResizable(false);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        final NTPUDPClient ntpudpClient = AppFrame.this.currentNtpUDpClient.getAndSet(null);
        if (ntpudpClient != null) {
          try {
            System.out.println("Closing NTP client");
            ntpudpClient.close();
          } catch (Exception ignore) {
          }
        }
        AppFrame.this.timerNtpRefresh.shutdownNow();
        var scheduledFuture = AppFrame.this.ntpScheduledFuture.getAndSet(null);
        if (scheduledFuture != null) {
          scheduledFuture.cancel(true);
        }
        AppFrame.this.appPanel.dispose();
        AppFrame.this.timer.stop();
        AppFrame.this.dispose();
      }
    });

    this.timer = new Timer(NTP_REFRESH_DELAY_MS, a -> this.appPanel.getTimePanel().refreshTime());
    this.timer.start();
    this.appPanel.getTimePanel().refreshTime();
    this.appPanel.refreshFreqButtons();
  }

  private static List<Image> loadAppIcons() {
    final List<Image> result = new ArrayList<>();
    result.add(GuiUtils.loadIcon("applogo16x16.png").getImage());
    result.add(GuiUtils.loadIcon("applogo32x32.png").getImage());
    result.add(GuiUtils.loadIcon("applogo64x64.png").getImage());
    result.add(GuiUtils.loadIcon("applogo128x128.png").getImage());
    result.add(GuiUtils.loadIcon("applogo256x256.png").getImage());
    return result;
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

  public static void ensureAppropriateLF() {
    final Runnable runnable = () -> {
      boolean systemLook = SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC;
      if (!systemLook) {
        try {
          for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if (info.getName().toLowerCase(Locale.ENGLISH).contains("nimbus")) {
              UIManager.setLookAndFeel(info.getClassName());
              break;
            }
          }
        } catch (Exception e) {
          systemLook = true;
        }
      }

      if (systemLook) {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
          // do nothing
        }
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  private MinuteBasedTimeSignalWavRenderer getCurrentMinuteWavDataRenderer() {
    return this.currentTimeSignalRenderer.get();
  }

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

      final MinuteBasedTimeSignalWavRenderer minuteBasedTimeSignalWavRenderer =
          this.getCurrentMinuteWavDataRenderer();

      final AmplitudeSoundSignalRenderer renderer =
          new AmplitudeSoundSignalRenderer(minuteBasedTimeSignalWavRenderer, 120,
              this.appPanel.getSampleRate(),
              a -> null);

      ZonedDateTime time = this.appPanel.getCurrentTime().withZoneSameInstant(UTC)
          .plusMinutes(1) // ensure upcoming minute
          .truncatedTo(ChronoUnit.MINUTES);

      final List<MinuteBasedTimeSignalBits> recordList = new ArrayList<>();
      for (int i = 0; i < minutes; i++) {
        recordList.add(minuteBasedTimeSignalWavRenderer.makeTimeSignalBits(time));
        time = time.plusMinutes(1);
      }
      try {
        final byte[] wavData = renderer.renderWav(recordList, this.appPanel.getCarrierFreq(),
            minuteBasedTimeSignalWavRenderer.getAmplitudeDeviation(),
            this.appPanel.getSignalShape());
        FileUtils.writeByteArrayToFile(file, wavData);
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
    final JMenu menuMode = new JMenu("Mode");
    final ButtonGroup modeButtonGroup = new ButtonGroup();
    MODES.forEach(x -> {
      final JRadioButtonMenuItem modeButton = new JRadioButtonMenuItem(x.name);
      modeButtonGroup.add(modeButton);
      modeButton.setToolTipText(x.toolTip);
      modeButton.addActionListener(a -> {
        System.out.println("Selected mode: " + x.name);
        this.currentTimeSignalRenderer.set(x.renderer);
        this.appPanel.refreshFreqButtons();
      });
      menuMode.add(modeButton);
    });

    final JMenu menuSettings = new JMenu("Settings");
    final JMenu menuHelp = new JMenu("Help");

    final JMenuItem menuSaveAs = new JMenuItem("Save as..", GuiUtils.loadIcon("file_save_as.png"));
    menuSaveAs.addActionListener(a -> this.saveAs());

    menuFile.add(menuSaveAs);
    menuFile.add(new JSeparator());
    final JMenuItem menuExit = new JMenuItem("Exit", GuiUtils.loadIcon("door_in.png"));
    menuExit.addActionListener(
        x -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
    menuFile.add(menuExit);

    final JMenuItem menuShowHelp = new JMenuItem("Help", GuiUtils.loadIcon("help.png"));
    final JMenuItem menuDoDonate = new JMenuItem("Donate", GuiUtils.loadIcon("moneybox.png"));
    final JMenuItem menuShowAbout = new JMenuItem("About", GuiUtils.loadIcon("information.png"));

    menuShowHelp.addActionListener(a -> this.showHelp());

    menuDoDonate.addActionListener(a -> {
      try {
        GuiUtils.browseURI(new URI(
                "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2"),
            true);
      } catch (Exception ex) {
        // ignore
      }
    });
    menuShowAbout.addActionListener(a -> this.showAbout());

    menuHelp.add(menuShowHelp);
    menuHelp.add(menuDoDonate);
    menuHelp.add(menuShowAbout);

    final ButtonGroup buttonGroupMenuSourceNtpServer = new ButtonGroup();
    final JMenu menuSourceNtpServer = new JMenu("Time sources");
    menuSourceNtpServer.setIcon(GuiUtils.loadIcon("time_go.png"));

    final AtomicInteger ntpErrorCounter = new AtomicInteger(MAX_NTP_ERROR_COUNTER);

    final Runnable activateFirstNtpSource =
        () -> Arrays.stream(menuSourceNtpServer.getMenuComponents())
            .filter(x -> x instanceof JRadioButtonMenuItem)
            .map(x -> (JRadioButtonMenuItem) x)
            .findFirst()
            .ifPresent(AbstractButton::doClick);

    for (final NtpTimeSource timeSource : NTP_TIME_SOURCES) {
      if (timeSource instanceof NtpTimeSourceSectionHeader) {
        menuSourceNtpServer.add(new JLabel("<html><b>" + timeSource.name + "</b></html>"));
        menuSourceNtpServer.add(new JSeparator());
      } else {
        final JRadioButtonMenuItem radioButtonMenuItem = new JRadioButtonMenuItem(timeSource.name);
        buttonGroupMenuSourceNtpServer.add(radioButtonMenuItem);
        menuSourceNtpServer.add(radioButtonMenuItem);

        if ("localhost".equalsIgnoreCase(timeSource.address)) {
          radioButtonMenuItem.addActionListener(l -> {
            AppFrame.this.setTitle(TITLE);
            System.out.println("Selected time source: " + timeSource);
            ntpErrorCounter.set(MAX_NTP_ERROR_COUNTER);
            var future = this.ntpScheduledFuture.getAndSet(null);
            if (future != null) {
              future.cancel(true);
            }
            final ScheduledFuture<?> newFuture = this.timerNtpRefresh.scheduleAtFixedRate(() -> {
                  final NTPUDPClient ntpudpClient = this.currentNtpUDpClient.getAndSet(null);
                  if (ntpudpClient != null) {
                    try {
                      System.out.println("Closing NTP client");
                      ntpudpClient.close();
                    } catch (Exception ignore) {
                    }
                  }
                  this.currentTime.set(Instant.now());
                },
                0L, NTP_REFRESH_DELAY_MS, TimeUnit.MILLISECONDS);
            if (!this.ntpScheduledFuture.compareAndSet(null, newFuture)) {
              System.err.println("Can't set new future, unexpected racing!");
              newFuture.cancel(true);
            }
          });
        } else {
          try {
            final InetAddress inetAddress = InetAddress.getByName(timeSource.address);
            radioButtonMenuItem.setToolTipText(timeSource.address);
            radioButtonMenuItem.addActionListener(l -> {
              AppFrame.this.setTitle(TITLE + " (ntp://" + timeSource.address + ')');
              System.out.println("Selected NTP server: " + timeSource);

              var future = this.ntpScheduledFuture.getAndSet(null);
              if (future != null) {
                future.cancel(true);
              }
              final ScheduledFuture<?> newFuture =
                  this.timerNtpRefresh.scheduleAtFixedRate(() -> {
                    long time;
                    NTPUDPClient ntpudpClient = this.currentNtpUDpClient.get();
                    if (ntpudpClient == null) {
                      System.out.println("Creating NTP client");
                      ntpudpClient = new NTPUDPClient();
                      ntpudpClient.setDefaultTimeout(Duration.ofMillis(NTP_TIMEOUT_MS));
                      try {
                        ntpudpClient.open();
                        this.currentNtpUDpClient.set(ntpudpClient);
                        System.out.println("NTP client successfully created and opened");
                      } catch (Exception ex) {
                        ntpudpClient = null;
                      }
                    }

                    if (ntpudpClient == null) {
                      var thisFuture = this.ntpScheduledFuture.get();
                      thisFuture.cancel(false);
                      SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(AppFrame.this,
                            "Can't initialize NTP client", "Error",
                            JOptionPane.ERROR_MESSAGE);
                        activateFirstNtpSource.run();
                      });
                      time = System.currentTimeMillis();
                    } else {
                      try {
                        ntpErrorCounter.set(MAX_NTP_ERROR_COUNTER);
                        final TimeInfo info = ntpudpClient.getTime(inetAddress);
                        time = System.currentTimeMillis() + info.getOffset();
                      } catch (Exception ex) {
                        if (ntpErrorCounter.decrementAndGet() <= 0) {
                          var thisFuture = this.ntpScheduledFuture.get();
                          thisFuture.cancel(false);
                          SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(AppFrame.this,
                                "Can't get NTP packets from " + timeSource.address + ": " +
                                    ex.getMessage(), "Can't get NTP packets",
                                JOptionPane.WARNING_MESSAGE);
                            activateFirstNtpSource.run();
                          });
                        }
                        time = System.currentTimeMillis();
                      }
                    }
                    this.currentTime.set(Instant.ofEpochMilli(time));
                  }, 0L, NTP_REFRESH_DELAY_MS, TimeUnit.MILLISECONDS);

              if (!this.ntpScheduledFuture.compareAndSet(null, newFuture)) {
                System.err.println("Can't set new future, unexpected racing!");
                newFuture.cancel(true);
              }

            });
          } catch (Exception ex) {
            radioButtonMenuItem.setEnabled(false);
          }
        }
      }
    }
    menuSettings.add(menuSourceNtpServer);

    final JMenu menuOutputDevices = new JMenu("Output device");
    menuOutputDevices.setIcon(GuiUtils.loadIcon("sound.png"));
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
    menuBar.add(menuMode);
    menuBar.add(menuSettings);
    menuBar.add(menuHelp);

    activateFirstNtpSource.run();
    Arrays.stream(menuMode.getMenuComponents())
        .filter(x -> x instanceof JRadioButtonMenuItem)
        .findFirst()
        .ifPresent(x -> ((JRadioButtonMenuItem) x).doClick());
    return menuBar;
  }

  private void showHelp() {
    try (final InputStream inputStream = Objects.requireNonNull(
        AppFrame.class.getResourceAsStream("/help/help.html"))) {
      final String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      final JPanel panel = new JPanel(new BorderLayout());
      final JEditorPane editorPane = new JEditorPane("text/html", text);
      editorPane.setEditable(false);
      final JScrollPane scrollPane = new JScrollPane(editorPane);
      panel.add(scrollPane, BorderLayout.CENTER);
      GuiUtils.makeOwningDialogResizable(scrollPane);
      JOptionPane.showMessageDialog(this, scrollPane, "Help", JOptionPane.PLAIN_MESSAGE);
    } catch (IOException ex) {
      // ignore
    }
  }

  private void showAbout() {
    JOptionPane.showMessageDialog(this, new AboutPanel(), "About", JOptionPane.PLAIN_MESSAGE,
        GuiUtils.loadIcon("applogo64x64.png"));
  }

  private static class NtpTimeSourceSectionHeader extends NtpTimeSource {
    public NtpTimeSourceSectionHeader(final String name) {
      super(name, null);
    }
  }

  private static class Mode {
    private final String name;
    private final String toolTip;
    private final MinuteBasedTimeSignalWavRenderer renderer;

    Mode(
        final String name,
        final String toolTip,
        final MinuteBasedTimeSignalWavRenderer renderer
    ) {
      this.name = name;
      this.toolTip = toolTip;
      this.renderer = renderer;
    }

  }

  private static class NtpTimeSource {
    private final String name;
    private final String address;

    NtpTimeSource(final String name, final String address) {
      this.name = name;
      this.address = address;
    }

    static NtpTimeSourceSectionHeader timeSourceSectionOf(final String name) {
      return new NtpTimeSourceSectionHeader(name);
    }

    static NtpTimeSource timeSourceOf(final String name, final String address) {
      return new NtpTimeSource(name, address);
    }

    @Override
    public String toString() {
      return "NtpTimeSource{" +
          "name='" + this.name + '\'' +
          ", address='" + this.address + '\'' +
          '}';
    }
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
