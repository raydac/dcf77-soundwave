package com.igormaznitsa.gui;

import static java.util.Objects.requireNonNull;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.ImageIcon;

public final class GuiUtils {
  public static final Font FONT_DIGITAL;
  public static final Font FONT_ADSR;

  static {
    try {
      final GraphicsEnvironment graphicsEnvironment =
          GraphicsEnvironment.getLocalGraphicsEnvironment();
      FONT_DIGITAL =
          Font.createFont(Font.TRUETYPE_FONT, requireNonNull(GuiUtils.class.getResourceAsStream(
              "/fonts/digital-7 (mono).ttf")));
      FONT_ADSR =
          Font.createFont(Font.TRUETYPE_FONT, requireNonNull(GuiUtils.class.getResourceAsStream(
              "/fonts/ADSR.TTF")));
      graphicsEnvironment.registerFont(FONT_DIGITAL);
      graphicsEnvironment.registerFont(FONT_ADSR);
    } catch (Exception ex) {
      throw new Error("Can't init class for error", ex);
    }
  }

  private GuiUtils() {

  }

  public static ImageIcon loadIcon(final String fileName) {
    return new ImageIcon(requireNonNull(GuiUtils.class.getResource("/icons/" + fileName)));
  }

}
