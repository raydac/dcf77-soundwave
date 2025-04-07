package com.igormaznitsa.gui;

import javax.swing.JToggleButton;

public class ControlButton extends JToggleButton {

  public ControlButton(final String text) {
    super(text);
    this.setFocusPainted(false);
  }
}
