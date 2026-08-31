package za.ac.cput.ui.patient.pages;
//JOSHUA REID ADAMS - 230317693
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.patient.components.AppointmentDetailsDialog;
import za.ac.cput.ui.patient.components.BookAppointmentDialog;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;


public class AppointmentsPage extends JPanel {

    private DefaultTableModel tableModel;
    private JTable appointmentsTable;

    private List<Appointment> allAppointments = List.of();
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel title = new JLabel("Appointments");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("View and manage your appointments.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        textStack.add(title);
        textStack.add(subtitle);

        JButton bookNew = new JButton("+ Book New");
        bookNew.setFont(FontManager.bodyFont(Font.BOLD, 13));
        bookNew.setForeground(AppTheme.TEXT_ON_PRIMARY);
        bookNew.setBackground(AppTheme.PRIMARY);
        bookNew.setFocusPainted(false);
        bookNew.setBorderPainted(false);
        bookNew.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bookNew.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        bookNew.addActionListener(e -> BookAppointmentDialog.show(this, this::loadData));

        panel.add(textStack, BorderLayout.WEST);
        panel.add(bookNew, BorderLayout.EAST);
        return panel;
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
        String[] columns = {"Doctor", "Date", "Time", "Status", "Reason", "ID"};
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
        Appointment appt = findById(appointmentId);
        if (appt == null) return;
        AppointmentDetailsDialog.show(this, appt);
    }

    private void loadData() {
        int patientId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<List<Appointment>> result =
                ApiClientProvider.getInstance().appointments().findByPatient(patientId);
        allAppointments = result.isSuccess() ? result.getData() : List.of();
        renderTable();
    }

    private void renderTable() {
        tableModel.setRowCount(0);

        List<Appointment> filtered = "ALL".equals(activeFilter)
                ? allAppointments
                : allAppointments.stream().filter(a -> activeFilter.equals(a.getConfirmationStatus())).collect(Collectors.toList());


        filtered = filtered.stream()
                .sorted((a, b) -> {
                    if (a.getAppointmentDate() == null) return 1;
                    if (b.getAppointmentDate() == null) return -1;
                    return b.getAppointmentDate().compareTo(a.getAppointmentDate());
                })
                .collect(Collectors.toList());

        for (Appointment appt : filtered) {
            tableModel.addRow(new Object[]{
                    doctorName(appt),
                    appt.getAppointmentDate() != null ? appt.getAppointmentDate().toString() : "—",
                    appt.getAppointmentTime() != null ? appt.getAppointmentTime().toString() : "—",
                    appt.getConfirmationStatus() != null ? appt.getConfirmationStatus() : "—",
                    appt.getReason() != null && !appt.getReason().isBlank() ? appt.getReason() : "—",
                    appt.getAppointmentId()
            });
        }
    }

    private String doctorName(Appointment appt) {
        if (appt.getDoctor() == null || appt.getDoctor().getName() == null) return "Not yet assigned";
        String last = appt.getDoctor().getName().getLastName();
        return "Dr. " + (last != null ? last : "—");
    }

    private Appointment findById(int appointmentId) {
        return allAppointments.stream().filter(a -> a.getAppointmentId() == appointmentId).findFirst().orElse(null);
    }
}