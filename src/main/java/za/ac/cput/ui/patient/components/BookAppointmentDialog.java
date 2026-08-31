package za.ac.cput.ui.patient.components;
//JOSHUA REID ADAMS - 230317693
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;


public class BookAppointmentDialog {

    public static void show(Component parent, Runnable onBooked) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Book New Appointment", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(460, 480);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        JLabel dateLabel = fieldLabel("Preferred Date");

        DatePickerSettings dateSettings = new DatePickerSettings();
        DatePicker datePicker = new DatePicker(dateSettings);
        dateSettings.setDateRangeLimits(LocalDate.now(), null); // must come AFTER constructing the DatePicker
        datePicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        datePicker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel timeLabel = fieldLabel("Preferred Time");

        TimePickerSettings timeSettings = new TimePickerSettings();
        TimePicker timePicker = new TimePicker(timeSettings);
        timePicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        timePicker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel reasonLabel = fieldLabel("Reason for Visit");

        JTextArea reasonArea = new JTextArea(4, 20);
        reasonArea.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        reasonScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        reasonScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel infoNote = new JLabel("<html><i>A nurse or admin will review your request "
                + "and confirm a doctor and appointment time shortly.</i></html>");
        infoNote.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        infoNote.setForeground(AppTheme.TEXT_MUTED);
        infoNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoNote.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));

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

        JButton submit = new JButton("Request Appointment");
        submit.setFont(FontManager.bodyFont(Font.BOLD, 13));
        submit.setForeground(AppTheme.TEXT_ON_PRIMARY);
        submit.setBackground(AppTheme.PRIMARY);
        submit.setFocusPainted(false);
        submit.setBorderPainted(false);
        submit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submit.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        submit.addActionListener(e -> {
            LocalDate date = datePicker.getDate();
            LocalTime time = timePicker.getTime();
            String reason = reasonArea.getText().trim();

            if (date == null) {
                errorLabel.setText("Please select a date.");
                return;
            }
            if (time == null) {
                errorLabel.setText("Please select a time.");
                return;
            }
            if (reason.isEmpty()) {
                errorLabel.setText("Please enter a reason for your visit.");
                return;
            }
            errorLabel.setText(" ");

            Appointment appointment = new Appointment();
            appointment.setAppointmentDate(date);
            appointment.setAppointmentTime(time);
            appointment.setReason(reason);
            appointment.setConfirmationStatus("PENDING");

            Patient self = new Patient();
            self.setUserId(SessionManager.getInstance().getUserId());
            appointment.setPatient(self);

            BaseApiClient.ApiResult<Appointment> result =
                    ApiClientProvider.getInstance().appointments().create(appointment);

            if (result.isSuccess()) {
                dialog.dispose();
                AppDialog.show(parent, "Appointment Requested",
                        "Your appointment request has been submitted for review.", AppDialog.Type.SUCCESS);
                if (onBooked != null) onBooked.run();
            } else {
                errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Unable to submit request.");
            }
        });

        buttonRow.add(cancel);
        buttonRow.add(submit);

        content.add(dateLabel);
        content.add(datePicker);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(timeLabel);
        content.add(timePicker);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(reasonLabel);
        content.add(reasonScroll);
        content.add(infoNote);
        content.add(errorLabel);
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.bodyFont(Font.BOLD, 12));
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return label;
    }
}