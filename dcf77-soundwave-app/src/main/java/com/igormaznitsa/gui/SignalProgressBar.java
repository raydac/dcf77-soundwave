package com.igormaznitsa.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.Timer;

public class SignalProgressBar extends JPanel {
  private static final int SPEED = 2;
  private static final int TIMER_DELAY = 30;
  private static final float MAX_STROKE_WIDTH = 2.6f;
  private static final float MIN_STROKE_WIDTH = 0.7f;
  private static final SignalColorPreset ACTIVE_SIGNAL_COLOR = SignalColorPreset.AMBER;
  private final AtomicReference<Timer> timerRef = new AtomicReference<>();
  private int offset = 0;

  public SignalProgressBar() {
    super();
    this.setOpaque(false);
    this.setForeground(ACTIVE_SIGNAL_COLOR.waveColor);

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
    final Timer timer = new Timer(TIMER_DELAY, a -> {
      this.offset += SPEED;
      final int spacing = Math.max(this.getHeight() / 4, 14);
      if (this.offset >= spacing) {
        this.offset -= spacing;
      }
      this.repaint();
    });
    final Timer old = this.timerRef.getAndSet(timer);
    if (old != null) {
      old.stop();
    }
    timer.start();
  }

  private void stopAnimation() {
    final Timer old = this.timerRef.getAndSet(null);
    if (old != null) {
      old.stop();
    }
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
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
    if (width <= 0 || height <= 0) {
      return;
    }

    final int sourceX = -Math.max(height / 2, 20);
    final int sourceY = height / 2;
    final int ringGap = Math.max(height / 4, 14);
    final int maxRadius = width - sourceX;
    final int rings = Math.max(8, maxRadius / ringGap + 3);
    final Color foregroundColor = ACTIVE_SIGNAL_COLOR.waveColor;
    final int r = foregroundColor.getRed();
    final int g = foregroundColor.getGreen();
    final int b = foregroundColor.getBlue();

    for (int i = 0; i < rings; i++) {
      final int radius = this.offset + i * ringGap;
      if (radius <= 0) {
        continue;
      }

      final int rightEdge = sourceX + radius;
      final float fadeAtRight = this.calculateRightEdgeFade(rightEdge, width);
      final int alpha = Math.max(14, Math.round((225 - i * 8) * fadeAtRight));
      g2d.setStroke(new BasicStroke(this.calculateStrokeWidth(rightEdge, width)));
      g2d.setColor(new Color(r, g, b, Math.min(255, alpha)));
      g2d.drawOval(sourceX - radius, sourceY - radius, radius * 2, radius * 2);
    }

    g2d.setColor(ACTIVE_SIGNAL_COLOR.transmitterHighlightColor);
    g2d.fillOval(sourceX - 2, sourceY - 2, 4, 4);
    g2d.setColor(ACTIVE_SIGNAL_COLOR.transmitterCoreColor);
    g2d.fillOval(sourceX - 4, sourceY - 4, 8, 8);
  }

  private float calculateRightEdgeFade(final int rightEdge, final int width) {
    final float fadeStart = 0.0f;
    final float minVisibleAlphaFactor = 0.06f;
    if (rightEdge <= fadeStart) {
      return 1.0f;
    }
    if (rightEdge >= width) {
      return minVisibleAlphaFactor;
    }
    final float linear = (width - rightEdge) / (width - fadeStart);
    return minVisibleAlphaFactor + (1.0f - minVisibleAlphaFactor) * linear;
  }

  private float calculateStrokeWidth(final int rightEdge, final int width) {
    if (rightEdge <= 0) {
      return MAX_STROKE_WIDTH;
    }
    if (rightEdge >= width) {
      return MIN_STROKE_WIDTH;
    }
    final float normalizedDistance = rightEdge / (float) width;
    return MIN_STROKE_WIDTH + (MAX_STROKE_WIDTH - MIN_STROKE_WIDTH) * (1.0f - normalizedDistance);
  }

  private enum SignalColorPreset {
    AMBER(new Color(178, 110, 22), new Color(255, 241, 214, 200), new Color(178, 110, 22, 220)),
    CYAN(new Color(33, 150, 243), new Color(222, 244, 255, 200), new Color(33, 150, 243, 220)),
    GREEN(new Color(57, 176, 91), new Color(230, 255, 238, 200), new Color(57, 176, 91, 220)),
    RED(new Color(207, 71, 71), new Color(255, 230, 230, 200), new Color(207, 71, 71, 220));

    private final Color waveColor;
    private final Color transmitterHighlightColor;
    private final Color transmitterCoreColor;

    SignalColorPreset(
        final Color waveColor,
        final Color transmitterHighlightColor,
        final Color transmitterCoreColor
    ) {
      this.waveColor = waveColor;
      this.transmitterHighlightColor = transmitterHighlightColor;
      this.transmitterCoreColor = transmitterCoreColor;
    }
  }
}