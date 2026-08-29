package za.ac.cput.ui.auth;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.DoctorSignupRequest;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.auth.components.LabeledTextField;
import za.ac.cput.ui.auth.components.PrimaryButton;
import za.ac.cput.ui.auth.components.ToggleablePasswordField;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DoctorSignupPanel extends JPanel {

    private final AppFrame appFrame;
    private String inviteToken;
    private JLabel emailValueLabel;

    private LabeledTextField firstName, middleName, lastName, cellPhone, dob, specialty, licenseNumber;
    private ToggleablePasswordField password, confirmPassword;
    private JLabel errorLabel;

    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DoctorSignupPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new GridBagLayout());
        setBackground(AppTheme.BACKGROUND);
        add(buildCard());
    }

    public void prefill(String token, String email) {
        this.inviteToken = token;
        emailValueLabel.setText(email);
    }

    private JComponent buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL)
        ));
        card.setPreferredSize(new Dimension(780, 640));

        JLabel eyebrow = new JLabel("PROFESSIONAL ENROLLMENT");
        eyebrow.setFont(FontManager.bodyFont(Font.BOLD, 12));
        eyebrow.setForeground(AppTheme.PRIMARY);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Complete Your Professional Profile");
        title.setFont(FontManager.headlineFont(Font.BOLD, 28));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel emailRow = new JLabel("Signing up as:");
        emailRow.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        emailRow.setForeground(AppTheme.TEXT_SECONDARY);
        emailRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailRow.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        emailValueLabel = new JLabel("—");
        emailValueLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        emailValueLabel.setForeground(AppTheme.PRIMARY);
        emailValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailValueLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_LG, 0));

        firstName = fieldOf("First Name", "e.g. Julian");
        middleName = fieldOf("Middle Name (optional)", "e.g. Alexander");
        lastName = fieldOf("Last Name", "e.g. Vance");
        cellPhone = fieldOf("Cell Phone", "+27 (0) 00 000 0000");
        dob = fieldOf("Date of Birth (yyyy-mm-dd)", "1985-03-20");
        specialty = fieldOf("Specialty", "e.g. Cardiology");
        licenseNumber = fieldOf("License Number", "e.g. MED-9988776655");

        password = new ToggleablePasswordField("Password");
        confirmPassword = new ToggleablePasswordField("Confirm Password");

        errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton completeButton = new PrimaryButton("Complete Registration");
        completeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        completeButton.setMaximumSize(new Dimension(280, 46));
        completeButton.addActionListener(e -> onComplete());

        card.add(eyebrow);
        card.add(title);
        card.add(emailRow);
        card.add(emailValueLabel);
        card.add(row(firstName, middleName, lastName));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(row(cellPhone, dob, specialty));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(row(licenseNumber));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(row(password, confirmPassword));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(completeButton);

        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(800, 660));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private LabeledTextField fieldOf(String label, String placeholder) {
        LabeledTextField f = new LabeledTextField(label);
        f.getField().putClientProperty("JTextField.placeholderText", placeholder);
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private JPanel row(JComponent... fields) {
        JPanel row = new JPanel(new GridLayout(1, fields.length, AppTheme.SPACE_MD, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        for (JComponent f : fields) row.add(f);
        return row;
    }

    private void onComplete() {
        String pwd = new String(password.getPassword());
        String confirmPwd = new String(confirmPassword.getPassword());

        if (firstName.getText().isBlank() || lastName.getText().isBlank()
                || licenseNumber.getText().isBlank() || pwd.isBlank()) {
            errorLabel.setText("Please fill in all required fields.");
            return;
        }
        if (!pwd.equals(confirmPwd)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        LocalDate parsedDob;
        try {
            parsedDob = LocalDate.parse(dob.getText().trim(), DOB_FORMAT);
        } catch (DateTimeParseException ex) {
            errorLabel.setText("Date of birth must be in yyyy-mm-dd format.");
            return;
        }

        errorLabel.setText(" ");

        DoctorSignupRequest request = new DoctorSignupRequest();
        request.setToken(inviteToken);
        request.setFirstName(firstName.getText().trim());
        request.setMiddleName(middleName.getText().trim());
        request.setLastName(lastName.getText().trim());
        request.setCellPhone(cellPhone.getText().trim());
        request.setPassword(pwd);
        request.setDob(parsedDob);
        request.setSpecialty(specialty.getText().trim());
        request.setLicenseNumber(licenseNumber.getText().trim());

        BaseApiClient.ApiResult<String> result = ApiClientProvider.getInstance().auth().signupDoctor(request);

        if (result.isSuccess()) {
            String signedUpEmail = emailValueLabel.getText();
            clearForm();
            appFrame.getSignupVerifyCodePanel().setEmail(signedUpEmail);
            appFrame.showScreen(AppFrame.SCREEN_SIGNUP_VERIFY);
        } else {
            errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Signup failed.");
        }
    }

    private void clearForm() {
        firstName.getField().setText("");
        middleName.getField().setText("");
        lastName.getField().setText("");
        cellPhone.getField().setText("");
        dob.getField().setText("");
        specialty.getField().setText("");
        licenseNumber.getField().setText("");
        password.clear();
        confirmPassword.clear();
        errorLabel.setText(" ");
    }
}