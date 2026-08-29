package za.ac.cput.ui.doctor.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;


public class CompleteConsultationDialog {

    public static void show(Component parent, PatientTicket ticket, Runnable onCompleted) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Complete Consultation", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 340);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        JLabel subtitle = new JLabel("<html>Record a brief diagnosis or consultation summary. "
                + "This marks the ticket resolved and the appointment complete — "
                + "the patient will then be billed by clinic staff.</html>");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));

        JLabel notesLabel = new JLabel("Diagnosis / Summary");
        notesLabel.setFont(FontManager.bodyFont(Font.BOLD, 12));
        notesLabel.setForeground(AppTheme.TEXT_PRIMARY);
        notesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JTextArea notesArea = new JTextArea(6, 20);
        notesArea.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

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

        JButton submit = new JButton("Complete Consultation");
        submit.setFont(FontManager.bodyFont(Font.BOLD, 13));
        submit.setForeground(AppTheme.TEXT_ON_PRIMARY);
        submit.setBackground(AppTheme.PRIMARY);
        submit.setFocusPainted(false);
        submit.setBorderPainted(false);
        submit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submit.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        submit.addActionListener(e -> {
            String notes = notesArea.getText().trim();
            if (notes.isEmpty()) {
                errorLabel.setText("Please enter a diagnosis or summary before completing.");
                return;
            }

            BaseApiClient.ApiResult<PatientTicket> result = ApiClientProvider.getInstance()
                    .patientTickets().progressStatus(ticket.getTicketId(), "RESOLVED", notes);

            if (result.isSuccess()) {
                dialog.dispose();
                AppDialog.show(parent, "Consultation Completed",
                        "The ticket has been resolved and the appointment marked complete.", AppDialog.Type.SUCCESS);
                if (onCompleted != null) onCompleted.run();
            } else {
                errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Unable to complete consultation.");
            }
        });

        buttonRow.add(cancel);
        buttonRow.add(submit);

        content.add(subtitle);
        content.add(notesLabel);
        content.add(notesScroll);
        content.add(errorLabel);
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }
}