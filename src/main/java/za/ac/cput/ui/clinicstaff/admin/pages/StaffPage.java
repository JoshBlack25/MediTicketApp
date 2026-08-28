package za.ac.cput.ui.clinicstaff.admin.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.ClinicStaff;
import za.ac.cput.model.domain.Doctor;
import za.ac.cput.ui.clinicstaff.components.StaffDetailsDialog;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only workforce directory: Doctors, Nurses, Admins. Patients are
 * intentionally excluded — this page is scoped to platform staff, not the
 * full user base. No creation here (that's EmployeeOnboardingPage's job)
 * and no editing yet (backend has no safe update-staff path).
 *
 * Rows open StaffDetailsDialog on click via RowClickHelper — no Action
 * column. The hidden last column holds the row's index into the current
 * filtered list (not a real entity ID, since Doctor and ClinicStaff have
 * independently auto-generated IDs that can collide across tables), which
 * onRowClicked() uses to look the row back up.
 */
public class StaffPage extends JPanel {

    // Unified row type so one table can hold both Doctor and ClinicStaff
    // without two different table shapes or duplicated rendering logic.
    private static class StaffRow {
        String name, role, email, phone, extra, status;
        Object source; // original Doctor or ClinicStaff, for the details dialog
        boolean isDoctor;
    }

    private SummaryCard doctorsCard, nursesCard, adminsCard, totalCard;
    private DefaultTableModel tableModel;
    private JTable staffTable;
    private JPanel filterBarContainer;

    private List<StaffRow> allRows = new ArrayList<>();
    private String activeFilter = "ALL";

    public StaffPage() {
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

        JLabel title = new JLabel("Staff");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("View everyone with clinic staff access to MediTicket. Click a row to view details.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        doctorsCard = new SummaryCard("Doctors", "—", AppTheme.PRIMARY);
        nursesCard = new SummaryCard("Nurses", "—", AppTheme.STATUS_INFO);
        adminsCard = new SummaryCard("Admins", "—", AppTheme.STATUS_WARNING);
        totalCard = new SummaryCard("Total Staff", "—", AppTheme.STATUS_SUCCESS);

        grid.add(doctorsCard);
        grid.add(nursesCard);
        grid.add(adminsCard);
        grid.add(totalCard);
        return grid;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"DOCTOR", "Doctors"}, {"NURSE", "Nurses"}, {"ADMIN", "Admins"}
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
        String[] columns = {"Name", "Role", "Email", "Phone", "Department / Specialty", "Status", "ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        staffTable = new JTable(tableModel);
        staffTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        staffTable.setRowHeight(40);
        staffTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        staffTable.setShowGrid(false);
        staffTable.setIntercellSpacing(new Dimension(0, 0));
        staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Hide the ID column — same pattern as PaymentsPage.
        staffTable.getColumnModel().getColumn(6).setMinWidth(0);
        staffTable.getColumnModel().getColumn(6).setMaxWidth(0);
        staffTable.getColumnModel().getColumn(6).setWidth(0);

        RowClickHelper.makeRowsClickable(staffTable, 6, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(staffTable);
        scroll.setPreferredSize(new Dimension(0, 400));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int filteredIndex) {
        List<StaffRow> filtered = currentFilteredRows();
        if (filteredIndex < 0 || filteredIndex >= filtered.size()) return;

        StaffRow row = filtered.get(filteredIndex);
        if (row.isDoctor) {
            StaffDetailsDialog.showDoctor(this, (Doctor) row.source);
        } else {
            StaffDetailsDialog.showClinicStaff(this, (ClinicStaff) row.source);
        }
    }

    // ── Data loading ──────────────────────────────────────────────

    private void loadData() {
        allRows.clear();

        BaseApiClient.ApiResult<List<Doctor>> doctorResult = ApiClientProvider.getInstance().doctors().getAll();
        List<Doctor> doctors = doctorResult.isSuccess() ? doctorResult.getData() : List.of();
        for (Doctor doctor : doctors) {
            StaffRow row = new StaffRow();
            row.name = fullName(doctor.getName());
            row.role = "DOCTOR";
            row.email = doctor.getEmail();
            row.phone = doctor.getCellPhone();
            row.extra = doctor.getSpecialty();
            row.status = doctor.getAccountStatus();
            row.source = doctor;
            row.isDoctor = true;
            allRows.add(row);
        }

        BaseApiClient.ApiResult<List<ClinicStaff>> staffResult = ApiClientProvider.getInstance().clinicStaff().getAll();
        List<ClinicStaff> staffList = staffResult.isSuccess() ? staffResult.getData() : List.of();
        for (ClinicStaff staff : staffList) {
            StaffRow row = new StaffRow();
            row.name = fullName(staff.getName());
            row.role = staff.getStaffRole();
            row.email = staff.getEmail();
            row.phone = staff.getCellPhone();
            row.extra = staff.getDepartment();
            row.status = staff.getAccountStatus();
            row.source = staff;
            row.isDoctor = false;
            allRows.add(row);
        }

        updateSummaryCards();
        renderTable();
    }

    private void updateSummaryCards() {
        long doctorCount = allRows.stream().filter(r -> "DOCTOR".equals(r.role)).count();
        long nurseCount = allRows.stream().filter(r -> "NURSE".equals(r.role)).count();
        long adminCount = allRows.stream().filter(r -> "ADMIN".equals(r.role)).count();

        doctorsCard.setValue(String.valueOf(doctorCount));
        nursesCard.setValue(String.valueOf(nurseCount));
        adminsCard.setValue(String.valueOf(adminCount));
        totalCard.setValue(String.valueOf(allRows.size()));
    }

    private void renderTable() {
        tableModel.setRowCount(0);

        List<StaffRow> filtered = currentFilteredRows();

        for (int i = 0; i < filtered.size(); i++) {
            StaffRow row = filtered.get(i);
            tableModel.addRow(new Object[]{
                    row.name != null && !row.name.isBlank() ? row.name : "—",
                    roleLabel(row.role),
                    row.email != null ? row.email : "—",
                    row.phone != null ? row.phone : "—",
                    row.extra != null && !row.extra.isBlank() ? row.extra : "—",
                    row.status != null ? row.status : "—",
                    i // hidden — index into the filtered list, used by onRowClicked
            });
        }
    }

    private List<StaffRow> currentFilteredRows() {
        return "ALL".equals(activeFilter)
                ? allRows
                : allRows.stream().filter(r -> activeFilter.equals(r.role)).collect(Collectors.toList());
    }

    private String roleLabel(String role) {
        if ("ADMIN".equals(role)) return "Administrator";
        if ("NURSE".equals(role)) return "Nurse";
        if ("DOCTOR".equals(role)) return "Doctor";
        return role != null ? role : "—";
    }

    private String fullName(Object name) {
        if (name == null) return "—";
        try {
            var firstMethod = name.getClass().getMethod("getFirstName");
            var lastMethod = name.getClass().getMethod("getLastName");
            String first = (String) firstMethod.invoke(name);
            String last = (String) lastMethod.invoke(name);
            return (first != null ? first : "") + " " + (last != null ? last : "");
        } catch (Exception e) {
            return "—";
        }
    }
}