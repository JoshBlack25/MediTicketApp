package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.doctor.components.PatientDetailsDialog;
import za.ac.cput.ui.layout.RowClickHelper;
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
        // ID column stays in the model for RowClickHelper, hidden from view.
        String[] columns = {"Name", "Email", "Phone", "ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        patientsTable = new JTable(tableModel);
        patientsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        patientsTable.setRowHeight(40);
        patientsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        patientsTable.setShowGrid(false);
        patientsTable.setIntercellSpacing(new Dimension(0, 0));
        patientsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        patientsTable.getColumnModel().getColumn(3).setMinWidth(0);
        patientsTable.getColumnModel().getColumn(3).setMaxWidth(0);
        patientsTable.getColumnModel().getColumn(3).setWidth(0);

        RowClickHelper.makeRowsClickable(patientsTable, 3, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(patientsTable);
        scroll.setPreferredSize(new Dimension(0, 400));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int patientId) {
        Patient patient = myPatients.stream().filter(p -> p.getUserId() == patientId).findFirst().orElse(null);
        if (patient == null) return;
        PatientDetailsDialog.show(this, patient);
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

        for (Patient p : filtered) {
            tableModel.addRow(new Object[]{
                    fullName(p).isBlank() ? "\u2014" : fullName(p),
                    p.getEmail() != null ? p.getEmail() : "\u2014",
                    p.getCellPhone() != null ? p.getCellPhone() : "\u2014",
                    p.getUserId()
            });
        }
    }

    private String fullName(Patient p) {
        if (p.getName() == null) return "";
        String first = p.getName().getFirstName();
        String last = p.getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }
}