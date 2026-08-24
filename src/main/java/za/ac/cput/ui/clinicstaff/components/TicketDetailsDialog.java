package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.model.domain.TicketStatus;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Modal detail view for a single ticket. Shows patient/doctor/appointment
 * context, full status history, and — only when the ticket is RESOLVED and
 * no Payment row exists yet for its appointment — a "Generate Payment
 * Request" action.
 *
 * Payment confirmation ("Mark as Paid") depends on payment method: EFT is
 * settled entirely by the patient via self-checkout (their own Payments
 * page); MEDICAL_AID is claimed by the patient from their own Payments
 * page but stays PENDING until the scheme authorises the claim, so staff
 * confirm it here once approved; CASH and CARD are collected in person,
 * so staff confirm those here once received.
 */
public class TicketDetailsDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    public static void show(Component parent, PatientTicket ticket, Payment existingPayment, Runnable onChanged) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Ticket #TK-" + String.format("%03d", ticket.getTicketId()),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(480, 560);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(fieldBlock("Patient", patientName(ticket)));
        content.add(fieldBlock("Doctor", doctorName(ticket)));
        content.add(fieldBlock("Appointment",
                ticket.getAppointment() != null && ticket.getAppointment().getAppointmentDate() != null
                        ? ticket.getAppointment().getAppointmentDate() + " " +
                        (ticket.getAppointment().getAppointmentTime() != null ? ticket.getAppointment().getAppointmentTime() : "")
                        : "—"));

        JLabel statusValue = new JLabel(statusBadgeText(ticket.getCurrentStatus()));
        statusValue.setFont(FontManager.bodyFont(Font.BOLD, 13));
        statusValue.setForeground(AppTheme.statusColor(ticket.getCurrentStatus()));
        content.add(labeledRow("Ticket Status", statusValue));

        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(sectionTitle("Status History"));
        content.add(buildHistoryList(ticket));

        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        content.add(sectionTitle("Payment"));

        if (existingPayment != null) {
            content.add(fieldBlock("Amount", "R" + formatAmount(existingPayment.getPaymentAmount())));
            content.add(fieldBlock("Method", existingPayment.getPaymentMethod() != null ? existingPayment.getPaymentMethod() : "—"));
            JLabel payStatus = new JLabel(existingPayment.getPaymentStatus());
            payStatus.setFont(FontManager.bodyFont(Font.BOLD, 13));
            payStatus.setForeground(AppTheme.statusColor(existingPayment.getPaymentStatus()));
            content.add(labeledRow("Status", payStatus));

            boolean isEft = "EFT".equals(existingPayment.getPaymentMethod());
            boolean isMedicalAid = "MEDICAL_AID".equals(existingPayment.getPaymentMethod());
            boolean isPending = "PENDING".equals(existingPayment.getPaymentStatus());

            if (isPending && isEft) {
                JLabel waiting = new JLabel("<html><i>Waiting for the patient to complete payment on their dashboard.</i></html>");
                waiting.setFont(FontManager.bodyFont(Font.PLAIN, 12));
                waiting.setForeground(AppTheme.TEXT_MUTED);
                waiting.setAlignmentX(Component.LEFT_ALIGNMENT);
                waiting.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));
                content.add(waiting);
            } else if (isPending) {
                JLabel note = new JLabel(isMedicalAid
                        ? "<html><i>Awaiting medical aid authorisation — confirm once approved.</i></html>"
                        : "<html><i>Collected in person — confirm once received.</i></html>");
                note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
                note.setForeground(AppTheme.TEXT_MUTED);
                note.setAlignmentX(Component.LEFT_ALIGNMENT);
                note.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_SM, 0));
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
                markPaidButton.addActionListener(e -> markAsPaid(dialog, parent, existingPayment, onChanged));
                content.add(markPaidButton);
            } else if ("FAILED".equals(existingPayment.getPaymentStatus())) {
                JLabel note = new JLabel("<html><i>This payment attempt failed.</i></html>");
                note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
                note.setForeground(AppTheme.STATUS_DANGER);
                note.setAlignmentX(Component.LEFT_ALIGNMENT);
                note.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));
                content.add(note);
            } else if ("REFUNDED".equals(existingPayment.getPaymentStatus())) {
                JLabel note = new JLabel("<html><i>This payment has been refunded.</i></html>");
                note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
                note.setForeground(AppTheme.TEXT_MUTED);
                note.setAlignmentX(Component.LEFT_ALIGNMENT);
                note.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));
                content.add(note);
            }
        } else if ("RESOLVED".equals(ticket.getCurrentStatus())) {
            JLabel noPayment = new JLabel("No payment request has been generated yet.");
            noPayment.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            noPayment.setForeground(AppTheme.TEXT_SECONDARY);
            noPayment.setAlignmentX(Component.LEFT_ALIGNMENT);
            noPayment.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));
            content.add(noPayment);

            JButton generateButton = new JButton("Generate Payment Request");
            generateButton.setFont(FontManager.bodyFont(Font.BOLD, 13));
            generateButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
            generateButton.setBackground(AppTheme.PRIMARY);
            generateButton.setFocusPainted(false);
            generateButton.setBorderPainted(false);
            generateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            generateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            generateButton.addActionListener(e -> {
                dialog.dispose();
                GeneratePaymentDialog.show(parent, ticket, onChanged);
            });
            content.add(generateButton);
        } else {
            JLabel notReady = new JLabel("Payment becomes available once the ticket is resolved.");
            notReady.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            notReady.setForeground(AppTheme.TEXT_MUTED);
            notReady.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(notReady);
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        dialog.setContentPane(scroll);
        dialog.setVisible(true);
    }

    // Flipping to PAID triggers PaymentService.closeTicketIfPaid() server-side
    // automatically — no separate "close ticket" call needed here.
    private static void markAsPaid(JDialog dialog, Component parent, Payment payment, Runnable onChanged) {
        String amount = formatAmount(payment.getPaymentAmount());
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

    private static String formatAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP).toString() : "0.00";
    }

    private static String patientName(PatientTicket ticket) {
        if (ticket.getPatient() == null || ticket.getPatient().getName() == null) return "—";
        return fullName(ticket.getPatient().getName().getFirstName(), ticket.getPatient().getName().getLastName());
    }

    private static String doctorName(PatientTicket ticket) {
        if (ticket.getAppointment() == null || ticket.getAppointment().getDoctor() == null
                || ticket.getAppointment().getDoctor().getName() == null) return "—";
        return "Dr. " + fullName(ticket.getAppointment().getDoctor().getName().getFirstName(),
                ticket.getAppointment().getDoctor().getName().getLastName());
    }

    private static String fullName(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null) sb.append(first);
        if (last != null) sb.append(" ").append(last);
        return sb.length() > 0 ? sb.toString().trim() : "—";
    }

    private static String statusBadgeText(String status) {
        return status != null ? status.replace("_", " ") : "—";
    }

    private static JComponent buildHistoryList(PatientTicket ticket) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (ticket.getStatusHistory() == null || ticket.getStatusHistory().isEmpty()) {
            JLabel none = new JLabel("No history recorded.");
            none.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            none.setForeground(AppTheme.TEXT_MUTED);
            list.add(none);
            return list;
        }

        for (TicketStatus status : ticket.getStatusHistory()) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

            JLabel statusLabel = new JLabel(statusBadgeText(status.getStatusType()));
            statusLabel.setFont(FontManager.bodyFont(Font.BOLD, 12));
            statusLabel.setForeground(AppTheme.statusColor(status.getStatusType()));

            JLabel dateLabel = new JLabel(status.getStatusDate() != null ? status.getStatusDate().format(DATE_FMT) : "");
            dateLabel.setFont(FontManager.bodyFont(Font.PLAIN, 11));
            dateLabel.setForeground(AppTheme.TEXT_MUTED);

            row.add(statusLabel, BorderLayout.WEST);
            row.add(dateLabel, BorderLayout.EAST);
            list.add(row);

            if (status.getNotes() != null && !status.getNotes().isBlank()) {
                JLabel notes = new JLabel("<html><i>" + status.getNotes() + "</i></html>");
                notes.setFont(FontManager.bodyFont(Font.PLAIN, 11));
                notes.setForeground(AppTheme.TEXT_SECONDARY);
                notes.setAlignmentX(Component.LEFT_ALIGNMENT);
                notes.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
                list.add(notes);
            }
        }
        return list;
    }

    private static JComponent fieldBlock(String label, String value) {
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

    private static JComponent sectionTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(FontManager.bodyFont(Font.BOLD, 14));
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_XS, 0));
        return label;
    }
}