package za.ac.cput.ui.doctor.components;

import za.ac.cput.model.domain.Appointment;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class AppointmentDetailsDialog {

    public static void show(Component parent, Appointment appointment) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Appointment Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 420);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(field("Patient", patientName(appointment)));
        content.add(field("Patient Email", appointment.getPatient() != null ? appointment.getPatient().getEmail() : "\u2014"));
        content.add(field("Patient Phone", appointment.getPatient() != null ? appointment.getPatient().getCellPhone() : "\u2014"));
        content.add(field("Date", appointment.getAppointmentDate() != null ? appointment.getAppointmentDate().toString() : "\u2014"));
        content.add(field("Time", appointment.getAppointmentTime() != null ? appointment.getAppointmentTime().toString() : "\u2014"));

        JLabel statusValue = new JLabel(appointment.getConfirmationStatus() != null ? appointment.getConfirmationStatus() : "\u2014");
        statusValue.setFont(FontManager.bodyFont(Font.BOLD, 14));
        statusValue.setForeground(AppTheme.statusColor(appointment.getConfirmationStatus()));
        content.add(labeledRow("Status", statusValue));

        content.add(field("Reason", appointment.getReason() != null && !appointment.getReason().isBlank()
                ? appointment.getReason() : "\u2014"));

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

    private static String patientName(Appointment appt) {
        if (appt.getPatient() == null || appt.getPatient().getName() == null) return "\u2014";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }

    private static JComponent field(String label, String value) {
        JLabel valueLabel = new JLabel(value != null && !value.isBlank() ? value : "\u2014");
        valueLabel.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        valueLabel.setForeground(AppTheme.TEXT_PRIMARY);
        return labeledRow(label, valueLabel);
    }

    private static JComponent labeledRow(String label, JComponent valueComponent) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueComponent.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(valueComponent);
        return block;
    }
}