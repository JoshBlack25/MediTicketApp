package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Doctor;
import za.ac.cput.model.domain.Name;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.auth.components.LabeledTextField;
import za.ac.cput.ui.clinicstaff.components.ChangePasswordDialog;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Mirrors the patient side's ProfilePage edit-in-place pattern. Email and
 * license number stay read-only (identity/credential fields, not
 * self-service), everything else — name, phone, specialty — can be edited.
 * Change Password reuses the clinic-staff dialog as-is since it's generic
 * over SessionManager's email + the shared auth().changePassword() call.
 */
public class ProfilePage extends JPanel {

    private Doctor doctor;

    private LabeledTextField firstNameField;
    private LabeledTextField lastNameField;
    private LabeledTextField emailField;
    private LabeledTextField phoneField;
    private LabeledTextField specialtyField;
    private LabeledTextField licenseField;
    private LabeledTextField dobField;

    private JButton editButton;
    private JButton saveButton;
    private JButton cancelButton;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    public ProfilePage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildFormCard());

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

        JLabel title = new JLabel("Profile");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Your personal and professional details.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildFormCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(560, Integer.MAX_VALUE));

        firstNameField = fieldRow(card, "First Name");
        lastNameField = fieldRow(card, "Last Name");
        emailField = fieldRow(card, "Email");
        phoneField = fieldRow(card, "Phone");
        specialtyField = fieldRow(card, "Specialty");
        licenseField = fieldRow(card, "License Number");
        dobField = fieldRow(card, "Date of Birth");

        // Email, license number, and DOB are identity/credential fields —
        // not something a doctor should be able to self-serve change here.
        emailField.getField().setEditable(false);
        licenseField.getField().setEditable(false);
        dobField.getField().setEditable(false);
        setEditable(false); // everything else starts read-only too, until "Edit" is clicked

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));

        editButton = actionButton("Edit Profile", AppTheme.PRIMARY, AppTheme.TEXT_ON_PRIMARY);
        editButton.addActionListener(e -> enterEditMode());

        saveButton = actionButton("Save Changes", AppTheme.PRIMARY, AppTheme.TEXT_ON_PRIMARY);
        saveButton.addActionListener(e -> saveChanges());
        saveButton.setVisible(false);

        cancelButton = actionButton("Cancel", AppTheme.SURFACE, AppTheme.TEXT_PRIMARY);
        cancelButton.addActionListener(e -> loadData()); // simplest "cancel": just re-fetch from server
        cancelButton.setVisible(false);

        JButton changePassword = actionButton("Change Password", AppTheme.SURFACE, AppTheme.TEXT_PRIMARY);
        changePassword.addActionListener(e -> ChangePasswordDialog.show(this));

        buttonRow.add(editButton);
        buttonRow.add(saveButton);
        buttonRow.add(cancelButton);
        buttonRow.add(changePassword);

        card.add(buttonRow);
        return card;
    }

    private LabeledTextField fieldRow(JPanel parent, String label) {
        LabeledTextField field = new LabeledTextField(label);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        parent.add(field);
        parent.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        return field;
    }

    private JButton actionButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(FontManager.bodyFont(Font.BOLD, 13));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setBorderPainted(background == AppTheme.SURFACE);
        button.setBorder(BorderFactory.createCompoundBorder(
                background == AppTheme.SURFACE ? BorderFactory.createLineBorder(AppTheme.BORDER, 1, true) : null,
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void setEditable(boolean editable) {
        firstNameField.getField().setEditable(editable);
        lastNameField.getField().setEditable(editable);
        phoneField.getField().setEditable(editable);
        specialtyField.getField().setEditable(editable);
    }

    private void enterEditMode() {
        setEditable(true);
        editButton.setVisible(false);
        saveButton.setVisible(true);
        cancelButton.setVisible(true);
    }

    private void exitEditMode() {
        setEditable(false);
        editButton.setVisible(true);
        saveButton.setVisible(false);
        cancelButton.setVisible(false);
    }

    private void loadData() {
        int doctorId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<Doctor> result = ApiClientProvider.getInstance().doctors().read(doctorId);

        if (result.isSuccess()) {
            doctor = result.getData();
            populateFields();
        }
        exitEditMode();
    }

    private void populateFields() {
        firstNameField.getField().setText(doctor.getName() != null ? doctor.getName().getFirstName() : "");
        lastNameField.getField().setText(doctor.getName() != null ? doctor.getName().getLastName() : "");
        emailField.getField().setText(doctor.getEmail());
        phoneField.getField().setText(doctor.getCellPhone());
        specialtyField.getField().setText(doctor.getSpecialty());
        licenseField.getField().setText(doctor.getLicenseNumber());
        dobField.getField().setText(doctor.getDob() != null ? doctor.getDob().format(DATE_FMT) : "\u2014");
    }

    private void saveChanges() {
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
            AppDialog.show(this, "Missing Information", "First and last name can't be empty.", AppDialog.Type.ERROR);
            return;
        }

        if (doctor.getName() == null) {
            doctor.setName(new Name());
        }
        doctor.getName().setFirstName(firstNameField.getText().trim());
        doctor.getName().setLastName(lastNameField.getText().trim());
        doctor.setCellPhone(phoneField.getText().trim());
        doctor.setSpecialty(specialtyField.getText().trim());

        BaseApiClient.ApiResult<Doctor> result = ApiClientProvider.getInstance().doctors().update(doctor);

        if (result.isSuccess()) {
            doctor = result.getData();
            populateFields();
            SessionManager.getInstance().setFullName(doctor.getName().getFullName());
            AppDialog.show(this, "Profile Updated", "Your changes have been saved.", AppDialog.Type.SUCCESS);
            exitEditMode();
        } else {
            AppDialog.show(this, "Update Failed",
                    result.getMessage() != null ? result.getMessage() : "Something went wrong.", AppDialog.Type.ERROR);
        }
    }
}