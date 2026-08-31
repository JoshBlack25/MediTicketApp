package za.ac.cput.ui.doctor.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.model.domain.TicketStatus;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;


public class TicketDetailsDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    public static void show(Component parent, PatientTicket ticket, Runnable onChanged) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Ticket #TK-" + String.format("%03d", ticket.getTicketId()),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(460, 520);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(fieldBlock("Patient", patientName(ticket)));
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

        String status = ticket.getCurrentStatus();
        if ("OPEN".equals(status)) {
            JButton start = new JButton("Start Consultation");
            start.setFont(FontManager.bodyFont(Font.BOLD, 13));
            start.setForeground(AppTheme.TEXT_ON_PRIMARY);
            start.setBackground(AppTheme.PRIMARY);
            start.setFocusPainted(false);
            start.setBorderPainted(false);
            start.setAlignmentX(Component.LEFT_ALIGNMENT);
            start.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            start.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            start.addActionListener(e -> startConsultation(dialog, parent, ticket, onChanged));
            content.add(start);
        } else if ("IN_PROGRESS".equals(status)) {
            JButton complete = new JButton("Complete Consultation");
            complete.setFont(FontManager.bodyFont(Font.BOLD, 13));
            complete.setForeground(AppTheme.TEXT_ON_PRIMARY);
            complete.setBackground(AppTheme.STATUS_SUCCESS);
            complete.setFocusPainted(false);
            complete.setBorderPainted(false);
            complete.setAlignmentX(Component.LEFT_ALIGNMENT);
            complete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            complete.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            complete.addActionListener(e -> {
                dialog.dispose();
                CompleteConsultationDialog.show(parent, ticket, onChanged);
            });
            content.add(complete);
        } else if ("RESOLVED".equals(status)) {
            JLabel note = new JLabel("<html><i>Consultation complete. This ticket is now with clinic staff for payment processing.</i></html>");
            note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            note.setForeground(AppTheme.TEXT_MUTED);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(note);
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        dialog.setContentPane(scroll);
        dialog.setVisible(true);
    }

    private static void startConsultation(JDialog dialog, Component parent, PatientTicket ticket, Runnable onChanged) {
        BaseApiClient.ApiResult<PatientTicket> result = ApiClientProvider.getInstance()
                .patientTickets().progressStatus(ticket.getTicketId(), "IN_PROGRESS", null);

        if (result.isSuccess()) {
            dialog.dispose();
            AppDialog.show(parent, "Consultation Started",
                    "The ticket is now in progress.", AppDialog.Type.SUCCESS);
            if (onChanged != null) onChanged.run();
        } else {
            AppDialog.show(parent, "Unable to Start",
                    result.getMessage() != null ? result.getMessage() : "Something went wrong.", AppDialog.Type.ERROR);
        }
    }

    private static String patientName(PatientTicket ticket) {
        if (ticket.getPatient() == null || ticket.getPatient().getName() == null) return "—";
        String first = ticket.getPatient().getName().getFirstName();
        String last = ticket.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
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