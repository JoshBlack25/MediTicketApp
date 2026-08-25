package za.ac.cput.ui.patient.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.model.domain.TicketStatus;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.patient.components.ElevatedCard;
import za.ac.cput.ui.patient.components.StatusBadge;
import za.ac.cput.ui.patient.components.WrappingLabel;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;



public class TicketsPage extends JPanel {

    private JPanel listContainer;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    public TicketsPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));

        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);
        listContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        listContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        content.add(listContainer);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Tickets");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Track the status of your consultation tickets.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }



    private void loadData() {
        int patientId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<List<PatientTicket>> result =
                ApiClientProvider.getInstance().patientTickets().findByPatientUserId(patientId);

        List<PatientTicket> tickets = result.isSuccess() ? result.getData() : List.of();
        renderList(tickets);
    }

    private void renderList(List<PatientTicket> tickets) {
        listContainer.removeAll();

        if (tickets.isEmpty()) {
            listContainer.add(emptyState());
            listContainer.revalidate();
            listContainer.repaint();
            return;
        }


        List<PatientTicket> sorted = tickets.stream()
                .sorted(Comparator.comparing(
                        (PatientTicket t) -> t.getTicketCreatedDate() != null ? t.getTicketCreatedDate() : java.time.LocalDateTime.MIN
                ).reversed())
                .toList();

        for (PatientTicket ticket : sorted) {
            listContainer.add(ticketCard(ticket));
            listContainer.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JComponent ticketCard(PatientTicket ticket) {
        ElevatedCard outer = new ElevatedCard(AppTheme.RADIUS_MD);
        outer.setLayout(new BorderLayout());
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Left accent strip colored by status — gives the eye an instant
        // read on ticket state before even reaching the badge on the right.
        JPanel accent = new JPanel();
        accent.setPreferredSize(new Dimension(4, 10));
        accent.setBackground(AppTheme.statusColor(ticket.getCurrentStatus()));
        outer.add(accent, BorderLayout.WEST);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD));

        JPanel topRow = new JPanel(new BorderLayout(AppTheme.SPACE_MD, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel ticketNumber = new JLabel("TICKET #" + ticket.getTicketId());
        ticketNumber.setFont(FontManager.bodyFont(Font.BOLD, 10));
        ticketNumber.setForeground(AppTheme.TEXT_MUTED);

        topRow.add(ticketNumber, BorderLayout.WEST);
        topRow.add(new StatusBadge(ticket.getCurrentStatus()), BorderLayout.EAST);

        String description = ticket.getTicketDescription() != null && !ticket.getTicketDescription().isBlank()
                ? ticket.getTicketDescription() : "No description";
        WrappingLabel descLabel = new WrappingLabel(description, FontManager.bodyFont(Font.BOLD, 15), AppTheme.TEXT_PRIMARY);
        descLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_XS, 0));

        String createdText = ticket.getTicketCreatedDate() != null
                ? "Opened " + ticket.getTicketCreatedDate().format(DATE_FMT) : "Opened —";
        String doctorText = ticket.getAppointment() != null
                && ticket.getAppointment().getDoctor() != null
                && ticket.getAppointment().getDoctor().getName() != null
                ? "  \u2022  Dr. " + ticket.getAppointment().getDoctor().getName().getFullName()
                : "";

        JLabel metaLabel = new JLabel(createdText + doctorText);
        metaLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        metaLabel.setForeground(AppTheme.TEXT_MUTED);
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(topRow);
        body.add(descLabel);
        body.add(metaLabel);

        if (ticket.getStatusHistory() != null && !ticket.getStatusHistory().isEmpty()) {
            body.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
            JSeparator divider = new JSeparator();
            divider.setForeground(AppTheme.DIVIDER);
            divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            body.add(divider);
            body.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
            body.add(statusHistoryRow(ticket.getStatusHistory()));
        }

        outer.add(body, BorderLayout.CENTER);
        return outer;
    }

    private JComponent statusHistoryRow(List<TicketStatus> history) {
        // Oldest first, so it reads left-to-right as a timeline.
        List<TicketStatus> sorted = history.stream()
                .sorted(Comparator.comparing(
                        (TicketStatus s) -> s.getStatusDate() != null ? s.getStatusDate() : java.time.LocalDateTime.MIN
                ))
                .toList();

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < sorted.size(); i++) {
            TicketStatus s = sorted.get(i);
            row.add(statusChip(s.getStatusType()));
            if (i < sorted.size() - 1) {
                JLabel arrow = new JLabel("  \u2192  ");
                arrow.setFont(FontManager.bodyFont(Font.PLAIN, 11));
                arrow.setForeground(AppTheme.TEXT_MUTED);
                row.add(arrow);
            }
        }
        return row;
    }

    private JComponent statusChip(String statusType) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.SURFACE_ALT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 8));

        JLabel dot = new JLabel("\u25CF");
        dot.setFont(FontManager.bodyFont(Font.PLAIN, 8));
        dot.setForeground(AppTheme.statusColor(statusType));

        JLabel text = new JLabel(statusType != null ? statusType.replace("_", " ") : "\u2014");
        text.setFont(FontManager.bodyFont(Font.BOLD, 10));
        text.setForeground(AppTheme.TEXT_SECONDARY);

        chip.add(dot);
        chip.add(text);
        return chip;
    }

    private JComponent emptyState() {
        ElevatedCard card = new ElevatedCard(AppTheme.RADIUS_MD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setBorder(BorderFactory.createCompoundBorder(
                card.getBorder(),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_XL, AppTheme.SPACE_LG, AppTheme.SPACE_XL, AppTheme.SPACE_LG)
        ));

        JLabel icon = new JLabel("\uD83C\uDFAB");
        icon.setFont(FontManager.bodyFont(Font.PLAIN, 28));
        icon.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("You don't have any tickets yet");
        label.setFont(FontManager.bodyFont(Font.BOLD, 15));
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_XS, 0));

        JLabel sub = new JLabel("These are created once a doctor reviews a confirmed appointment.");
        sub.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        sub.setForeground(AppTheme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(icon);
        card.add(label);
        card.add(sub);
        return card;
    }
}