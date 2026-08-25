package za.ac.cput.ui.patient.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.patient.components.AppointmentDetailsDialog;
import za.ac.cput.ui.patient.components.BookAppointmentDialog;
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
        String[] columns = {"Doctor", "Date", "Time", "Status", "Reason", "Action"};
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

        JScrollPane scroll = new JScrollPane(appointmentsTable);
        scroll.setPreferredSize(new Dimension(0, 400));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
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

        // Most recent first
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
        private int currentAppointmentId;

        ActionCellEditor() {
            JButton viewBtn = smallButton("View");
            viewBtn.addActionListener(e -> {
                fireEditingStopped();
                Appointment appt = findById(currentAppointmentId);
                if (appt != null) {
                    SwingUtilities.invokeLater(() -> AppointmentDetailsDialog.show(AppointmentsPage.this, appt));
                }
            });
            panel.add(viewBtn);
            panel.setBackground(AppTheme.SURFACE);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            if (row < 0 || row >= tableModel.getRowCount()) return panel;
            Object idValue = tableModel.getValueAt(row, 5);
            currentAppointmentId = idValue != null ? (int) idValue : -1;
            return panel;
        }

        @Override
        public Object getCellEditorValue() { return currentAppointmentId; }
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