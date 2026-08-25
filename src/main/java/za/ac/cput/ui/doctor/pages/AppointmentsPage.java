package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.doctor.components.AppointmentDetailsDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only view of the doctor's own appointment schedule. No
 * findByDoctor-with-status-filter endpoint exists, so this loads
 * findByDoctor(doctorId) once and filters client-side — same pattern
 * TicketsPage already uses for this role. Approve/Reject isn't offered
 * here; that's a staff-side action (AppointmentController#approve
 * requires a staffId), the doctor is only reviewing what's booked.
 */
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
        String[] columns = {"Patient", "Date", "Time", "Reason", "Status", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 5; }
        };
        appointmentsTable = new JTable(tableModel);
        appointmentsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        appointmentsTable.setRowHeight(40);
        appointmentsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        appointmentsTable.setShowGrid(false);
        appointmentsTable.setIntercellSpacing(new Dimension(0, 0));
        appointmentsTable.getColumnModel().getColumn(5).setCellRenderer(new ActionCellRenderer());
        appointmentsTable.getColumnModel().getColumn(5).setCellEditor(new ActionCellEditor());
        appointmentsTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        appointmentsTable.getColumnModel().getColumn(5).setMinWidth(90);

        JScrollPane scroll = new JScrollPane(appointmentsTable);
        scroll.setPreferredSize(new Dimension(0, 400));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    // ── Data loading ──────────────────────────────────────────────

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

        for (int i = 0; i < filtered.size(); i++) {
            Appointment a = filtered.get(i);
            tableModel.addRow(new Object[]{
                    patientName(a),
                    a.getAppointmentDate() != null ? a.getAppointmentDate().toString() : "\u2014",
                    a.getAppointmentTime() != null ? a.getAppointmentTime().toString() : "\u2014",
                    a.getReason() != null && !a.getReason().isBlank() ? a.getReason() : "\u2014",
                    a.getConfirmationStatus() != null ? a.getConfirmationStatus().replace("_", " ") : "\u2014",
                    i // row index into the filtered list, used by the action column
            });
        }
    }

    private String patientName(Appointment appt) {
        if (appt.getPatient() == null || appt.getPatient().getName() == null) return "\u2014";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }

    // ── Table action column ──────────────────────────────────────

    private class ActionCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        ActionCellRenderer() { setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4)); }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int col) {
            removeAll();
            setBackground(AppTheme.SURFACE);
            add(smallButton("View"));
            return this;
        }
    }

    private class ActionCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        private int currentIndex;

        ActionCellEditor() {
            JButton view = smallButton("View");
            view.addActionListener(e -> {
                fireEditingStopped();
                List<Appointment> filtered = currentFilteredRows();
                if (currentIndex >= 0 && currentIndex < filtered.size()) {
                    Appointment appointment = filtered.get(currentIndex);
                    SwingUtilities.invokeLater(() -> AppointmentDetailsDialog.show(AppointmentsPage.this, appointment));
                }
            });
            panel.add(view);
            panel.setBackground(AppTheme.SURFACE);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            if (row < 0 || row >= tableModel.getRowCount()) return panel;
            Object idxValue = tableModel.getValueAt(row, 5);
            currentIndex = idxValue != null ? (int) idxValue : -1;
            return panel;
        }

        @Override
        public Object getCellEditorValue() { return currentIndex; }
    }

    private JButton smallButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FontManager.bodyFont(Font.BOLD, 11));
        button.setForeground(AppTheme.PRIMARY);
        button.setBackground(AppTheme.SURFACE);
        button.setBorder(BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(2, 8, 2, 8));
        return button;
    }
}