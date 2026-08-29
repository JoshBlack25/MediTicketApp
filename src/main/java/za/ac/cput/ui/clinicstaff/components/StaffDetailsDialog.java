package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.model.domain.ClinicStaff;
import za.ac.cput.model.domain.Doctor;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class StaffDetailsDialog {

    public static void showDoctor(Component parent, Doctor doctor) {
        show(parent, "Doctor Details", buildDoctorFields(doctor));
    }

    public static void showClinicStaff(Component parent, ClinicStaff staff) {
        String roleLabel = "ADMIN".equals(staff.getStaffRole()) ? "Administrator" : "Nurse";
        show(parent, roleLabel + " Details", buildClinicStaffFields(staff));
    }

    private static void show(Component parent, String title, JComponent fields) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(fields);

        JButton close = new JButton("Close");
        close.setFont(FontManager.bodyFont(Font.BOLD, 13));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setAlignmentX(Component.RIGHT_ALIGNMENT);
        close.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));
        close.addActionListener(e -> dialog.dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(close);
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static JComponent buildDoctorFields(Doctor doctor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(field("Name", fullName(doctor)));
        panel.add(field("Email", doctor.getEmail()));
        panel.add(field("Phone", doctor.getCellPhone()));
        panel.add(field("Date of Birth", doctor.getDob() != null ? doctor.getDob().toString() : "—"));
        panel.add(field("Specialty", doctor.getSpecialty()));
        panel.add(field("License Number", doctor.getLicenseNumber()));
        panel.add(statusField(doctor.getAccountStatus()));
        return panel;
    }

    private static JComponent buildClinicStaffFields(ClinicStaff staff) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(field("Name", fullName(staff)));
        panel.add(field("Email", staff.getEmail()));
        panel.add(field("Phone", staff.getCellPhone()));
        panel.add(field("Date of Birth", staff.getDob() != null ? staff.getDob().toString() : "—"));
        panel.add(field("Role", staff.getStaffRole()));
        panel.add(field("Department", staff.getDepartment()));
        panel.add(statusField(staff.getAccountStatus()));
        return panel;
    }

    private static String fullName(Object user) {
        try {
            var nameMethod = user.getClass().getMethod("getName");
            Object name = nameMethod.invoke(user);
            if (name == null) return "—";
            var firstMethod = name.getClass().getMethod("getFirstName");
            var lastMethod = name.getClass().getMethod("getLastName");
            String first = (String) firstMethod.invoke(name);
            String last = (String) lastMethod.invoke(name);
            return (first != null ? first : "") + " " + (last != null ? last : "");
        } catch (Exception e) {
            return "—";
        }
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

        JLabel valueComp = new JLabel(value != null && !value.isBlank() ? value : "—");
        valueComp.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        valueComp.setForeground(AppTheme.TEXT_PRIMARY);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(valueComp);
        return block;
    }

    private static JComponent statusField(String status) {
        JLabel value = new JLabel(status != null ? status : "—");
        value.setFont(FontManager.bodyFont(Font.BOLD, 13));
        value.setForeground(AppTheme.statusColor(status));

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComp = new JLabel("Account Status");
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(value);
        return block;
    }
}