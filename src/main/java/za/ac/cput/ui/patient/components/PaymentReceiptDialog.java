/*
 PaymentReceiptDialog.java

 Patient Payment Receipt Dialog — read-only view for a PAID or REFUNDED
 payment. No actions available; this is purely informational.

 Author: Abdullahi Farah (230971091)
*/
package za.ac.cput.ui.patient.components;

import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public class PaymentReceiptDialog {

    public static void show(Component parent, Payment payment) {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(parent),
                "Payment Receipt",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(AppTheme.SURFACE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader(payment));
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildDivider());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        content.add(buildRow("Doctor", doctorName(payment)));
        content.add(buildRow("Appointment Date", appointmentDate(payment)));
        content.add(buildRow("Payment Date", paymentDate(payment)));
        content.add(buildRow("Method", methodDisplayName(payment.getPaymentMethod())));
        content.add(buildRow("Payment ID", String.valueOf(payment.getPaymentId())));

        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        content.add(buildDivider());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        content.add(buildAmountRow(payment));

        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildCloseButton(dialog));

        dialog.add(content, BorderLayout.CENTER);
        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private static JComponent buildHeader(Payment payment) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Payment Receipt");
        title.setFont(FontManager.headlineFont(Font.BOLD, 20));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean isPaid = "PAID".equals(payment.getPaymentStatus());
        JLabel status = new JLabel(statusLabel(payment.getPaymentStatus()));
        status.setFont(FontManager.bodyFont(Font.BOLD, 13));
        status.setForeground(isPaid ? AppTheme.STATUS_SUCCESS : AppTheme.STATUS_NEUTRAL);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(status);
        return panel;
    }

    private static JComponent buildDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(AppTheme.DIVIDER);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private static JComponent buildRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        labelComp.setForeground(AppTheme.TEXT_SECONDARY);

        JLabel valueComp = new JLabel(value != null ? value : "—");
        valueComp.setFont(FontManager.bodyFont(Font.BOLD, 13));
        valueComp.setForeground(AppTheme.TEXT_PRIMARY);
        valueComp.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(labelComp, BorderLayout.WEST);
        row.add(valueComp, BorderLayout.EAST);
        return row;
    }

    private static JComponent buildAmountRow(Payment payment) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel label = new JLabel("Amount");
        label.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        label.setForeground(AppTheme.TEXT_SECONDARY);

        String amountText = payment.getPaymentAmount() != null
                ? "R" + payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP)
                : "—";
        JLabel amount = new JLabel(amountText);
        amount.setFont(FontManager.headlineFont(Font.BOLD, 20));
        amount.setForeground(AppTheme.TEXT_PRIMARY);
        amount.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(label, BorderLayout.WEST);
        row.add(amount, BorderLayout.EAST);
        return row;
    }

    private static JComponent buildCloseButton(JDialog dialog) {
        JButton close = new JButton("Close");
        close.setFont(FontManager.bodyFont(Font.BOLD, 13));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setBackground(AppTheme.PRIMARY);
        close.setForeground(AppTheme.TEXT_ON_PRIMARY);
        close.setAlignmentX(Component.CENTER_ALIGNMENT);
        close.addActionListener(e -> dialog.dispose());

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(close);
        return wrapper;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private static String statusLabel(String status) {
        if ("PAID".equals(status)) return "Paid in full";
        if ("REFUNDED".equals(status)) return "Refunded";
        return status != null ? status : "—";
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
        return appt.getAppointmentDate().toString();
    }

    private static String paymentDate(Payment payment) {
        if (payment.getPaymentDate() == null) return "—";
        return payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    private static String methodDisplayName(String method) {
        if (method == null) return "—";
        return switch (method) {
            case "CASH" -> "Cash";
            case "CARD" -> "Card";
            case "EFT" -> "EFT";
            case "MEDICAL_AID" -> "Medical Aid";
            default -> method;
        };
    }
}