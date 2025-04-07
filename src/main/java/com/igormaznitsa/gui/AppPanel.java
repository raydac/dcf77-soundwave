package com.igormaznitsa.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

public class AppPanel extends JPanel {

  private final TimePanel timePanel;
  private final StartStopButton buttonStartStop;
  private final JProgressBar progressBarTime;
  private final Component progressBarReplacement;
  private final ControlButton buttonSine;
  private final ControlButton buttonSquare;
  private final ControlButton buttonTriangle;
  private final ControlButton button44100;
  private final ControlButton button48000;
  private final ControlButton button96000;
  private final ControlButton button13700;
  private final ControlButton button15500;
  private final ControlButton button17125;

  public AppPanel() {
    super(new BorderLayout(0, 0));
    this.timePanel = new TimePanel();
    this.progressBarTime = new JProgressBar(JProgressBar.HORIZONTAL);
    this.progressBarTime.setStringPainted(false);
    this.progressBarTime.setBorderPainted(false);
    this.progressBarTime.setIndeterminate(true);

    this.add(this.timePanel, BorderLayout.CENTER);

    this.buttonStartStop = new StartStopButton();
    final JPanel bottomPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    bottomPanel.add(this.buttonStartStop, gbc);
    gbc.gridx = 1;
    gbc.weightx = 100000;
    bottomPanel.add(this.progressBarTime, gbc);
    this.progressBarTime.setVisible(false);
    gbc.weightx = 10000;
    this.progressBarReplacement = Box.createHorizontalGlue();
    bottomPanel.add(this.progressBarReplacement, gbc);

    this.add(bottomPanel, BorderLayout.SOUTH);

    final JPanel controlPanel = new JPanel(new GridLayout(3, 3));

    final Font defaultButtonFont = UIManager.getFont("Button.font");

    this.buttonSine = new ControlButton("w");
    this.buttonSine.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));
    this.buttonSquare = new ControlButton("Q");
    this.buttonSquare.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));
    this.buttonTriangle = new ControlButton("T");
    this.buttonTriangle.setFont(
        GuiUtils.FONT_ADSR.deriveFont(defaultButtonFont.getStyle(), defaultButtonFont.getSize2D()));

    final ButtonGroup buttonGroupSignal = new ButtonGroup();
    buttonGroupSignal.add(this.buttonSine);
    buttonGroupSignal.add(this.buttonSquare);
    buttonGroupSignal.add(this.buttonTriangle);

    this.button13700 = new ControlButton("13700 Hz");
    this.button15500 = new ControlButton("15500 Hz");
    this.button17125 = new ControlButton("17125 Hz");
    final ButtonGroup buttonGroupCarrier = new ButtonGroup();
    buttonGroupCarrier.add(this.button13700);
    buttonGroupCarrier.add(this.button15500);
    buttonGroupCarrier.add(this.button17125);

    this.button44100 = new ControlButton("44.1 KHz");
    this.button48000 = new ControlButton("48 KHz");
    this.button96000 = new ControlButton("96 KHz");

    final ButtonGroup buttonGroupSampleRate = new ButtonGroup();
    buttonGroupSampleRate.add(this.button44100);
    buttonGroupSampleRate.add(this.button48000);
    buttonGroupSampleRate.add(this.button96000);

    controlPanel.add(this.buttonSine);
    controlPanel.add(this.buttonSquare);
    controlPanel.add(this.buttonTriangle);

    controlPanel.add(this.button13700);
    controlPanel.add(this.button15500);
    controlPanel.add(this.button17125);

    controlPanel.add(this.button44100);
    controlPanel.add(this.button48000);
    controlPanel.add(this.button96000);

    this.add(controlPanel, BorderLayout.EAST);

    this.buttonSine.setSelected(true);
    this.button44100.setSelected(true);
    this.button15500.setSelected(true);


    final Consumer<Boolean> controlEnabler = flag -> {
      this.buttonSine.setEnabled(flag);
      this.buttonSquare.setEnabled(flag);
      this.buttonTriangle.setEnabled(flag);
      this.button44100.setEnabled(flag);
      this.button48000.setEnabled(flag);
      this.button96000.setEnabled(flag);
      this.button13700.setEnabled(flag);
      this.button15500.setEnabled(flag);
      this.button17125.setEnabled(flag);
    };

    this.buttonStartStop.addActionListener(e -> {
      if (this.buttonStartStop.isSelected()) {
        this.progressBarTime.setVisible(true);
        this.progressBarReplacement.setVisible(false);
        controlEnabler.accept(false);
      } else {
        this.progressBarTime.setVisible(false);
        this.progressBarReplacement.setVisible(true);
        controlEnabler.accept(true);
      }
    });
  }

  public TimePanel getTimePanel() {
    return this.timePanel;
  }

}
