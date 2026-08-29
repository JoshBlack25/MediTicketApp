package za.ac.cput.ui.patient.components;

import za.ac.cput.model.domain.Appointment;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class AppointmentDetailsDialog {

    public static void show(Component parent, Appointment appointment) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Appointment Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(field("Doctor", doctorName(appointment)));
        content.add(field("Date", appointment.getAppointmentDate() != null ? appointment.getAppointmentDate().toString() : "—"));
        content.add(field("Time", appointment.getAppointmentTime() != null ? appointment.getAppointmentTime().toString() : "—"));

        JLabel statusValue = new JLabel(appointment.getConfirmationStatus() != null ? appointment.getConfirmationStatus() : "—");
        statusValue.setFont(FontManager.bodyFont(Font.BOLD, 14));
        statusValue.setForeground(AppTheme.statusColor(appointment.getConfirmationStatus()));
        content.add(labeledRow("Status", statusValue));

        content.add(field("Reason", appointment.getReason() != null && !appointment.getReason().isBlank()
                ? appointment.getReason() : "—"));

        if ("REJECTED".equals(appointment.getConfirmationStatus())) {
            JLabel rejectedNote = new JLabel("<html><i>This appointment request could not be approved. "
                    + "Please contact the clinic if you have questions, or submit a new request.</i></html>");
            rejectedNote.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            rejectedNote.setForeground(AppTheme.STATUS_DANGER);
            rejectedNote.setAlignmentX(Component.LEFT_ALIGNMENT);
            rejectedNote.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));
            content.add(rejectedNote);
        }

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

    private static String doctorName(Appointment appointment) {
        if (appointment.getDoctor() == null || appointment.getDoctor().getName() == null) return "Not yet assigned";
        String last = appointment.getDoctor().getName().getLastName();
        return "Dr. " + (last != null ? last : "—");
    }

    private static JComponent field(String label, String value) {
        JLabel valueLabel = new JLabel(value);
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