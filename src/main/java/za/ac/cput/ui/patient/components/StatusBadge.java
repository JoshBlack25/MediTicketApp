package za.ac.cput.ui.patient.components;

import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class StatusBadge extends JPanel {

    private final JLabel textLabel;
    private Color backgroundColor;

    public StatusBadge(String status) {
        setOpaque(false); // we paint our own rounded background below
        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        textLabel = new JLabel();
        textLabel.setFont(FontManager.bodyFont(Font.BOLD, 11));
        add(textLabel);

        setStatus(status);
    }

    public void setStatus(String status) {
        backgroundColor = AppTheme.statusBackground(status);
        textLabel.setForeground(AppTheme.statusColor(status));
        textLabel.setText(toTitleCase(status));
        revalidate();
        repaint();
    }

    private String toTitleCase(String status) {
        if (status == null || status.isBlank()) return "—";
        String[] words = status.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(backgroundColor);

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}