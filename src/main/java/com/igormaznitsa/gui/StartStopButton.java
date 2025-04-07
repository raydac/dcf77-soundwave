package com.igormaznitsa.gui;

import javax.swing.JToggleButton;

public class StartStopButton extends JToggleButton {

  public StartStopButton() {
    super();
    this.setOpaque(false);
    this.setContentAreaFilled(false);
    this.setBorderPainted(false);
    this.setFocusPainted(false);

    this.setRolloverEnabled(false);

    this.setSelectedIcon(GuiUtils.loadIcon("control_stop_blue.png"));
    this.setIcon(GuiUtils.loadIcon("control_play_blue.png"));
  }

}
