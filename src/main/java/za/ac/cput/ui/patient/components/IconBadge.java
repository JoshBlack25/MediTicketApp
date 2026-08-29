package za.ac.cput.ui.patient.components;

import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class IconBadge extends JPanel {

    private static final int SIZE = 38;

    public IconBadge(String emoji, Color background) {
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
        setOpaque(false);
        setBackground(background);
        setLayout(new GridBagLayout());

        JLabel label = new JLabel(emoji);
        label.setFont(FontManager.bodyFont(Font.PLAIN, 16));
        add(label);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillOval(0, 0, SIZE, SIZE);
        g2.dispose();
        super.paintComponent(g);
    }
}