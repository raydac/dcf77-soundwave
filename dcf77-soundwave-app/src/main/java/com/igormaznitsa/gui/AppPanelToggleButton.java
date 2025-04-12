package com.igormaznitsa.gui;

import javax.swing.JToggleButton;

public class AppPanelToggleButton extends JToggleButton {

  public AppPanelToggleButton(final String text) {
    super(text);
    this.setFocusPainted(false);
    this.setFocusable(false);
  }
}
