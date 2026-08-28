package za.ac.cput.ui.patient.components;

import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.model.domain.TicketStatus;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Read-only ticket detail view for patients. No actions live here —
 * payment settlement happens on the patient's own Payments page
 * (see FakeCheckoutDialog), and consultation progress is entirely
 * doctor-driven. This dialog exists purely so patients can see their
 * ticket status history.
 */
public class TicketDetailsDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    public static void show(Component parent, PatientTicket ticket) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Ticket #TK-" + String.format("%03d", ticket.getTicketId()),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(460, 480);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

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

        if ("RESOLVED".equals(ticket.getCurrentStatus())) {
            content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
            JLabel note = new JLabel("<html><i>Your consultation is complete. Check your Payments page "
                    + "to settle any outstanding balance.</i></html>");
            note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            note.setForeground(AppTheme.TEXT_SECONDARY);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(note);
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        dialog.setContentPane(scroll);
        dialog.setVisible(true);
    }

    private static String doctorName(PatientTicket ticket) {
        if (ticket.getAppointment() == null || ticket.getAppointment().getDoctor() == null
                || ticket.getAppointment().getDoctor().getName() == null) return "—";
        String last = ticket.getAppointment().getDoctor().getName().getLastName();
        return "Dr. " + (last != null ? last : "—");
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