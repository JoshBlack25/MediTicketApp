package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.PatientDetailsDialog;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The doctor's own patient roster — derived from findByDoctor(doctorId)
 * appointments rather than a dedicated "patients of this doctor" endpoint
 * (none exists server-side), deduplicated by patient userId so someone
 * with 5 appointments only shows up once. Read-only: reuses the admin
 * side's PatientDetailsDialog directly, since a doctor has no business
 * editing patient records from here.
 */
public class PatientsPage extends JPanel {

    private SummaryCard totalPatientsCard, appointmentsThisMonthCard;
    private DefaultTableModel tableModel;
    private JTable patientsTable;
    private JTextField searchField;

    private List<Patient> myPatients = List.of();
    private String searchText = "";

    public PatientsPage() {
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

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        toolbar.add(buildSearchField(), BorderLayout.EAST);
        content.add(toolbar);

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

        JLabel title = new JLabel("Patients");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Everyone you've had an appointment with.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        totalPatientsCard = new SummaryCard("Total Patients", "\u2014", AppTheme.PRIMARY);
        appointmentsThisMonthCard = new SummaryCard("Appointments This Month", "\u2014", AppTheme.STATUS_INFO);

        grid.add(totalPatientsCard);
        grid.add(appointmentsThisMonthCard);
        return grid;
    }

    private JComponent buildSearchField() {
        searchField = new JTextField(18);
        searchField.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or email...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearchChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearchChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearchChanged(); }
        });
        return searchField;
    }

    private void onSearchChanged() {
        searchText = searchField.getText().trim().toLowerCase();
        renderTable();
    }

    private JComponent buildTable() {
        String[] columns = {"Name", "Email", "Phone", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 3; }
        };
        patientsTable = new JTable(tableModel);
        patientsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        patientsTable.setRowHeight(40);
        patientsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        patientsTable.setShowGrid(false);
        patientsTable.setIntercellSpacing(new Dimension(0, 0));
        patientsTable.getColumnModel().getColumn(3).setCellRenderer(new ActionCellRenderer());
        patientsTable.getColumnModel().getColumn(3).setCellEditor(new ActionCellEditor());
        patientsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        patientsTable.getColumnModel().getColumn(3).setMinWidth(90);

        JScrollPane scroll = new JScrollPane(patientsTable);
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
        List<Appointment> appointments = result.isSuccess() ? result.getData() : List.of();

        // Dedupe patients by userId, keeping just one Patient object per person.
        Map<Integer, Patient> byId = new LinkedHashMap<>();
        for (Appointment a : appointments) {
            if (a.getPatient() != null) {
                byId.put(a.getPatient().getUserId(), a.getPatient());
            }
        }
        myPatients = byId.values().stream()
                .sorted(Comparator.comparing(this::fullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        java.time.LocalDate startOfMonth = java.time.LocalDate.now().withDayOfMonth(1);
        long appointmentsThisMonth = appointments.stream()
                .filter(a -> a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(startOfMonth))
                .count();

        totalPatientsCard.setValue(String.valueOf(myPatients.size()));
        appointmentsThisMonthCard.setValue(String.valueOf(appointmentsThisMonth));

        renderTable();
    }

    private List<Patient> currentFilteredRows() {
        return myPatients.stream().filter(this::matchesSearch).collect(Collectors.toList());
    }

    private boolean matchesSearch(Patient p) {
        if (searchText.isEmpty()) return true;
        String name = fullName(p).toLowerCase();
        String email = p.getEmail() != null ? p.getEmail().toLowerCase() : "";
        return name.contains(searchText) || email.contains(searchText);
    }

    private void renderTable() {
        tableModel.setRowCount(0);
        List<Patient> filtered = currentFilteredRows();

        for (int i = 0; i < filtered.size(); i++) {
            Patient p = filtered.get(i);
            tableModel.addRow(new Object[]{
                    fullName(p).isBlank() ? "\u2014" : fullName(p),
                    p.getEmail() != null ? p.getEmail() : "\u2014",
                    p.getCellPhone() != null ? p.getCellPhone() : "\u2014",
                    i // row index into the filtered list, used by the action column
            });
        }
    }

    private String fullName(Patient p) {
        if (p.getName() == null) return "";
        String first = p.getName().getFirstName();
        String last = p.getName().getLastName();
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
                List<Patient> filtered = currentFilteredRows();
                if (currentIndex >= 0 && currentIndex < filtered.size()) {
                    Patient patient = filtered.get(currentIndex);
                    SwingUtilities.invokeLater(() -> PatientDetailsDialog.show(PatientsPage.this, patient));
                }
            });
            panel.add(view);
            panel.setBackground(AppTheme.SURFACE);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            if (row < 0 || row >= tableModel.getRowCount()) return panel;
            Object idxValue = tableModel.getValueAt(row, 3);
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