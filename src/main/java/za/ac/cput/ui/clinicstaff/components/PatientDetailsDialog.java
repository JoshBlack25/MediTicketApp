package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class PatientDetailsDialog {

    public static void show(Component parent, Patient patient, Runnable onChanged) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Patient Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 480);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildFields(patient));

        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        JSeparator divider = new JSeparator();
        divider.setForeground(AppTheme.DIVIDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        content.add(divider);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        boolean isActive = "ACTIVE".equals(patient.getAccountStatus());

        JButton toggleButton = new JButton(isActive ? "Deactivate Patient" : "Activate Patient");
        toggleButton.setFont(FontManager.bodyFont(Font.BOLD, 13));
        toggleButton.setForeground(isActive ? AppTheme.STATUS_DANGER : AppTheme.STATUS_SUCCESS);
        toggleButton.setFocusPainted(false);
        toggleButton.setBackground(AppTheme.SURFACE);
        toggleButton.setBorder(BorderFactory.createLineBorder(isActive ? AppTheme.STATUS_DANGER : AppTheme.STATUS_SUCCESS, 1, true));
        toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggleButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        toggleButton.addActionListener(e -> {
            dialog.dispose();
            toggleStatus(parent, patient, onChanged);
        });

        JButton deleteButton = new JButton("Delete Patient");
        deleteButton.setFont(FontManager.bodyFont(Font.BOLD, 13));
        deleteButton.setForeground(AppTheme.STATUS_DANGER);
        deleteButton.setFocusPainted(false);
        deleteButton.setBackground(AppTheme.SURFACE);
        deleteButton.setBorderPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        deleteButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        deleteButton.addActionListener(e -> {
            dialog.dispose();
            deletePatient(parent, patient, onChanged);
        });

        JButton close = new JButton("Close");
        close.setFont(FontManager.bodyFont(Font.BOLD, 13));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setAlignmentX(Component.LEFT_ALIGNMENT);
        close.addActionListener(e -> dialog.dispose());

        content.add(toggleButton);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(deleteButton);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        content.add(close);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        dialog.setContentPane(scroll);
        dialog.setVisible(true);
    }

    private static void toggleStatus(Component parent, Patient patient, Runnable onChanged) {
        String newStatus = "ACTIVE".equals(patient.getAccountStatus()) ? "INACTIVE" : "ACTIVE";
        patient.setAccountStatus(newStatus);

        BaseApiClient.ApiResult<Patient> result = ApiClientProvider.getInstance().patients().update(patient);

        if (result.isSuccess()) {
            AppDialog.show(parent, "Patient Updated",
                    "This patient is now " + newStatus.toLowerCase() + ".", AppDialog.Type.SUCCESS);
        } else {
            AppDialog.show(parent, "Update Failed",
                    result.getMessage() != null ? result.getMessage() : "Could not update this patient.", AppDialog.Type.ERROR);
        }
        if (onChanged != null) onChanged.run();
    }

    private static void deletePatient(Component parent, Patient patient, Runnable onChanged) {
        int confirm = JOptionPane.showConfirmDialog(parent,
                "Permanently delete " + fullName(patient) + "?\n" +
                        "This also removes their appointments, tickets, notifications, and payment history.\n" +
                        "This cannot be undone.",
                "Delete Patient", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        BaseApiClient.ApiResult<Void> result = ApiClientProvider.getInstance().patients().delete(patient.getUserId());

        if (result.isSuccess()) {
            AppDialog.show(parent, "Patient Deleted", "The patient record has been removed.", AppDialog.Type.SUCCESS);
        } else {
            AppDialog.show(parent, "Delete Failed",
                    result.getMessage() != null ? result.getMessage() : "This patient could not be deleted.", AppDialog.Type.ERROR);
        }
        if (onChanged != null) onChanged.run();
    }

    private static JComponent buildFields(Patient patient) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(field("Name", fullName(patient)));
        panel.add(field("Email", patient.getEmail()));
        panel.add(field("Phone", patient.getCellPhone()));
        panel.add(field("Date of Birth", patient.getDob() != null ? patient.getDob().toString() : "\u2014"));
        panel.add(field("Date Registered", patient.getDateRegistered() != null ? patient.getDateRegistered().toString() : "\u2014"));
        panel.add(field("Emergency Contact", patient.getEmergencyContact()));
        panel.add(statusField(patient.getAccountStatus()));
        return panel;
    }

    private static String fullName(Patient patient) {
        if (patient.getName() == null) return "\u2014";
        String first = patient.getName().getFirstName();
        String last = patient.getName().getLastName();
        String name = (first != null ? first : "") + " " + (last != null ? last : "");
        return name.isBlank() ? "\u2014" : name;
    }

    private static JComponent field(String label, String value) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueComp = new JLabel(value != null && !value.isBlank() ? value : "\u2014");
        valueComp.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        valueComp.setForeground(AppTheme.TEXT_PRIMARY);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(valueComp);
        return block;
    }

    private static JComponent statusField(String status) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComp = new JLabel("Account Status");
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel value = new JLabel(status != null ? status : "\u2014");
        value.setFont(FontManager.bodyFont(Font.BOLD, 13));
        value.setForeground(AppTheme.statusColor(status));
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(value);
        return block;
    }
}