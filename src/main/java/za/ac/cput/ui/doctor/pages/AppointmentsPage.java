package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.doctor.components.AppointmentDetailsDialog;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class AppointmentsPage extends JPanel {

    private SummaryCard pendingCard, confirmedCard, completedCard;
    private DefaultTableModel tableModel;
    private JTable appointmentsTable;

    private List<Appointment> myAppointments = List.of();
    private String activeFilter = "ALL";
    private JPanel filterBarContainer;

    public AppointmentsPage() {
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Appointments");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Your upcoming and past appointment schedule.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 3, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        pendingCard = new SummaryCard("Pending", "\u2014", AppTheme.STATUS_WARNING);
        confirmedCard = new SummaryCard("Confirmed", "\u2014", AppTheme.PRIMARY);
        completedCard = new SummaryCard("Completed", "\u2014", AppTheme.STATUS_SUCCESS);

        grid.add(pendingCard);
        grid.add(confirmedCard);
        grid.add(completedCard);
        return grid;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"PENDING", "Pending"}, {"CONFIRMED", "Confirmed"},
                {"COMPLETED", "Completed"}, {"CANCELLED", "Cancelled"}, {"REJECTED", "Rejected"}
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
        String[] columns = {"Patient", "Date", "Time", "Reason", "Status", "ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        appointmentsTable = new JTable(tableModel);
        appointmentsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        appointmentsTable.setRowHeight(40);
        appointmentsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        appointmentsTable.setShowGrid(false);
        appointmentsTable.setIntercellSpacing(new Dimension(0, 0));
        appointmentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        appointmentsTable.getColumnModel().getColumn(5).setMinWidth(0);
        appointmentsTable.getColumnModel().getColumn(5).setMaxWidth(0);
        appointmentsTable.getColumnModel().getColumn(5).setWidth(0);

        RowClickHelper.makeRowsClickable(appointmentsTable, 5, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(appointmentsTable);
        scroll.setPreferredSize(new Dimension(0, 400));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int appointmentId) {
        Appointment appointment = currentFilteredRows().stream()
                .filter(a -> a.getAppointmentId() == appointmentId)
                .findFirst().orElse(null);
        if (appointment == null) return;
        AppointmentDetailsDialog.show(this, appointment);
    }


    private void loadData() {
        int doctorId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<List<Appointment>> result =
                ApiClientProvider.getInstance().appointments().findByDoctor(doctorId);

        myAppointments = result.isSuccess() ? result.getData() : List.of();
        updateSummaryCards();
        renderTable();
    }

    private void updateSummaryCards() {
        pendingCard.setValue(String.valueOf(countByStatus("PENDING")));
        confirmedCard.setValue(String.valueOf(countByStatus("CONFIRMED")));
        completedCard.setValue(String.valueOf(countByStatus("COMPLETED")));
    }

    private long countByStatus(String status) {
        return myAppointments.stream().filter(a -> status.equals(a.getConfirmationStatus())).count();
    }

    private List<Appointment> currentFilteredRows() {
        List<Appointment> filtered = "ALL".equals(activeFilter)
                ? myAppointments
                : myAppointments.stream().filter(a -> activeFilter.equals(a.getConfirmationStatus())).collect(Collectors.toList());

        return filtered.stream()
                .sorted(Comparator.comparing(
                        (Appointment a) -> a.getAppointmentDate() != null ? a.getAppointmentDate() : java.time.LocalDate.MAX
                ).reversed())
                .collect(Collectors.toList());
    }

    private void renderTable() {
        tableModel.setRowCount(0);
        List<Appointment> filtered = currentFilteredRows();

        for (Appointment a : filtered) {
            tableModel.addRow(new Object[]{
                    patientName(a),
                    a.getAppointmentDate() != null ? a.getAppointmentDate().toString() : "\u2014",
                    a.getAppointmentTime() != null ? a.getAppointmentTime().toString() : "\u2014",
                    a.getReason() != null && !a.getReason().isBlank() ? a.getReason() : "\u2014",
                    a.getConfirmationStatus() != null ? a.getConfirmationStatus().replace("_", " ") : "\u2014",
                    a.getAppointmentId()
            });
        }
    }

    private String patientName(Appointment appt) {
        if (appt.getPatient() == null || appt.getPatient().getName() == null) return "\u2014";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }
}