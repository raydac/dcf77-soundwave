package com.igormaznitsa.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.HierarchyEvent;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.Timer;

public class RadioSignalIndicator extends JPanel {
  private static final Stroke STROKE = new BasicStroke(0.7f);
  private static final int CIRCLES = 5;
  private static final int SPEED = 2;
  private static final int TIMER_DELAY = 30;
  private final AtomicReference<Timer> timerRef = new AtomicReference<>();
  private int offset = 0;

  public RadioSignalIndicator() {
    super();
    this.setOpaque(false);
    this.setForeground(Color.ORANGE.darker().darker());

    this.addHierarchyListener(e -> {
      if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED
          && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
        if (this.isVisible()) {
          this.startAnimation();
        } else {
          this.stopAnimation();
        }
      }
    });
  }

  private void startAnimation() {
    Timer timer = new Timer(TIMER_DELAY, a -> {
      offset += SPEED;
      int spacing = RadioSignalIndicator.this.getHeight();
      if (offset >= spacing) {
        offset -= spacing;
      }
      repaint();
    });
    final Timer old = timerRef.getAndSet(timer);
    if (old != null) {
      old.stop();
    }
    timer.start();
  }

  private void stopAnimation() {
    final Timer old = timerRef.getAndSet(null);
    if (old != null) {
      old.stop();
    }
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2d = (Graphics2D) g;
    g2d.setComposite(AlphaComposite.SrcOver);
    this.drawSignals(g2d);
  }

  private void drawSignals(final Graphics2D g2d) {
    g2d.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
    );

    final int width = this.getWidth();
    final int height = this.getHeight();
    final int centerY = height / 2;
    final float signalRadius = (float) height / CIRCLES;
    final float alphaStep = (float) 240 / CIRCLES;

    final int maxX = width + height;

    final Color foregroundColor = Objects.requireNonNullElse(this.getForeground(), Color.GREEN);
    final int r = foregroundColor.getRed();
    final int g = foregroundColor.getGreen();
    final int b = foregroundColor.getBlue();

    g2d.setStroke(STROKE);
    for (int x = offset - height; x < maxX; x += height) {
      for (int i = 0; i < CIRCLES; i++) {
        final int radius = Math.round(signalRadius * (i + 1));
        final int alpha = Math.round(255 - i * alphaStep);
        g2d.setColor(new Color(r, g, b, Math.max(alpha, 0)));
        g2d.drawOval(x - radius, centerY - radius, radius * 2, radius * 2);
      }
      g2d.setColor(new Color(r, g, b, 130));
      g2d.fillOval(x - 3, centerY - 3, 6, 6);
    }
  }
}