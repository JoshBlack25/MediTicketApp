package za.ac.cput.ui.clinicstaff.admin.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.ui.clinicstaff.components.PatientDetailsDialog;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
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

/**
 * Patient directory for admins — the counterpart to StaffPage, but for the
 * patient user base. Beyond viewing, admins can toggle a patient between
 * ACTIVE/INACTIVE and delete a patient outright (backend now cascades the
 * delete through appointments/tickets/notifications/payments — see
 * PatientService.delete() on the backend). "Clear Inactive Patients" bulk-
 * deletes every currently-INACTIVE patient in one action, since that's the
 * main cleanup use case admins asked for. All network calls run inside a
 * SwingWorker so the window doesn't freeze mid-request.
 */
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
        String[] columns = {"Name", "Email", "Phone", "Date Registered", "Status", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 5; }
        };
        patientsTable = new JTable(tableModel);
        patientsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        patientsTable.setRowHeight(44);
        patientsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        patientsTable.setShowGrid(false);
        patientsTable.setIntercellSpacing(new Dimension(0, 0));
        patientsTable.getColumnModel().getColumn(5).setCellRenderer(new ActionCellRenderer());
        patientsTable.getColumnModel().getColumn(5).setCellEditor(new ActionCellEditor());
        patientsTable.getColumnModel().getColumn(5).setPreferredWidth(230);
        patientsTable.getColumnModel().getColumn(5).setMinWidth(230);

        JScrollPane scroll = new JScrollPane(patientsTable);
        scroll.setPreferredSize(new Dimension(0, 400));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    // ── Data loading ──────────────────────────────────────────────

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

        for (int i = 0; i < filtered.size(); i++) {
            Patient p = filtered.get(i);
            tableModel.addRow(new Object[]{
                    fullName(p).isBlank() ? "—" : fullName(p),
                    p.getEmail() != null ? p.getEmail() : "—",
                    p.getCellPhone() != null ? p.getCellPhone() : "—",
                    p.getDateRegistered() != null ? p.getDateRegistered().toString() : "—",
                    p.getAccountStatus() != null ? p.getAccountStatus() : "—",
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

    // ── Status toggle ────────────────────────────────────────────

    private void onToggleStatus(Patient patient) {
        String newStatus = "ACTIVE".equals(patient.getAccountStatus()) ? "INACTIVE" : "ACTIVE";
        patient.setAccountStatus(newStatus);

        setTableEnabled(false);
        SwingWorker<BaseApiClient.ApiResult<Patient>, Void> worker = new SwingWorker<>() {
            @Override
            protected BaseApiClient.ApiResult<Patient> doInBackground() {
                return ApiClientProvider.getInstance().patients().update(patient);
            }

            @Override
            protected void done() {
                setTableEnabled(true);
                try {
                    BaseApiClient.ApiResult<Patient> result = get();
                    if (!result.isSuccess()) {
                        JOptionPane.showMessageDialog(PatientsPage.this,
                                result.getMessage() != null ? result.getMessage() : "Could not update patient status.",
                                "Update Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PatientsPage.this,
                            "Something went wrong updating this patient.",
                            "Update Failed", JOptionPane.ERROR_MESSAGE);
                }
                loadData();
            }
        };
        worker.execute();
    }

    // ── Delete (single) ─────────────────────────────────────────

    private void onDeletePatient(Patient patient) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete " + fullName(patient) + "?\n" +
                        "This also removes their appointments, tickets, notifications, and payment history.\n" +
                        "This cannot be undone.",
                "Delete Patient", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        setTableEnabled(false);
        SwingWorker<BaseApiClient.ApiResult<Void>, Void> worker = new SwingWorker<>() {
            @Override
            protected BaseApiClient.ApiResult<Void> doInBackground() {
                return ApiClientProvider.getInstance().patients().delete(patient.getUserId());
            }

            @Override
            protected void done() {
                setTableEnabled(true);
                try {
                    BaseApiClient.ApiResult<Void> result = get();
                    if (!result.isSuccess()) {
                        JOptionPane.showMessageDialog(PatientsPage.this,
                                result.getMessage() != null ? result.getMessage() : "Could not delete this patient.",
                                "Delete Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PatientsPage.this,
                            "Something went wrong deleting this patient.",
                            "Delete Failed", JOptionPane.ERROR_MESSAGE);
                }
                loadData();
            }
        };
        worker.execute();
    }

    // ── Clear Inactive Patients (bulk) ──────────────────────────

    private void onClearInactive() {
        List<Patient> inactive = allPatients.stream()
                .filter(p -> "INACTIVE".equals(p.getAccountStatus()))
                .collect(Collectors.toList());

        if (inactive.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no inactive patients to clear.",
                    "Nothing To Do", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete all " + inactive.size() + " inactive patient(s)?\n" +
                        "This also removes their appointments, tickets, notifications, and payment history.\n" +
                        "This cannot be undone.",
                "Clear Inactive Patients", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        setTableEnabled(false);
        clearInactiveButton.setText("Clearing...");
        clearInactiveButton.setEnabled(false);

        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                int succeeded = 0, failed = 0;
                for (Patient p : inactive) {
                    BaseApiClient.ApiResult<Void> result = ApiClientProvider.getInstance().patients().delete(p.getUserId());
                    if (result.isSuccess()) succeeded++; else failed++;
                }
                return new int[]{succeeded, failed};
            }

            @Override
            protected void done() {
                setTableEnabled(true);
                clearInactiveButton.setText("Clear Inactive Patients");
                clearInactiveButton.setEnabled(true);

                try {
                    int[] counts = get();
                    String message = "Deleted " + counts[0] + " inactive patient(s).";
                    if (counts[1] > 0) {
                        message += "\n" + counts[1] + " could not be deleted due to a server error.";
                    }
                    JOptionPane.showMessageDialog(PatientsPage.this, message,
                            "Clear Inactive Patients", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PatientsPage.this,
                            "Something went wrong clearing inactive patients.",
                            "Clear Inactive Patients", JOptionPane.ERROR_MESSAGE);
                }
                loadData();
            }
        };
        worker.execute();
    }

    private void setTableEnabled(boolean enabled) {
        patientsTable.setEnabled(enabled);
        searchField.setEnabled(enabled);
    }

    // ── Table action column ──────────────────────────────────────

    private class ActionCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        ActionCellRenderer() { setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4)); }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int col) {
            removeAll();
            setBackground(AppTheme.SURFACE);

            List<Patient> filtered = currentFilteredRows();
            String toggleLabel = "View";
            if (row >= 0 && row < filtered.size()) {
                Patient p = filtered.get(row);
                toggleLabel = "ACTIVE".equals(p.getAccountStatus()) ? "Deactivate" : "Activate";
            }

            add(smallButton("View", AppTheme.PRIMARY));
            add(smallButton(toggleLabel, AppTheme.STATUS_INFO));
            add(smallButton("Delete", AppTheme.STATUS_DANGER));
            return this;
        }
    }

    private class ActionCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        private final JButton viewBtn;
        private final JButton toggleBtn;
        private final JButton deleteBtn;
        private int currentIndex;

        ActionCellEditor() {
            viewBtn = smallButton("View", AppTheme.PRIMARY);
            viewBtn.addActionListener(e -> {
                fireEditingStopped();
                withCurrentPatient(patient ->
                        SwingUtilities.invokeLater(() -> PatientDetailsDialog.show(PatientsPage.this, patient)));
            });

            toggleBtn = smallButton("Activate", AppTheme.STATUS_INFO);
            toggleBtn.addActionListener(e -> {
                fireEditingStopped();
                withCurrentPatient(PatientsPage.this::onToggleStatus);
            });

            deleteBtn = smallButton("Delete", AppTheme.STATUS_DANGER);
            deleteBtn.addActionListener(e -> {
                fireEditingStopped();
                withCurrentPatient(PatientsPage.this::onDeletePatient);
            });

            panel.add(viewBtn);
            panel.add(toggleBtn);
            panel.add(deleteBtn);
            panel.setBackground(AppTheme.SURFACE);
        }

        private void withCurrentPatient(java.util.function.Consumer<Patient> action) {
            List<Patient> filtered = currentFilteredRows();
            if (currentIndex < 0 || currentIndex >= filtered.size()) return;
            action.accept(filtered.get(currentIndex));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            if (row < 0 || row >= tableModel.getRowCount()) return panel;
            Object idxValue = tableModel.getValueAt(row, 5);
            currentIndex = idxValue != null ? (int) idxValue : -1;

            List<Patient> filtered = currentFilteredRows();
            if (currentIndex >= 0 && currentIndex < filtered.size()) {
                Patient p = filtered.get(currentIndex);
                toggleBtn.setText("ACTIVE".equals(p.getAccountStatus()) ? "Deactivate" : "Activate");
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() { return currentIndex; }
    }

    private JButton smallButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(FontManager.bodyFont(Font.BOLD, 11));
        button.setForeground(color);
        button.setBackground(AppTheme.SURFACE);
        button.setBorder(BorderFactory.createLineBorder(color, 1, true));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(2, 6, 2, 6));
        return button;
    }
}