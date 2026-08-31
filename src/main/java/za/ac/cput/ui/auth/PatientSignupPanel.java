package za.ac.cput.ui.auth;
//AIDAN BARENDS - 230155639
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.PatientSignupRequest;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.auth.components.LabeledTextField;
import za.ac.cput.ui.auth.components.PrimaryButton;
import za.ac.cput.ui.auth.components.ToggleablePasswordField;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;
import za.ac.cput.ui.theme.ImageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class PatientSignupPanel extends JPanel {

    private final AppFrame appFrame;

    private LabeledTextField firstName, middleName, lastName, email, cellPhone, dob, emergencyContact;
    private ToggleablePasswordField password, confirmPassword;
    private JLabel errorLabel;
    private PrimaryButton createAccount;

    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PatientSignupPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildFormCard(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_XL, AppTheme.SPACE_LG, AppTheme.SPACE_XL));

        JLabel logo = new JLabel(ImageManager.getIcon(ImageManager.LOGO_PRIMARY, -1, 44));
        header.add(logo, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_LG, 0));
        right.setOpaque(false);

        JLabel already = new JLabel("ALREADY HAVE AN ACCOUNT?");
        already.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        already.setForeground(AppTheme.TEXT_MUTED);

        JLabel signIn = tealLink("Sign In", () -> appFrame.showScreen(AppFrame.SCREEN_LOGIN));
        JLabel staffRequest = tealLink("Employee Signup Request", () -> appFrame.showScreen(AppFrame.SCREEN_ACCESS_REQUEST));

        right.add(already);
        right.add(signIn);
        right.add(staffRequest);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JLabel tealLink(String text, Runnable onClick) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.bodyFont(Font.BOLD, 13));
        label.setForeground(AppTheme.PRIMARY);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return label;
    }

    private JComponent buildFormCard() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(AppTheme.BACKGROUND);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL)
        ));
        card.setPreferredSize(new Dimension(760, 720));

        JLabel title = new JLabel("Join Our Elite Network");
        title.setFont(FontManager.headlineFont(Font.BOLD, 30));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Experience healthcare with unparalleled care and precision.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, AppTheme.SPACE_LG, 0));

        firstName = fieldOf("First Name", "e.g. Alexander");
        middleName = fieldOf("Middle Name (optional)", "e.g. James");
        lastName = fieldOf("Last Name", "e.g. Sterling");
        email = fieldOf("Email Address", "alexander@example.com");
        cellPhone = fieldOf("Cell Phone Number", "+27 (0) 00 000 0000");
        dob = fieldOf("Date of Birth (yyyy-mm-dd)", "1990-05-14");
        emergencyContact = fieldOf("Emergency Contact (Name & Phone)", "e.g. Sarah Sterling - +27 (0) 00 000 0000");

        password = new ToggleablePasswordField("Password");
        confirmPassword = new ToggleablePasswordField("Confirm Password");

        errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        createAccount = new PrimaryButton("Create Account");
        createAccount.setAlignmentX(Component.LEFT_ALIGNMENT);
        createAccount.setMaximumSize(new Dimension(240, 46));
        createAccount.addActionListener(e -> onCreateAccount());

        card.add(title);
        card.add(subtitle);
        card.add(row(firstName, middleName, lastName));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(row(email, cellPhone));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(row(dob));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(row(emergencyContact));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(row(password, confirmPassword));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(createAccount);

        outer.add(card);
        JScrollPane scroll = new JScrollPane(outer);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
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

    private JComponent buildFooter() {
        JLabel tagline = new JLabel("CRAFTED FOR THE PURSUIT OF LONGEVITY", SwingConstants.CENTER);
        tagline.setFont(FontManager.bodyFont(Font.PLAIN, 11));
        tagline.setForeground(AppTheme.TEXT_MUTED);
        tagline.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, 0, AppTheme.SPACE_LG, 0));
        return tagline;
    }


    private void onCreateAccount() {
        String pwd = new String(password.getPassword());
        String confirmPwd = new String(confirmPassword.getPassword());

        if (firstName.getText().isBlank() || lastName.getText().isBlank()
                || email.getText().isBlank() || pwd.isBlank()) {
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
        createAccount.setEnabled(false);
        createAccount.setText("Creating Account...");

        PatientSignupRequest request = new PatientSignupRequest(
                firstName.getText().trim(),
                middleName.getText().trim(),
                lastName.getText().trim(),
                email.getText().trim(),
                cellPhone.getText().trim(),
                pwd,
                parsedDob,
                LocalDate.now(),
                emergencyContact.getText().trim()
        );

        SwingWorker<BaseApiClient.ApiResult<String>, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected BaseApiClient.ApiResult<String> doInBackground() {
                        // Runs off the EDT — safe to block here.
                        return ApiClientProvider.getInstance().auth().signup(request);
                    }

                    @Override
                    protected void done() {
                        // Back on the EDT automatically — safe to touch Swing components here.
                        createAccount.setEnabled(true);
                        createAccount.setText("Create Account");

                        BaseApiClient.ApiResult<String> result;
                        try {
                            result = get();
                        } catch (Exception e) {
                            errorLabel.setText("Something went wrong. Please try again.");
                            return;
                        }

                        if (result.isSuccess()) {
                            clearForm();
                            AppDialog.show(PatientSignupPanel.this, "Account Created",
                                    "Your account has been created successfully.\nPlease check your email and click the verification link before logging in.",
                                    AppDialog.Type.SUCCESS);
                            appFrame.showScreen(AppFrame.SCREEN_LOGIN);
                        } else {
                            errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Signup failed.");
                        }
                    }
                };

        worker.execute();
    }

    private void clearForm() {
        firstName.getField().setText("");
        middleName.getField().setText("");
        lastName.getField().setText("");
        email.getField().setText("");
        cellPhone.getField().setText("");
        dob.getField().setText("");
        emergencyContact.getField().setText("");
        password.clear();
        confirmPassword.clear();
        errorLabel.setText(" ");
    }
}