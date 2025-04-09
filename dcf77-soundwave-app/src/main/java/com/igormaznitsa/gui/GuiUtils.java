package com.igormaznitsa.gui;

import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.SystemUtils;

public final class GuiUtils {
  public static final Font FONT_DIGITAL;
  public static final Font FONT_ADSR;
  public static final Font FONT_SOSA;

  static {
    try {
      final GraphicsEnvironment graphicsEnvironment =
          GraphicsEnvironment.getLocalGraphicsEnvironment();
      FONT_DIGITAL =
          Font.createFont(Font.TRUETYPE_FONT, requireNonNull(GuiUtils.class.getResourceAsStream(
              "/fonts/digital-7 (mono).ttf")));
      FONT_SOSA =
          Font.createFont(Font.TRUETYPE_FONT, requireNonNull(GuiUtils.class.getResourceAsStream(
              "/fonts/SosaRegular-MJmx.ttf")));
      FONT_ADSR =
          Font.createFont(Font.TRUETYPE_FONT, requireNonNull(GuiUtils.class.getResourceAsStream(
              "/fonts/ADSR.TTF")));
      graphicsEnvironment.registerFont(FONT_DIGITAL);
      graphicsEnvironment.registerFont(FONT_SOSA);
      graphicsEnvironment.registerFont(FONT_ADSR);
    } catch (Exception ex) {
      throw new Error("Can't init class for error", ex);
    }
  }

  private GuiUtils() {

  }

  private static void showURL(final URL url) {
    showURLExternal(url);
  }

  private static void showURLExternal(final URL url) {
    if (Desktop.isDesktopSupported()) {
      final Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
          desktop.browse(url.toURI());
        } catch (Exception x) {
          // ignore
        }
      } else if (SystemUtils.IS_OS_LINUX) {
        final Runtime runtime = Runtime.getRuntime();
        try {
          runtime.exec("xdg-open " + url);
        } catch (IOException e) {
          // ignore
        }
      } else if (SystemUtils.IS_OS_MAC) {
        final Runtime runtime = Runtime.getRuntime();
        try {
          runtime.exec("open " + url);
        } catch (IOException e) {
          // ignore
        }
      }
    }

  }

  public static boolean browseURI(final URI uri,
                                  final boolean preferInsideBrowserIfPossible) {
    try {
      if (preferInsideBrowserIfPossible) {
        showURL(uri.toURL());
      } else {
        showURLExternal(uri.toURL());
      }
      return true;
    } catch (MalformedURLException ex) {
      return false;
    }
  }

  public static void makeOwningDialogResizable(final Component component,
                                               final Runnable... extraActions) {
    final HierarchyListener listener = new HierarchyListener() {
      @Override
      public void hierarchyChanged(final HierarchyEvent e) {
        final Window window = SwingUtilities.getWindowAncestor(component);
        if (window instanceof Dialog) {
          final Dialog dialog = (Dialog) window;
          if (!dialog.isResizable()) {
            dialog.setResizable(true);
            component.removeHierarchyListener(this);

            for (final Runnable r : extraActions) {
              r.run();
            }
          }
        }
      }
    };
    component.addHierarchyListener(listener);
  }

  public static ImageIcon loadIcon(final String fileName) {
    return new ImageIcon(requireNonNull(GuiUtils.class.getResource("/icons/" + fileName)));
  }

}
