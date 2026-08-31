package za.ac.cput.ui.doctor.components;
//AIDAN BARENDS - 230155639
import za.ac.cput.model.domain.Patient;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class PatientDetailsDialog {

    public static void show(Component parent, Patient patient) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Patient Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(field("Name", fullName(patient)));
        content.add(field("Email", patient.getEmail()));
        content.add(field("Phone", patient.getCellPhone()));
        content.add(field("Date of Birth", patient.getDob() != null ? patient.getDob().toString() : "\u2014"));
        content.add(field("Emergency Contact", patient.getEmergencyContact()));
        content.add(statusField(patient.getAccountStatus()));

        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        JButton close = new JButton("Close");
        close.setFont(FontManager.bodyFont(Font.BOLD, 13));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setAlignmentX(Component.LEFT_ALIGNMENT);
        close.addActionListener(e -> dialog.dispose());
        content.add(close);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static String fullName(Patient patient) {
        if (patient.getName() == null) return "\u2014";
        String first = patient.getName().getFirstName();
        String last = patient.getName().getLastName();
        String name = (first != null ? first : "") + " " + (last != null ? last : "");
        return name.isBlank() ? "\u2014" : name;
    }

    private static JComponent field(String label, String value) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueComp = new JLabel(value != null && !value.isBlank() ? value : "\u2014");
        valueComp.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        valueComp.setForeground(AppTheme.TEXT_PRIMARY);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(valueComp);
        return block;
    }

    private static JComponent statusField(String status) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComp = new JLabel("Account Status");
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel value = new JLabel(status != null ? status : "\u2014");
        value.setFont(FontManager.bodyFont(Font.BOLD, 13));
        value.setForeground(AppTheme.statusColor(status));
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(value);
        return block;
    }
}