package za.ac.cput.ui.patient.components;

import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;

public class SummaryCard extends ElevatedCard {

    private final JLabel valueLabel;

    public SummaryCard(String title, String initialValue, Color accent) {
        super(AppTheme.RADIUS_MD);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                getBorder(),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD)
        ));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 11));
        titleLabel.setForeground(AppTheme.TEXT_MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(FontManager.headlineFont(Font.BOLD, 32));
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        add(titleLabel);
        add(valueLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}