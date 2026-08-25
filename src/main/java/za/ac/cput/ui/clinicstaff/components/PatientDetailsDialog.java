package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.model.domain.Patient;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;

/**
 * Read-only view for a single patient. Mirrors StaffDetailsDialog's layout
 * and styling so patient/staff detail popups feel consistent across the
 * admin side. No Edit action here for the same reason StaffDetailsDialog
 * has none yet — no safe update-patient path wired from the admin side.
 */
public class PatientDetailsDialog {

    public static void show(Component parent, Patient patient) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Patient Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildFields(patient));

        JButton close = new JButton("Close");
        close.setFont(FontManager.bodyFont(Font.BOLD, 13));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setAlignmentX(Component.RIGHT_ALIGNMENT);
        close.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));
        close.addActionListener(e -> dialog.dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(close);
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static JComponent buildFields(Patient patient) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(field("Name", fullName(patient)));
        panel.add(field("Email", patient.getEmail()));
        panel.add(field("Phone", patient.getCellPhone()));
        panel.add(field("Date of Birth", patient.getDob() != null ? patient.getDob().toString() : "—"));
        panel.add(field("Date Registered", patient.getDateRegistered() != null ? patient.getDateRegistered().toString() : "—"));
        panel.add(field("Emergency Contact", patient.getEmergencyContact()));
        panel.add(statusField(patient.getAccountStatus()));
        return panel;
    }

    private static String fullName(Patient patient) {
        if (patient.getName() == null) return "—";
        String first = patient.getName().getFirstName();
        String last = patient.getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
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

        JLabel valueComp = new JLabel(value != null && !value.isBlank() ? value : "—");
        valueComp.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        valueComp.setForeground(AppTheme.TEXT_PRIMARY);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(valueComp);
        return block;
    }

    private static JComponent statusField(String status) {
        JLabel value = new JLabel(status != null ? status : "—");
        value.setFont(FontManager.bodyFont(Font.BOLD, 13));
        value.setForeground(AppTheme.statusColor(status));
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComp = new JLabel("Account Status");
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(value);
        return block;
    }
}