package za.ac.cput.ui.patient.components;

import za.ac.cput.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Rounded, softly-shadowed card container — the visual building block for
 * ticket cards, notification cards, and the dashboard's summary panels.
 * Replaces the old pattern of a plain JPanel with a square 1px border,
 * which is what made those lists look flat and "unfinished". Content is
 * added exactly like a normal panel; the shadow/rounding is self-contained
 * in paintComponent so callers don't need to think about it.
 */
public class ElevatedCard extends JPanel {

    private static final int SHADOW_SIZE = 5;

    private final int radius;
    private boolean drawBorder = true;

    public ElevatedCard() {
        this(AppTheme.RADIUS_MD);
    }

    public ElevatedCard(int radius) {
        this.radius = radius;
        setOpaque(false);
        setBackground(AppTheme.SURFACE);
        // Reserve room on the bottom/right for the drop shadow so it never
        // gets clipped by the component's own bounds.
        setBorder(BorderFactory.createEmptyBorder(0, 0, SHADOW_SIZE + 1, SHADOW_SIZE + 1));
    }

    /** Colored cards (e.g. the primary-tinted "next appointment" card) skip the hairline border. */
    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth() - SHADOW_SIZE - 1;
        int h = getHeight() - SHADOW_SIZE - 1;
        if (w <= 0 || h <= 0) {
            super.paintComponent(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Soft shadow: a handful of stacked, increasingly-transparent
        // rounded rects offset slightly down-right of the card.
        for (int i = SHADOW_SIZE; i >= 1; i--) {
            int alpha = 3 + (SHADOW_SIZE - i) * 2;
            g2.setColor(new Color(20, 30, 32, alpha));
            g2.fillRoundRect(i / 2, i, w, h, radius, radius);
        }

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w, h, radius, radius);

        if (drawBorder) {
            g2.setColor(AppTheme.BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}