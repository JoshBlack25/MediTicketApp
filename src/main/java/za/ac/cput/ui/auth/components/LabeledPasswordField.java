package za.ac.cput.ui.auth.components;

import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LabeledPasswordField extends JPanel {

    private final JPasswordField field = new JPasswordField();
    private final JLabel forgotLink = new JLabel("Forgot password?");
    private final JButton toggle;
    private boolean visible = false;
    private final char defaultEchoChar;

    public LabeledPasswordField(String labelText) {
        setLayout(new BorderLayout(0, 6));
        setOpaque(false);

        // ===== Top row: label + forgot password link =====
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(FontManager.bodyFont(Font.BOLD, 13));
        label.setForeground(AppTheme.TEXT_PRIMARY);

        forgotLink.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        forgotLink.setForeground(AppTheme.PRIMARY);
        forgotLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        topRow.add(label, BorderLayout.WEST);
        topRow.add(forgotLink, BorderLayout.EAST);

        // ===== Password field =====
        field.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 42));
        field.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        defaultEchoChar = field.getEchoChar();

        // ===== Show/Hide toggle button =====
        toggle = new JButton("Show");
        toggle.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        toggle.setForeground(AppTheme.TEXT_SECONDARY);
        toggle.setBorderPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> {
            visible = !visible;
            field.setEchoChar(visible ? (char) 0 : defaultEchoChar);
            toggle.setText(visible ? "Hide" : "Show");
        });

        // ===== Field row: password field + toggle button, bordered together =====
        JPanel fieldRow = new JPanel(new BorderLayout());
        fieldRow.setOpaque(false);
        fieldRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(2, 12, 2, 6)
        ));
        fieldRow.add(field, BorderLayout.CENTER);
        fieldRow.add(toggle, BorderLayout.EAST);

        add(topRow, BorderLayout.NORTH);
        add(fieldRow, BorderLayout.CENTER);
    }

    public char[] getPassword() { return field.getPassword(); }
    public JPasswordField getField() { return field; }

    public void onForgotPasswordClick(Runnable action) {
        forgotLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { action.run(); }
        });
    }

    /** Clears the field's text and resets visibility back to hidden. */
    public void clear() {
        field.setText("");
        visible = false;
        field.setEchoChar(defaultEchoChar);
        toggle.setText("Show");
    }
}