package za.ac.cput.ui.clinicstaff.nurse.pages;
//JOSHUA REID ADAMS - 230317693
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.ApproveAppointmentDialog;
import za.ac.cput.ui.clinicstaff.components.AppointmentDetailsDialog;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nurse's Appointments page — functionally identical to
 * clinicstaff.admin.pages.AppointmentsPage. Nurses share full approve/reject
 * capability with admin per the business flow, so this reuses the shared
 * ApproveAppointmentDialog and AppointmentDetailsDialog from
 * za.ac.cput.ui.clinicstaff.components directly, rather than duplicating
 * them — those dialogs derive staffId from SessionManager, which resolves
 * correctly for either role.
 */
public class AppointmentsPage extends JPanel {

    private SummaryCard pendingCard, confirmedCard, completedCard, cancelledCard;
    private JPanel needsAttentionSection;
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
        content.add(buildSummaryCards());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        needsAttentionSection = emptySection();
        content.add(needsAttentionSection);
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

        JLabel subtitle = new JLabel("Review, approve, and manage clinic appointments. Click a row to view details.");
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

        pendingCard = new SummaryCard("Pending", "—", AppTheme.STATUS_WARNING);
        confirmedCard = new SummaryCard("Confirmed", "—", AppTheme.PRIMARY);
        completedCard = new SummaryCard("Completed", "—", AppTheme.STATUS_SUCCESS);
        cancelledCard = new SummaryCard("Cancelled", "—", AppTheme.STATUS_DANGER);

        grid.add(pendingCard);
        grid.add(confirmedCard);
        grid.add(completedCard);
        grid.add(cancelledCard);
        return grid;
    }

    private JPanel emptySection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"PENDING", "Pending"}, {"CONFIRMED", "Confirmed"},
                {"COMPLETED", "Completed"}, {"CANCELLED", "Cancelled"}, {"RESCHEDULED", "Rescheduled"}
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
        String[] columns = {"Patient", "Doctor", "Date", "Time", "Status", "Reason", "ID"};
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

        appointmentsTable.getColumnModel().getColumn(6).setMinWidth(0);
        appointmentsTable.getColumnModel().getColumn(6).setMaxWidth(0);
        appointmentsTable.getColumnModel().getColumn(6).setWidth(0);

        RowClickHelper.makeRowsClickable(appointmentsTable, 6, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(appointmentsTable);
        scroll.setPreferredSize(new Dimension(0, 380));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int appointmentId) {
        Appointment appt = findById(appointmentId);
        if (appt == null) return;

        AppointmentDetailsDialog.show(this, appt, new AppointmentDetailsDialog.ActionCallback() {
            @Override
            public void onApprove() {
                ApproveAppointmentDialog.show(AppointmentsPage.this, appt, AppointmentsPage.this::loadData);
            }

            @Override
            public void onReject() {
                rejectAppointment(appt);
            }
        });
    }

    // ── Data loading ──────────────────────────────────────────────

    private void loadData() {
        BaseApiClient.ApiResult<List<Appointment>> result = ApiClientProvider.getInstance().appointments().getAll();
        allAppointments = result.isSuccess() ? result.getData() : List.of();

        updateSummaryCards();
        updateNeedsAttention();
        renderTable();
    }

    private void updateSummaryCards() {
        pendingCard.setValue(String.valueOf(countByStatus("PENDING")));
        confirmedCard.setValue(String.valueOf(countByStatus("CONFIRMED")));
        completedCard.setValue(String.valueOf(countByStatus("COMPLETED")));
        cancelledCard.setValue(String.valueOf(countByStatus("CANCELLED")));
    }

    private long countByStatus(String status) {
        return allAppointments.stream().filter(a -> status.equals(a.getConfirmationStatus())).count();
    }

    private void updateNeedsAttention() {
        needsAttentionSection.removeAll();

        long pending = countByStatus("PENDING");
        if (pending == 0) {
            needsAttentionSection.revalidate();
            needsAttentionSection.repaint();
            return;
        }

        JLabel title = new JLabel("Needs Attention");
        title.setFont(FontManager.bodyFont(Font.BOLD, 15));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));
        needsAttentionSection.add(title);

        needsAttentionSection.add(attentionBanner(
                "\uD83D\uDCC5 " + pending + " appointment" + (pending == 1 ? "" : "s") + " awaiting approval",
                "New booking requests need review.", "PENDING"));

        needsAttentionSection.revalidate();
        needsAttentionSection.repaint();
    }

    private JComponent attentionBanner(String title, String subtitle, String filterOnClick) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(AppTheme.SURFACE_ALT);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_MD, AppTheme.SPACE_SM, AppTheme.SPACE_MD)
        ));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        subtitleLabel.setForeground(AppTheme.TEXT_SECONDARY);

        textStack.add(titleLabel);
        textStack.add(subtitleLabel);

        JButton viewButton = new JButton("View Appointments");
        viewButton.setFont(FontManager.bodyFont(Font.BOLD, 12));
        viewButton.setFocusPainted(false);
        viewButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewButton.addActionListener(e -> {
            activeFilter = filterOnClick;
            renderTable();
            filterBarContainer.removeAll();
            filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
            filterBarContainer.revalidate();
            filterBarContainer.repaint();
        });

        banner.add(textStack, BorderLayout.WEST);
        banner.add(viewButton, BorderLayout.EAST);
        return banner;
    }

    private void renderTable() {
        tableModel.setRowCount(0);

        List<Appointment> filtered = "ALL".equals(activeFilter)
                ? allAppointments
                : allAppointments.stream().filter(a -> activeFilter.equals(a.getConfirmationStatus())).collect(Collectors.toList());

        for (Appointment appt : filtered) {
            tableModel.addRow(new Object[]{
                    patientName(appt),
                    doctorName(appt),
                    appt.getAppointmentDate() != null ? appt.getAppointmentDate().toString() : "—",
                    appt.getAppointmentTime() != null ? appt.getAppointmentTime().toString() : "—",
                    appt.getConfirmationStatus() != null ? appt.getConfirmationStatus() : "—",
                    appt.getReason() != null && !appt.getReason().isBlank() ? appt.getReason() : "—",
                    appt.getAppointmentId()
            });
        }
    }

    private String patientName(Appointment appt) {
        if (appt.getPatient() == null || appt.getPatient().getName() == null) return "—";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last.charAt(0) + "." : "");
    }

    private String doctorName(Appointment appt) {
        if (appt.getDoctor() == null || appt.getDoctor().getName() == null) return "Unassigned";
        String last = appt.getDoctor().getName().getLastName();
        return "Dr. " + (last != null ? last : "—");
    }

    private Appointment findById(int appointmentId) {
        return allAppointments.stream().filter(a -> a.getAppointmentId() == appointmentId).findFirst().orElse(null);
    }

    private void rejectAppointment(Appointment appt) {
        String reason = JOptionPane.showInputDialog(this, "Reason for rejection (required):",
                "Reject Appointment", JOptionPane.PLAIN_MESSAGE);

        if (reason == null) {
            return; // user cancelled — do nothing
        }
        if (reason.isBlank()) {
            AppDialog.show(this, "Reason Required",
                    "Please provide a reason for rejecting this appointment.", AppDialog.Type.ERROR);
            return;
        }

        int staffId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<Appointment> result = ApiClientProvider.getInstance()
                .appointments().reject(appt.getAppointmentId(), staffId, reason);

        if (result.isSuccess()) {
            AppDialog.show(this, "Appointment Rejected", "The appointment has been rejected.", AppDialog.Type.INFO);
            loadData();
        } else {
            AppDialog.show(this, "Unable to Reject",
                    result.getMessage() != null ? result.getMessage() : "Something went wrong.", AppDialog.Type.ERROR);
        }
    }
}