package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;

public class ActivityRow extends JPanel {

    public ActivityRow(String label, String value, Color valueColor) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        labelComp.setForeground(AppTheme.TEXT_SECONDARY);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(FontManager.bodyFont(Font.BOLD, 13));
        valueComp.setForeground(valueColor != null ? valueColor : AppTheme.TEXT_PRIMARY);

        add(labelComp, BorderLayout.WEST);
        add(valueComp, BorderLayout.EAST);
    }

    public static JComponent textRow(String text) {
        JLabel label = new JLabel("• " + text);
        label.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}