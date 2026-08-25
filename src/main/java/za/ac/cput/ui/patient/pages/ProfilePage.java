package za.ac.cput.ui.patient.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.auth.components.LabeledTextField;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ProfilePage extends JPanel {

    private Patient patient;

    private LabeledTextField firstNameField;
    private LabeledTextField lastNameField;
    private LabeledTextField emailField;
    private LabeledTextField phoneField;
    private LabeledTextField emergencyContactField;
    private LabeledTextField dobField;
    private LabeledTextField registeredField;

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

        JLabel subtitle = new JLabel("Your personal and contact details.");
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
        emergencyContactField = fieldRow(card, "Emergency Contact");
        dobField = fieldRow(card, "Date of Birth");
        registeredField = fieldRow(card, "Patient Since");

        // Email, DOB, and registration date are never editable — they're
        // identity/system fields, not things a patient should self-serve
        // change from a profile form.
        emailField.getField().setEditable(false);
        dobField.getField().setEditable(false);
        registeredField.getField().setEditable(false);
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

        buttonRow.add(editButton);
        buttonRow.add(saveButton);
        buttonRow.add(cancelButton);

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
        emergencyContactField.getField().setEditable(editable);
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
        int patientId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<Patient> result = ApiClientProvider.getInstance().patients().read(patientId);

        if (result.isSuccess()) {
            patient = result.getData();
            populateFields();
        }
        exitEditMode();
    }

    private void populateFields() {
        firstNameField.getField().setText(patient.getName() != null ? patient.getName().getFirstName() : "");
        lastNameField.getField().setText(patient.getName() != null ? patient.getName().getLastName() : "");
        emailField.getField().setText(patient.getEmail());
        phoneField.getField().setText(patient.getCellPhone());
        emergencyContactField.getField().setText(patient.getEmergencyContact());
        dobField.getField().setText(patient.getDob() != null ? patient.getDob().format(DATE_FMT) : "—");
        registeredField.getField().setText(patient.getDateRegistered() != null ? patient.getDateRegistered().format(DATE_FMT) : "—");
    }

    private void saveChanges() {
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
            AppDialog.show(this, "Missing Information", "First and last name can't be empty.", AppDialog.Type.ERROR);
            return;
        }


        if (patient.getName() == null) {
            patient.setName(new za.ac.cput.model.domain.Name());
        }
        patient.getName().setFirstName(firstNameField.getText().trim());
        patient.getName().setLastName(lastNameField.getText().trim());
        patient.setCellPhone(phoneField.getText().trim());
        patient.setEmergencyContact(emergencyContactField.getText().trim());

        BaseApiClient.ApiResult<Patient> result = ApiClientProvider.getInstance().patients().update(patient);

        if (result.isSuccess()) {
            patient = result.getData();
            populateFields();
            SessionManager.getInstance().setFullName(patient.getName().getFullName());
            AppDialog.show(this, "Profile Updated", "Your changes have been saved.", AppDialog.Type.SUCCESS);
            exitEditMode();
        } else {
            AppDialog.show(this, "Update Failed",
                    result.getMessage() != null ? result.getMessage() : "Something went wrong.", AppDialog.Type.ERROR);
        }
    }
}