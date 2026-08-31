package za.ac.cput.ui.clinicstaff.nurse.pages;
//AIDAN BARENDS - 230155639
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.ui.clinicstaff.components.PatientDetailsDialog;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


public class PatientsPage extends JPanel {

    private SummaryCard totalCard, activeCard, newThisMonthCard;
    private DefaultTableModel tableModel;
    private JTable patientsTable;
    private JPanel filterBarContainer;
    private JTextField searchField;
    private JButton clearInactiveButton;

    private List<Patient> allPatients = List.of();
    private String activeFilter = "ALL";
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
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        filterBarContainer = new JPanel(new BorderLayout());
        filterBarContainer.setOpaque(false);
        filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
        toolbar.add(filterBarContainer, BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_SM, 0));
        rightTools.setOpaque(false);
        rightTools.add(buildSearchField());
        rightTools.add(buildClearInactiveButton());
        toolbar.add(rightTools, BorderLayout.EAST);
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

        JLabel subtitle = new JLabel("View and manage everyone registered as a patient on MediTicket.");
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

        totalCard = new SummaryCard("Total Patients", "—", AppTheme.PRIMARY);
        activeCard = new SummaryCard("Active", "—", AppTheme.STATUS_SUCCESS);
        newThisMonthCard = new SummaryCard("New This Month", "—", AppTheme.STATUS_INFO);

        grid.add(totalCard);
        grid.add(activeCard);
        grid.add(newThisMonthCard);
        return grid;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"ACTIVE", "Active"}, {"INACTIVE", "Inactive"}, {"SUSPENDED", "Suspended"}
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

    private JComponent buildSearchField() {
        searchField = new JTextField(18);
        searchField.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or email...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { onSearchChanged(); }
            @Override
            public void removeUpdate(DocumentEvent e) { onSearchChanged(); }
            @Override
            public void changedUpdate(DocumentEvent e) { onSearchChanged(); }
        });
        return searchField;
    }

    private JComponent buildClearInactiveButton() {
        clearInactiveButton = new JButton("Clear Inactive Patients");
        clearInactiveButton.setFont(FontManager.bodyFont(Font.BOLD, 12));
        clearInactiveButton.setForeground(AppTheme.STATUS_DANGER);
        clearInactiveButton.setBackground(AppTheme.SURFACE);
        clearInactiveButton.setBorder(BorderFactory.createLineBorder(AppTheme.STATUS_DANGER, 1, true));
        clearInactiveButton.setFocusPainted(false);
        clearInactiveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearInactiveButton.addActionListener(e -> onClearInactive());
        return clearInactiveButton;
    }

    private void onSearchChanged() {
        searchText = searchField.getText().trim().toLowerCase();
        renderTable();
    }

    private JComponent buildTable() {
        String[] columns = {"Name", "Email", "Phone", "Date Registered", "Status", "ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        patientsTable = new JTable(tableModel);
        patientsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        patientsTable.setRowHeight(44);
        patientsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        patientsTable.setShowGrid(false);
        patientsTable.setIntercellSpacing(new Dimension(0, 0));
        patientsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        patientsTable.getColumnModel().getColumn(5).setMinWidth(0);
        patientsTable.getColumnModel().getColumn(5).setMaxWidth(0);
        patientsTable.getColumnModel().getColumn(5).setWidth(0);

        RowClickHelper.makeRowsClickable(patientsTable, 5, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(patientsTable);
        scroll.setPreferredSize(new Dimension(0, 400));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int patientId) {
        Patient patient = findPatientById(patientId);
        if (patient == null) return;
        PatientDetailsDialog.show(this, patient, this::loadData);
    }


    private void loadData() {
        BaseApiClient.ApiResult<List<Patient>> result = ApiClientProvider.getInstance().patients().getAll();
        allPatients = result.isSuccess() ? result.getData() : List.of();
        updateSummaryCards();
        renderTable();
    }

    private void updateSummaryCards() {
        long activeCount = allPatients.stream().filter(p -> "ACTIVE".equals(p.getAccountStatus())).count();

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        long newThisMonth = allPatients.stream()
                .filter(p -> p.getDateRegistered() != null && !p.getDateRegistered().isBefore(startOfMonth))
                .count();

        totalCard.setValue(String.valueOf(allPatients.size()));
        activeCard.setValue(String.valueOf(activeCount));
        newThisMonthCard.setValue(String.valueOf(newThisMonth));
    }

    private List<Patient> currentFilteredRows() {
        return allPatients.stream()
                .filter(p -> "ALL".equals(activeFilter) || activeFilter.equals(p.getAccountStatus()))
                .filter(this::matchesSearch)
                .collect(Collectors.toList());
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
                    p.getDateRegistered() != null ? p.getDateRegistered().toString() : "\u2014",
                    p.getAccountStatus() != null ? p.getAccountStatus() : "\u2014",
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

    private Patient findPatientById(int patientId) {
        return allPatients.stream().filter(p -> p.getUserId() == patientId).findFirst().orElse(null);
    }


    private void onClearInactive() {
        List<Patient> inactive = allPatients.stream()
                .filter(p -> "INACTIVE".equals(p.getAccountStatus()))
                .collect(Collectors.toList());

        if (inactive.isEmpty()) {
            AppDialog.show(this, "Nothing To Do", "There are no inactive patients to clear.", AppDialog.Type.SUCCESS);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete all " + inactive.size() + " inactive patient(s)?\n" +
                        "This also removes their appointments, tickets, notifications, and payment history.\n" +
                        "This cannot be undone.",
                "Clear Inactive Patients", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        int succeeded = 0, failed = 0;
        for (Patient p : inactive) {
            BaseApiClient.ApiResult<Void> result = ApiClientProvider.getInstance().patients().delete(p.getUserId());
            if (result.isSuccess()) succeeded++; else failed++;
        }

        String message = "Deleted " + succeeded + " inactive patient(s).";
        if (failed > 0) {
            message += "\n" + failed + " could not be deleted due to a server error.";
        }
        AppDialog.show(this, "Clear Inactive Patients", message, AppDialog.Type.SUCCESS);
        loadData();
    }
}
