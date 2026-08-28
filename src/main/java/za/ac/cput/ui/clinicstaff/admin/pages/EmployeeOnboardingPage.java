package za.ac.cput.ui.clinicstaff.admin.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.EmployeeAccessRequest;
import za.ac.cput.ui.clinicstaff.components.AccessRequestDetailsDialog;
import za.ac.cput.ui.clinicstaff.components.InviteEmployeeDialog;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

public class EmployeeOnboardingPage extends JPanel {

    private static final int ID_COLUMN = 4; // kept in the model for row lookups, hidden from view

    private DefaultTableModel tableModel;
    private JTable requestsTable;
    private List<EmployeeAccessRequest> currentRequests = List.of();

    public EmployeeOnboardingPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildInviteCards());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildPendingRequestsSection());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadPendingRequests();
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Employee Onboarding");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Invite new employees to join the MediTicket clinic.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildInviteCards() {
        JPanel grid = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_LG, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        grid.add(buildInviteCard(
                "Invite Doctor",
                "Send an invitation to a new doctor to join the clinic.",
                () -> InviteEmployeeDialog.show(this, "DOCTOR", null, this::loadPendingRequests)
        ));
        grid.add(buildInviteCard(
                "Invite Nurse",
                "Send an invitation to a new clinic nurse to join the clinic.",
                () -> InviteEmployeeDialog.show(this, "CLINIC_STAFF", "NURSE", this::loadPendingRequests)
        ));

        return grid;
    }

    private JPanel buildInviteCard(String title, String description, Runnable onInvite) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 16));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        descLabel.setForeground(AppTheme.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, AppTheme.SPACE_MD, 0));

        JButton inviteButton = new JButton(title);
        inviteButton.setFont(FontManager.bodyFont(Font.BOLD, 13));
        inviteButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
        inviteButton.setBackground(AppTheme.PRIMARY);
        inviteButton.setFocusPainted(false);
        inviteButton.setBorderPainted(false);
        inviteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        inviteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        inviteButton.addActionListener(e -> onInvite.run());

        card.add(titleLabel);
        card.add(descLabel);
        card.add(inviteButton);
        return card;
    }

    private JComponent buildPendingRequestsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(AppTheme.SURFACE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD)
        ));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Pending Access Requests");
        title.setFont(FontManager.bodyFont(Font.BOLD, 16));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel note = new JLabel("Employees who self-requested access. Click a row to review, approve, or reject.");
        note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        note.setForeground(AppTheme.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, AppTheme.SPACE_SM, 0));

        String[] columns = {"Email", "Type", "Role", "Requested", "id"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        requestsTable = new JTable(tableModel);
        requestsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        requestsTable.setRowHeight(40);
        requestsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        requestsTable.setShowGrid(false);
        requestsTable.setIntercellSpacing(new Dimension(0, 0));

        // Hide the id column from view — it stays in the model so
        // RowClickHelper can look up which request a row represents.
        TableColumn idColumn = requestsTable.getColumnModel().getColumn(ID_COLUMN);
        requestsTable.getColumnModel().removeColumn(idColumn);

        RowClickHelper.makeRowsClickable(requestsTable, ID_COLUMN, this::openDetails);

        JScrollPane tableScroll = new JScrollPane(requestsTable);
        tableScroll.setPreferredSize(new Dimension(0, 260));
        tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(title);
        section.add(note);
        section.add(tableScroll);
        return section;
    }

    private void loadPendingRequests() {
        BaseApiClient.ApiResult<List<EmployeeAccessRequest>> result =
                ApiClientProvider.getInstance().auth().getAccessRequests("PENDING");

        tableModel.setRowCount(0);
        currentRequests = result.isSuccess() ? result.getData() : List.of();

        for (EmployeeAccessRequest r : currentRequests) {
            String role = r.getRequestedStaffRole() != null ? r.getRequestedStaffRole() : "—";
            tableModel.addRow(new Object[]{
                    r.getEmail(), r.getRequestedUserType(), role,
                    r.getRequestDate() != null ? r.getRequestDate().toLocalDate().toString() : "—",
                    r.getRequestId()
            });
        }
    }

    // Note: RowClickHelper's idColumn lookup reads table.getValueAt(row, idColumn),
    // which still works correctly after removeColumn() — that call operates on the
    // TableColumnModel (view), not the underlying TableModel the id data lives in.
    private void openDetails(int requestId) {
        currentRequests.stream()
                .filter(r -> r.getRequestId() == requestId)
                .findFirst()
                .ifPresent(r -> AccessRequestDetailsDialog.show(this, r, this::loadPendingRequests));
    }
}