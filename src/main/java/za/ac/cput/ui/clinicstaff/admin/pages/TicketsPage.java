package za.ac.cput.ui.clinicstaff.admin.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.clinicstaff.components.TicketDetailsDialog;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TicketsPage extends JPanel {

    private SummaryCard openCard, inProgressCard, resolvedCard, closedCard;
    private JPanel needsAttentionSection;
    private DefaultTableModel tableModel;
    private JTable ticketsTable;

    private List<PatientTicket> allTickets = List.of();
    private Map<Integer, Payment> paymentsByAppointmentId = new HashMap<>();
    private String activeFilter = "ALL";
    private JPanel filterBarContainer;

    public TicketsPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildSummaryCards());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        needsAttentionSection = buildEmptySection();
        content.add(needsAttentionSection);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));

        filterBarContainer = new JPanel(new BorderLayout());
        filterBarContainer.setOpaque(false);
        filterBarContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBarContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
        content.add(filterBarContainer);

        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(buildTable());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel title = new JLabel("Tickets");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Manage patient consultation tickets and payment progress. Click a row to view details.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        textStack.add(title);
        textStack.add(subtitle);

        JButton refreshButton = new JButton("\u27F3 Refresh");
        refreshButton.setFont(FontManager.bodyFont(Font.BOLD, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> loadData());

        header.add(textStack, BorderLayout.WEST);
        header.add(refreshButton, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        openCard = new SummaryCard("Open", "—", AppTheme.PRIMARY);
        inProgressCard = new SummaryCard("In Consultation", "—", AppTheme.STATUS_INFO);
        resolvedCard = new SummaryCard("Ready for Payment", "—", AppTheme.STATUS_WARNING);
        closedCard = new SummaryCard("Closed", "—", AppTheme.STATUS_SUCCESS);

        grid.add(openCard);
        grid.add(inProgressCard);
        grid.add(resolvedCard);
        grid.add(closedCard);
        return grid;
    }

    private JPanel buildEmptySection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"OPEN", "Open"}, {"IN_PROGRESS", "In Consultation"},
                {"RESOLVED", "Ready for Payment"}, {"CLOSED", "Closed"}, {"ESCALATED", "Escalated"}
        };

        for (String[] f : filters) {
            JButton btn = new JButton(f[1]);
            btn.setFont(FontManager.bodyFont(Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBackground(f[0].equals(activeFilter) ? AppTheme.PRIMARY : AppTheme.SURFACE);
            btn.setForeground(f[0].equals(activeFilter) ? AppTheme.TEXT_ON_PRIMARY : AppTheme.TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
            btn.addActionListener(e -> {
                activeFilter = f[0];
                renderTable();
                filterBarContainer.removeAll();
                filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
                filterBarContainer.revalidate();
                filterBarContainer.repaint();
            });
            bar.add(btn);
        }
        return bar;
    }

    private JComponent buildTable() {
        String[] columns = {"Ticket", "Patient", "Doctor", "Appointment", "Status", "Amount", "ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        ticketsTable = new JTable(tableModel);
        ticketsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        ticketsTable.setRowHeight(40);
        ticketsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        ticketsTable.setShowGrid(false);
        ticketsTable.setIntercellSpacing(new Dimension(0, 0));
        ticketsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ticketsTable.getColumnModel().getColumn(6).setMinWidth(0);
        ticketsTable.getColumnModel().getColumn(6).setMaxWidth(0);
        ticketsTable.getColumnModel().getColumn(6).setWidth(0);

        RowClickHelper.makeRowsClickable(ticketsTable, 6, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(ticketsTable);
        scroll.setPreferredSize(new Dimension(0, 360));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int ticketId) {
        PatientTicket ticket = findTicketById(ticketId);
        if (ticket == null) return;

        Payment payment = ticket.getAppointment() != null
                ? paymentsByAppointmentId.get(ticket.getAppointment().getAppointmentId())
                : null;

        TicketDetailsDialog.show(this, ticket, payment, this::loadData);
    }


    private void loadData() {
        BaseApiClient.ApiResult<List<PatientTicket>> ticketResult =
                ApiClientProvider.getInstance().patientTickets().getAll();
        allTickets = ticketResult.isSuccess() ? ticketResult.getData() : List.of();

        BaseApiClient.ApiResult<List<Payment>> paymentResult =
                ApiClientProvider.getInstance().payments().getAll();
        List<Payment> payments = paymentResult.isSuccess() ? paymentResult.getData() : List.of();

        paymentsByAppointmentId = payments.stream()
                .filter(p -> p.getAppointment() != null)
                .collect(Collectors.toMap(p -> p.getAppointment().getAppointmentId(), p -> p, (a, b) -> a));

        updateSummaryCards();
        updateNeedsAttention();
        renderTable();
    }

    private void updateSummaryCards() {
        openCard.setValue(String.valueOf(countByStatus("OPEN")));
        inProgressCard.setValue(String.valueOf(countByStatus("IN_PROGRESS")));
        resolvedCard.setValue(String.valueOf(countByStatus("RESOLVED")));
        closedCard.setValue(String.valueOf(countByStatus("CLOSED")));
    }

    private long countByStatus(String status) {
        return allTickets.stream().filter(t -> status.equals(t.getCurrentStatus())).count();
    }

    private void updateNeedsAttention() {
        needsAttentionSection.removeAll();

        long readyForPayment = countByStatus("RESOLVED");
        long escalated = countByStatus("ESCALATED");

        if (readyForPayment == 0 && escalated == 0) {
            needsAttentionSection.revalidate();
            needsAttentionSection.repaint();
            return;
        }

        JLabel title = new JLabel("Needs Attention");
        title.setFont(FontManager.bodyFont(Font.BOLD, 15));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));
        needsAttentionSection.add(title);

        if (readyForPayment > 0) {
            needsAttentionSection.add(attentionBanner(
                    "\uD83D\uDCB3 " + readyForPayment + " ticket" + (readyForPayment == 1 ? "" : "s") + " ready for payment",
                    "Patients are waiting for payment requests.", "RESOLVED"));
            needsAttentionSection.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        }
        if (escalated > 0) {
            needsAttentionSection.add(attentionBanner(
                    "\uD83D\uDEA9 " + escalated + " ticket" + (escalated == 1 ? "" : "s") + " escalated",
                    "These tickets need immediate review.", "ESCALATED"));
        }

        needsAttentionSection.revalidate();
        needsAttentionSection.repaint();
    }

    private JComponent attentionBanner(String title, String subtitle, String filterOnClick) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(AppTheme.SURFACE_ALT);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_MD, AppTheme.SPACE_SM, AppTheme.SPACE_MD)
        ));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        subtitleLabel.setForeground(AppTheme.TEXT_SECONDARY);

        textStack.add(titleLabel);
        textStack.add(subtitleLabel);

        JButton viewButton = new JButton("View Tickets");
        viewButton.setFont(FontManager.bodyFont(Font.BOLD, 12));
        viewButton.setFocusPainted(false);
        viewButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewButton.addActionListener(e -> {
            activeFilter = filterOnClick;
            renderTable();
            filterBarContainer.removeAll();
            filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
            filterBarContainer.revalidate();
            filterBarContainer.repaint();
        });

        banner.add(textStack, BorderLayout.WEST);
        banner.add(viewButton, BorderLayout.EAST);
        return banner;
    }

    private void renderTable() {
        tableModel.setRowCount(0);

        List<PatientTicket> filtered = "ALL".equals(activeFilter)
                ? allTickets
                : allTickets.stream().filter(t -> activeFilter.equals(t.getCurrentStatus())).collect(Collectors.toList());

        for (PatientTicket ticket : filtered) {
            Payment payment = ticket.getAppointment() != null
                    ? paymentsByAppointmentId.get(ticket.getAppointment().getAppointmentId())
                    : null;

            tableModel.addRow(new Object[]{
                    "TK-" + String.format("%03d", ticket.getTicketId()),
                    patientName(ticket),
                    doctorName(ticket),
                    appointmentDate(ticket),
                    ticket.getCurrentStatus() != null ? ticket.getCurrentStatus().replace("_", " ") : "—",
                    payment != null ? "R" + payment.getPaymentAmount() : "—",
                    ticket.getTicketId()
            });
        }
    }

    private String patientName(PatientTicket ticket) {
        if (ticket.getPatient() == null || ticket.getPatient().getName() == null) return "—";
        String first = ticket.getPatient().getName().getFirstName();
        String last = ticket.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last.charAt(0) + "." : "");
    }

    private String doctorName(PatientTicket ticket) {
        if (ticket.getAppointment() == null || ticket.getAppointment().getDoctor() == null
                || ticket.getAppointment().getDoctor().getName() == null) return "—";
        String last = ticket.getAppointment().getDoctor().getName().getLastName();
        return "Dr. " + (last != null ? last : "—");
    }

    private String appointmentDate(PatientTicket ticket) {
        if (ticket.getAppointment() == null || ticket.getAppointment().getAppointmentDate() == null) return "—";
        return ticket.getAppointment().getAppointmentDate().toString();
    }

    private PatientTicket findTicketById(int ticketId) {
        return allTickets.stream().filter(t -> t.getTicketId() == ticketId).findFirst().orElse(null);
    }
}