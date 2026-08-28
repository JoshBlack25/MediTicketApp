package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.EmployeeAccessRequest;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;

/**
 * Read/act dialog for a single self-service employee access request.
 * Approve/Reject only render while status is PENDING — once processed,
 * the request becomes a read-only record (mirrors PaymentDetailsDialog's
 * status-gated action pattern).
 */
public class AccessRequestDetailsDialog {

    public static void show(Component parent, EmployeeAccessRequest request, Runnable onChanged) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Access Request Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(field("Email", request.getEmail()));
        content.add(field("Requested Role", requestedRoleLabel(request)));
        content.add(field("Requested", request.getRequestDate() != null
                ? request.getRequestDate().toLocalDate().toString() : "—"));

        JLabel statusValue = new JLabel(request.getStatus() != null ? request.getStatus() : "—");
        statusValue.setFont(FontManager.bodyFont(Font.BOLD, 14));
        statusValue.setForeground(AppTheme.statusColor(request.getStatus()));
        content.add(labeledRow("Status", statusValue));

        boolean isPending = "PENDING".equals(request.getStatus());

        if (isPending) {
            content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

            JLabel note = new JLabel("<html><i>Approving sends an invitation email to this address.</i></html>");
            note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            note.setForeground(AppTheme.TEXT_MUTED);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            note.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));
            content.add(note);

            JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
            actionRow.setOpaque(false);
            actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton approveButton = actionButton("Approve", AppTheme.STATUS_SUCCESS);
            approveButton.addActionListener(e -> approve(dialog, parent, request, onChanged));

            JButton rejectButton = actionButton("Reject", AppTheme.STATUS_DANGER);
            rejectButton.addActionListener(e -> reject(dialog, parent, request, onChanged));

            actionRow.add(approveButton);
            actionRow.add(rejectButton);
            content.add(actionRow);
        } else if (request.getAdminNotes() != null && !request.getAdminNotes().isBlank()) {
            content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
            content.add(field("Admin Notes", request.getAdminNotes()));
        }

        JButton close = new JButton("Close");
        close.setFont(FontManager.bodyFont(Font.BOLD, 13));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setAlignmentX(Component.LEFT_ALIGNMENT);
        close.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));
        close.addActionListener(e -> dialog.dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(close);
        content.add(Box.createVerticalGlue());
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static void approve(JDialog dialog, Component parent, EmployeeAccessRequest request, Runnable onChanged) {
        int confirm = JOptionPane.showConfirmDialog(dialog,
                "Approve access for " + request.getEmail() + "? An invitation email will be sent.",
                "Approve Request", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        BaseApiClient.ApiResult<String> result =
                ApiClientProvider.getInstance().auth().approveAccessRequest(request.getRequestId());

        if (result.isSuccess()) {
            dialog.dispose();
            AppDialog.show(parent, "Request Approved",
                    "An invitation email has been sent to the employee.", AppDialog.Type.SUCCESS);
            if (onChanged != null) onChanged.run();
        } else {
            AppDialog.show(parent, "Unable to Approve",
                    result.getMessage() != null ? result.getMessage() : "Something went wrong.", AppDialog.Type.ERROR);
        }
    }

    private static void reject(JDialog dialog, Component parent, EmployeeAccessRequest request, Runnable onChanged) {
        String notes = JOptionPane.showInputDialog(dialog, "Reason for rejection (optional):",
                "Reject Request", JOptionPane.PLAIN_MESSAGE);

        BaseApiClient.ApiResult<String> result =
                ApiClientProvider.getInstance().auth().rejectAccessRequest(request.getRequestId(), notes);

        if (result.isSuccess()) {
            dialog.dispose();
            AppDialog.show(parent, "Request Rejected", "The request has been rejected.", AppDialog.Type.INFO);
            if (onChanged != null) onChanged.run();
        } else {
            AppDialog.show(parent, "Unable to Reject",
                    result.getMessage() != null ? result.getMessage() : "Something went wrong.", AppDialog.Type.ERROR);
        }
    }

    private static String requestedRoleLabel(EmployeeAccessRequest request) {
        String userType = request.getRequestedUserType();
        String staffRole = request.getRequestedStaffRole();
        if ("DOCTOR".equals(userType)) return "Doctor";
        if (staffRole != null) return "Clinic Staff — " + staffRole;
        return userType != null ? userType : "—";
    }

    private static JButton actionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(FontManager.bodyFont(Font.BOLD, 13));
        button.setForeground(AppTheme.TEXT_ON_PRIMARY);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return button;
    }

    private static JComponent field(String label, String value) {
        JLabel valueLabel = new JLabel(value != null && !value.isBlank() ? value : "—");
        valueLabel.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        valueLabel.setForeground(AppTheme.TEXT_PRIMARY);
        return labeledRow(label, valueLabel);
    }

    private static JComponent labeledRow(String label, JComponent valueComponent) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueComponent.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(valueComponent);
        return block;
    }
}