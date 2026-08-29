package za.ac.cput.ui.auth;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.AuthResponse;
import za.ac.cput.model.auth.LoginRequest;
import za.ac.cput.security.JwtUtil;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.auth.components.LabeledPasswordField;
import za.ac.cput.ui.auth.components.LabeledTextField;
import za.ac.cput.ui.auth.components.PrimaryButton;
import za.ac.cput.ui.clinicstaff.admin.AdminDashboard;
import za.ac.cput.ui.clinicstaff.nurse.NurseDashboard;
import za.ac.cput.ui.doctor.DoctorDashboard;
import za.ac.cput.ui.patient.PatientDashboard;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;
import za.ac.cput.ui.theme.ImageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class LoginPanel extends JPanel {

    private final AppFrame appFrame;

    private LabeledTextField emailField;
    private LabeledPasswordField passwordField;
    private JLabel errorLabel;
    private PrimaryButton signInButton;

    public LoginPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new GridLayout(1, 2));
        add(buildHeroSection());
        add(buildFormSection());
    }



    private JComponent buildHeroSection() {
        JPanel hero = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image bg = ImageManager.getImage("hero/herobg.jpg");
                if (bg != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                    // Dark overlay for logo/text contrast against the photo
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            }
        };

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel(ImageManager.getIcon(ImageManager.LOGO_WHITE, 260, -1));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel slogan = new JLabel("Care, Coordinated.");
        slogan.setFont(FontManager.headlineFont(Font.BOLD, 22));
        slogan.setForeground(Color.WHITE);
        slogan.setAlignmentX(Component.CENTER_ALIGNMENT);
        slogan.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));

        content.add(logo);
        content.add(slogan);

        hero.add(content);
        return hero;
    }



    private JComponent buildFormSection() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(AppTheme.SURFACE);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(380, 420));

        JLabel title = new JLabel("Welcome Back");
        title.setFont(FontManager.headlineFont(Font.BOLD, 28));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Access your personalized healthcare suite or dashboard.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, AppTheme.SPACE_LG, 0));

        emailField = new LabeledTextField("Email");
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        passwordField = new LabeledPasswordField("Password");
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        passwordField.onForgotPasswordClick(this::onForgotPassword);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        signInButton = new PrimaryButton("Sign In");
        signInButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        signInButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        signInButton.addActionListener(e -> onSignIn());

        JLabel patientSignup = buildLinkRow("New patient?", "Sign up", this::onPatientSignup);
        JLabel staffSignup = buildLinkRow("Clinic professional?", "Signup request", this::onStaffSignupRequest);

        form.add(title);
        form.add(subtitle);
        form.add(emailField);
        form.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        form.add(signInButton);
        form.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        form.add(patientSignup);
        form.add(Box.createVerticalStrut(AppTheme.SPACE_XS));
        form.add(staffSignup);

        wrapper.add(form);
        return wrapper;
    }


    private JLabel buildLinkRow(String prefix, String linkText, Runnable onClick) {
        JLabel combined = new JLabel(
                "<html>" + prefix + " <span style='color:#0E7C86;font-weight:bold;'>" + linkText + "</span></html>"
        );
        combined.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        combined.setForeground(AppTheme.TEXT_SECONDARY);
        combined.setAlignmentX(Component.LEFT_ALIGNMENT);
        combined.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combined.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return combined;
    }


    private void onSignIn() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        errorLabel.setText(" ");
        signInButton.setEnabled(false);
        signInButton.setText("Signing In...");

        SwingWorker<BaseApiClient.ApiResult<AuthResponse>, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected BaseApiClient.ApiResult<AuthResponse> doInBackground() {
                        // Runs off the EDT — safe to block here.
                        return ApiClientProvider.getInstance().auth().login(new LoginRequest(email, password));
                    }

                    @Override
                    protected void done() {
                        // Back on the EDT automatically — safe to touch Swing components here.
                        signInButton.setEnabled(true);
                        signInButton.setText("Sign In");

                        BaseApiClient.ApiResult<AuthResponse> result;
                        try {
                            result = get();
                        } catch (Exception e) {
                            errorLabel.setText("Something went wrong. Please try again.");
                            return;
                        }

                        handleLoginResult(result, email);
                    }
                };

        worker.execute();
    }

    private void handleLoginResult(BaseApiClient.ApiResult<AuthResponse> result, String email) {
        if (!result.isSuccess()) {
            errorLabel.setText(result.getMessage() != null
                    ? "Invalid email or password."
                    : "Could not reach the server.");
            return;
        }

        AuthResponse auth = result.getData();
        String token = auth.getAccessToken();

        SessionManager session = SessionManager.getInstance();
        session.setAccessToken(token);
        session.setRefreshToken(auth.getRefreshToken());
        session.setUserId(JwtUtil.extractUserId(token));
        session.setUserType(JwtUtil.extractUserType(token));
        session.setStaffRole(JwtUtil.extractStaffRole(token));
        session.setEmail(email);

        ApiClientProvider.getInstance().getBaseApiClient().setAuthToken(token);


        if ("CLINIC_STAFF".equals(session.getUserType())) {
            var staff = ApiClientProvider.getInstance().clinicStaff().findByEmail(session.getEmail());
            if (staff.isSuccess()) session.setFullName(staff.getData().getName().getFullName());
        } else if ("PATIENT".equals(session.getUserType())) {
            var patient = ApiClientProvider.getInstance().patients().findByEmail(session.getEmail());
            if (patient.isSuccess()) session.setFullName(patient.getData().getName().getFullName());
        }


        if (session.isAdmin()) {
            AdminDashboard dashboard = new AdminDashboard(appFrame);
            appFrame.addScreen(AppFrame.SCREEN_ADMIN_DASHBOARD, dashboard);
            appFrame.showScreen(AppFrame.SCREEN_ADMIN_DASHBOARD);
        } else if ("DOCTOR".equals(session.getUserType())) {
            DoctorDashboard dashboard = new DoctorDashboard(appFrame);
            appFrame.addScreen(AppFrame.SCREEN_DOCTOR_DASHBOARD, dashboard);
            appFrame.showScreen(AppFrame.SCREEN_DOCTOR_DASHBOARD);
        } else if ("PATIENT".equals(session.getUserType())) {
            PatientDashboard dashboard = new PatientDashboard(appFrame);
            appFrame.addScreen(AppFrame.SCREEN_PATIENT_DASHBOARD, dashboard);
            appFrame.showScreen(AppFrame.SCREEN_PATIENT_DASHBOARD);
        } else if ("CLINIC_STAFF".equals(session.getUserType())) {
            // NURSE — the only non-admin CLINIC_STAFF role right now.
            NurseDashboard dashboard = new NurseDashboard(appFrame);
            appFrame.addScreen(AppFrame.SCREEN_NURSE_DASHBOARD, dashboard);
            appFrame.showScreen(AppFrame.SCREEN_NURSE_DASHBOARD);
        } else {
            JOptionPane.showMessageDialog(this, "Logged in as " + session.getUserType()
                    + " — dashboard not built yet.", "Login OK", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onForgotPassword() {
        appFrame.showScreen(AppFrame.SCREEN_FORGOT_PASSWORD);
    }

    private void onPatientSignup() {
        appFrame.showScreen(AppFrame.SCREEN_PATIENT_SIGNUP);
    }

    private void onStaffSignupRequest() {
        appFrame.showScreen(AppFrame.SCREEN_ACCESS_REQUEST);
    }
}