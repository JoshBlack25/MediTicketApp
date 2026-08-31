package za.ac.cput.ui.clinicstaff.components;
//JOSHUA REID ADAMS - 230317693
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Doctor;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.auth.components.PrimaryButton;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;


public class ApproveAppointmentDialog {

    public static void show(Component parent, Appointment appointment, Runnable onApproved) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Approve Appointment", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 260);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        JLabel subtitle = new JLabel("<html>Select the doctor to assign to this appointment. "
                + "The patient and clinic will be notified once approved.</html>");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));

        JLabel doctorLabel = new JLabel("Doctor");
        doctorLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        doctorLabel.setForeground(AppTheme.TEXT_PRIMARY);
        doctorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<Doctor> doctorCombo = new JComboBox<>();
        doctorCombo.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        doctorCombo.setPreferredSize(new Dimension(0, 42));
        doctorCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        doctorCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        doctorCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Doctor doctor) {
                    String name = doctor.getName() != null
                            ? "Dr. " + safe(doctor.getName().getFirstName()) + " " + safe(doctor.getName().getLastName())
                            : "Doctor #" + doctor.getUserId();
                    setText(name + (doctor.getSpecialty() != null ? " — " + doctor.getSpecialty() : ""));
                }
                return this;
            }
        });

        BaseApiClient.ApiResult<List<Doctor>> doctorResult = ApiClientProvider.getInstance().doctors().getAll();
        if (doctorResult.isSuccess()) {
            for (Doctor doctor : doctorResult.getData()) {
                doctorCombo.addItem(doctor);
            }
        }

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_SM, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        buttonRow.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));

        JButton cancel = new JButton("Cancel");
        cancel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> dialog.dispose());

        PrimaryButton confirm = new PrimaryButton("Approve Appointment");
        confirm.setPreferredSize(new Dimension(200, 42));
        confirm.addActionListener(e -> {
            Doctor selected = (Doctor) doctorCombo.getSelectedItem();
            if (selected == null) {
                errorLabel.setText("Please select a doctor.");
                return;
            }

            int staffId = SessionManager.getInstance().getUserId();
            BaseApiClient.ApiResult<Appointment> result = ApiClientProvider.getInstance()
                    .appointments().approve(appointment.getAppointmentId(), selected.getUserId(), staffId);

            if (result.isSuccess()) {
                dialog.dispose();
                AppDialog.show(parent, "Appointment Approved",
                        "The appointment has been confirmed and assigned to the selected doctor.", AppDialog.Type.SUCCESS);
                if (onApproved != null) onApproved.run();
            } else {
                errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Unable to approve appointment.");
            }
        });

        buttonRow.add(cancel);
        buttonRow.add(confirm);

        content.add(subtitle);
        content.add(doctorLabel);
        content.add(doctorCombo);
        content.add(errorLabel);
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static String safe(String s) { return s != null ? s : ""; }
}