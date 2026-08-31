package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;


public class CreateTicketDialog {

    public static void show(Component parent, Appointment appointment, Runnable onCreated) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Create Ticket", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 320);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        JLabel subtitle = new JLabel("<html>Creates a consultation ticket for this appointment. "
                + "The ticket starts as <b>OPEN</b> and the doctor will progress it from here.</html>");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));

        content.add(subtitle);
        content.add(readOnlyField("Patient", patientName(appointment)));
        content.add(readOnlyField("Doctor", doctorName(appointment)));
        content.add(readOnlyField("Appointment", appointmentDate(appointment)));

        JLabel descLabel = new JLabel("Ticket Description");
        descLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        descLabel.setForeground(AppTheme.TEXT_PRIMARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 4, 0));

        JTextArea descArea = new JTextArea(3, 20);
        descArea.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_SM, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));

        JButton cancel = new JButton("Cancel");
        cancel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> dialog.dispose());

        JButton create = new JButton("Create Ticket");
        create.setFont(FontManager.bodyFont(Font.BOLD, 13));
        create.setForeground(AppTheme.TEXT_ON_PRIMARY);
        create.setBackground(AppTheme.PRIMARY);
        create.setFocusPainted(false);
        create.setBorderPainted(false);
        create.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        create.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        create.addActionListener(e -> {
            String description = descArea.getText().trim();
            if (description.isEmpty()) {
                errorLabel.setText("Please enter a ticket description.");
                return;
            }

            PatientTicket ticket = new PatientTicket();
            ticket.setPatient(appointment.getPatient());
            ticket.setAppointment(appointment);
            ticket.setTicketDescription(description);
            ticket.setTicketCreatedDate(LocalDateTime.now());

            BaseApiClient.ApiResult<PatientTicket> result = ApiClientProvider.getInstance().patientTickets().create(ticket);

            if (result.isSuccess()) {
                dialog.dispose();
                AppDialog.show(parent, "Ticket Created",
                        "A consultation ticket has been created for this appointment.", AppDialog.Type.SUCCESS);
                if (onCreated != null) onCreated.run();
            } else {
                errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Unable to create ticket.");
            }
        });

        buttonRow.add(cancel);
        buttonRow.add(create);

        content.add(descLabel);
        content.add(descScroll);
        content.add(errorLabel);
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static JComponent readOnlyField(String label, String value) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_XS, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        valueComp.setForeground(AppTheme.TEXT_PRIMARY);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(valueComp);
        return block;
    }

    private static String patientName(Appointment appointment) {
        if (appointment.getPatient() == null || appointment.getPatient().getName() == null) return "—";
        String first = appointment.getPatient().getName().getFirstName();
        String last = appointment.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }

    private static String doctorName(Appointment appointment) {
        if (appointment.getDoctor() == null || appointment.getDoctor().getName() == null) return "—";
        String last = appointment.getDoctor().getName().getLastName();
        return "Dr. " + (last != null ? last : "—");
    }

    private static String appointmentDate(Appointment appointment) {
        if (appointment.getAppointmentDate() == null) return "—";
        return appointment.getAppointmentDate() + " " +
                (appointment.getAppointmentTime() != null ? appointment.getAppointmentTime() : "");
    }
}