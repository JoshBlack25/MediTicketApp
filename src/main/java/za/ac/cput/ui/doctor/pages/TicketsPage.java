package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.doctor.components.TicketDetailsDialog;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;


public class TicketsPage extends JPanel {

    private SummaryCard openCard, inProgressCard, resolvedCard;
    private DefaultTableModel tableModel;
    private JTable ticketsTable;

    private List<PatientTicket> myTickets = List.of();
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

        JLabel subtitle = new JLabel("Your assigned consultations. Click a row to view details.");
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
        JPanel grid = new JPanel(new GridLayout(1, 3, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        openCard = new SummaryCard("Open", "—", AppTheme.PRIMARY);
        inProgressCard = new SummaryCard("In Progress", "—", AppTheme.STATUS_INFO);
        resolvedCard = new SummaryCard("Resolved", "—", AppTheme.STATUS_SUCCESS);

        grid.add(openCard);
        grid.add(inProgressCard);
        grid.add(resolvedCard);
        return grid;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"OPEN", "Open"}, {"IN_PROGRESS", "In Progress"}, {"RESOLVED", "Resolved"}
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
        String[] columns = {"Ticket", "Patient", "Appointment Date", "Status", "ID"};
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

        ticketsTable.getColumnModel().getColumn(4).setMinWidth(0);
        ticketsTable.getColumnModel().getColumn(4).setMaxWidth(0);
        ticketsTable.getColumnModel().getColumn(4).setWidth(0);

        RowClickHelper.makeRowsClickable(ticketsTable, 4, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(ticketsTable);
        scroll.setPreferredSize(new Dimension(0, 380));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int ticketId) {
        PatientTicket ticket = findById(ticketId);
        if (ticket != null) {
            TicketDetailsDialog.show(this, ticket, this::loadData);
        }
    }


    private void loadData() {
        int doctorId = SessionManager.getInstance().getUserId();

        BaseApiClient.ApiResult<List<PatientTicket>> result = ApiClientProvider.getInstance().patientTickets().getAll();
        List<PatientTicket> all = result.isSuccess() ? result.getData() : List.of();

        myTickets = all.stream()
                .filter(t -> t.getAppointment() != null
                        && t.getAppointment().getDoctor() != null
                        && t.getAppointment().getDoctor().getUserId() == doctorId)
                .filter(t -> !"CLOSED".equals(t.getCurrentStatus()))
                .collect(Collectors.toList());

        updateSummaryCards();
        renderTable();
    }

    private void updateSummaryCards() {
        openCard.setValue(String.valueOf(countByStatus("OPEN")));
        inProgressCard.setValue(String.valueOf(countByStatus("IN_PROGRESS")));
        resolvedCard.setValue(String.valueOf(countByStatus("RESOLVED")));
    }

    private long countByStatus(String status) {
        return myTickets.stream().filter(t -> status.equals(t.getCurrentStatus())).count();
    }

    private void renderTable() {
        tableModel.setRowCount(0);

        List<PatientTicket> filtered = "ALL".equals(activeFilter)
                ? myTickets
                : myTickets.stream().filter(t -> activeFilter.equals(t.getCurrentStatus())).collect(Collectors.toList());

        for (PatientTicket ticket : filtered) {
            tableModel.addRow(new Object[]{
                    "TK-" + String.format("%03d", ticket.getTicketId()),
                    patientName(ticket),
                    appointmentDate(ticket),
                    ticket.getCurrentStatus() != null ? ticket.getCurrentStatus().replace("_", " ") : "—",
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

    private String appointmentDate(PatientTicket ticket) {
        if (ticket.getAppointment() == null || ticket.getAppointment().getAppointmentDate() == null) return "—";
        return ticket.getAppointment().getAppointmentDate().toString();
    }

    private PatientTicket findById(int ticketId) {
        return myTickets.stream().filter(t -> t.getTicketId() == ticketId).findFirst().orElse(null);
    }
}