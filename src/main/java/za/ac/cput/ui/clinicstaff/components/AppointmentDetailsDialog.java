package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.model.domain.Appointment;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class AppointmentDetailsDialog {

    public interface ActionCallback {
        void onApprove();
        void onReject();
    }

    public static void show(Component parent, Appointment appointment, ActionCallback callback) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Appointment Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 400);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(field("Patient", patientName(appointment)));
        content.add(field("Doctor", doctorName(appointment)));
        content.add(field("Date", appointment.getAppointmentDate() != null ? appointment.getAppointmentDate().toString() : "—"));
        content.add(field("Time", appointment.getAppointmentTime() != null ? appointment.getAppointmentTime().toString() : "—"));

        JLabel statusValue = new JLabel(appointment.getConfirmationStatus() != null ? appointment.getConfirmationStatus() : "—");
        statusValue.setFont(FontManager.bodyFont(Font.BOLD, 14));
        statusValue.setForeground(AppTheme.statusColor(appointment.getConfirmationStatus()));
        content.add(labeledRow("Status", statusValue));

        content.add(field("Reason", appointment.getReason() != null && !appointment.getReason().isBlank()
                ? appointment.getReason() : "—"));

        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        if ("PENDING".equals(appointment.getConfirmationStatus())) {
            JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
            buttonRow.setOpaque(false);
            buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton reject = new JButton("Reject");
            reject.setFont(FontManager.bodyFont(Font.BOLD, 13));
            reject.setForeground(AppTheme.STATUS_DANGER);
            reject.setBackground(AppTheme.SURFACE);
            reject.setBorder(BorderFactory.createLineBorder(AppTheme.STATUS_DANGER, 1, true));
            reject.setFocusPainted(false);
            reject.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            reject.addActionListener(e -> { dialog.dispose(); callback.onReject(); });

            JButton approve = new JButton("Approve");
            approve.setFont(FontManager.bodyFont(Font.BOLD, 13));
            approve.setForeground(AppTheme.TEXT_ON_PRIMARY);
            approve.setBackground(AppTheme.STATUS_SUCCESS);
            approve.setBorderPainted(false);
            approve.setFocusPainted(false);
            approve.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            approve.addActionListener(e -> { dialog.dispose(); callback.onApprove(); });

            buttonRow.add(reject);
            buttonRow.add(approve);
            content.add(buttonRow);
        } else {
            JLabel noActions = new JLabel("No further action needed at this stage.");
            noActions.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            noActions.setForeground(AppTheme.TEXT_MUTED);
            noActions.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(noActions);
        }

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static String patientName(Appointment appt) {
        if (appt.getPatient() == null || appt.getPatient().getName() == null) return "—";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }

    private static String doctorName(Appointment appt) {
        if (appt.getDoctor() == null || appt.getDoctor().getName() == null) return "Unassigned";
        String last = appt.getDoctor().getName().getLastName();
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