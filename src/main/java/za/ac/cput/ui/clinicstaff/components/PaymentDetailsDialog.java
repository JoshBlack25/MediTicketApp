package za.ac.cput.ui.clinicstaff.components;
//ABDULLAHI RAAGE FARAH - 230971091
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;


public class PaymentDetailsDialog {

    public static void show(Component parent, Payment payment, Runnable onChanged) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Payment Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 400);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(field("Patient", patientName(payment)));
        content.add(field("Doctor", doctorName(payment)));
        content.add(field("Appointment", appointmentDate(payment)));
        content.add(field("Amount", payment.getPaymentAmount() != null
                ? "R" + payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP) : "—"));
        content.add(field("Method", payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "—"));

        JLabel statusValue = new JLabel(payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "—");
        statusValue.setFont(FontManager.bodyFont(Font.BOLD, 14));
        statusValue.setForeground(AppTheme.statusColor(payment.getPaymentStatus()));
        content.add(labeledRow("Status", statusValue));

        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        boolean isEft = "EFT".equals(payment.getPaymentMethod());
        boolean isMedicalAid = "MEDICAL_AID".equals(payment.getPaymentMethod());
        boolean isPending = "PENDING".equals(payment.getPaymentStatus());

        if (isPending && isEft) {
            JLabel note = new JLabel("<html><i>Waiting for the patient to complete payment on their dashboard.</i></html>");
            note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            note.setForeground(AppTheme.TEXT_MUTED);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(note);
        } else if (isPending) {

            JLabel note = new JLabel(isMedicalAid
                    ? "<html><i>Awaiting medical aid authorisation — confirm once approved.</i></html>"
                    : "<html><i>Collected in person — confirm once received.</i></html>");
            note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            note.setForeground(AppTheme.TEXT_MUTED);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            note.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));
            content.add(note);

            JButton markPaidButton = new JButton("Mark as Paid");
            markPaidButton.setFont(FontManager.bodyFont(Font.BOLD, 13));
            markPaidButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
            markPaidButton.setBackground(AppTheme.STATUS_SUCCESS);
            markPaidButton.setFocusPainted(false);
            markPaidButton.setBorderPainted(false);
            markPaidButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            markPaidButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            markPaidButton.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            markPaidButton.addActionListener(e -> markAsPaid(dialog, parent, payment, onChanged));
            content.add(markPaidButton);
        } else if ("FAILED".equals(payment.getPaymentStatus())) {
            JLabel note = new JLabel("<html><i>This payment attempt failed.</i></html>");
            note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            note.setForeground(AppTheme.STATUS_DANGER);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(note);
        } else if ("REFUNDED".equals(payment.getPaymentStatus())) {
            JLabel note = new JLabel("<html><i>This payment has been refunded.</i></html>");
            note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            note.setForeground(AppTheme.TEXT_MUTED);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(note);
        }

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }


    private static void markAsPaid(JDialog dialog, Component parent, Payment payment, Runnable onChanged) {
        String amount = payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP).toString();
        String prompt = "MEDICAL_AID".equals(payment.getPaymentMethod())
                ? "Confirm that the medical aid claim for R" + amount + " has been approved?"
                : "Confirm that payment of R" + amount + " has been received in person?";

        int confirm = JOptionPane.showConfirmDialog(dialog, prompt,
                "Mark as Paid", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        payment.setPaymentStatus("PAID");
        BaseApiClient.ApiResult<Payment> result = ApiClientProvider.getInstance().payments().update(payment);

        if (result.isSuccess()) {
            dialog.dispose();
            AppDialog.show(parent, "Payment Confirmed",
                    "The payment has been marked as paid, and the ticket has been closed.", AppDialog.Type.SUCCESS);
            if (onChanged != null) onChanged.run();
        } else {
            AppDialog.show(parent, "Unable to Update Payment",
                    result.getMessage() != null ? result.getMessage() : "Something went wrong.", AppDialog.Type.ERROR);
        }
    }

    private static String patientName(Payment payment) {
        Appointment appt = payment.getAppointment();
        if (appt == null || appt.getPatient() == null || appt.getPatient().getName() == null) return "—";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }

    private static String doctorName(Payment payment) {
        Appointment appt = payment.getAppointment();
        if (appt == null || appt.getDoctor() == null || appt.getDoctor().getName() == null) return "—";
        String last = appt.getDoctor().getName().getLastName();
        return "Dr. " + (last != null ? last : "—");
    }

    private static String appointmentDate(Payment payment) {
        Appointment appt = payment.getAppointment();
        if (appt == null || appt.getAppointmentDate() == null) return "—";
        return appt.getAppointmentDate() + " " + (appt.getAppointmentTime() != null ? appt.getAppointmentTime() : "");
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