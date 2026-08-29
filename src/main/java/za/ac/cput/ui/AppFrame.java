package za.ac.cput.ui;

import za.ac.cput.ui.auth.*;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.ImageManager;

import javax.swing.*;
import java.awt.*;


public class AppFrame extends JFrame {

    public static final String SCREEN_LOGIN = "LOGIN";
    public static final String SCREEN_PATIENT_SIGNUP = "PATIENT_SIGNUP";
    public static final String SCREEN_ACCESS_REQUEST = "ACCESS_REQUEST";
    public static final String SCREEN_REQUEST_SUBMITTED = "REQUEST_SUBMITTED";
    public static final String SCREEN_VERIFY_INVITE = "VERIFY_INVITE";
    public static final String SCREEN_DOCTOR_SIGNUP = "DOCTOR_SIGNUP";
    public static final String SCREEN_CLINICSTAFF_SIGNUP = "CLINICSTAFF_SIGNUP";
    public static final String SCREEN_EMPLOYEE_SIGNUP_SUCCESS = "EMPLOYEE_SIGNUP_SUCCESS";
    public static final String SCREEN_ADMIN_DASHBOARD = "ADMIN_DASHBOARD";
    public static final String SCREEN_FORGOT_PASSWORD = "FORGOT_PASSWORD";
    public static final String SCREEN_VERIFY_RESET_CODE = "VERIFY_RESET_CODE";
    public static final String SCREEN_NEW_PASSWORD = "NEW_PASSWORD";
    public static final String SCREEN_DOCTOR_DASHBOARD = "DOCTOR_DASHBOARD";
    public static final String SCREEN_PATIENT_DASHBOARD = "PATIENT_DASHBOARD";
    public static final String SCREEN_NURSE_DASHBOARD = "NURSE_DASHBOARD";
    public static final String SCREEN_SIGNUP_VERIFY = "SIGNUP_VERIFY";


    private final CardLayout cardLayout;
    private final JPanel contentContainer;

    private DoctorSignupPanel doctorSignupPanel;
    private ClinicStaffSignupPanel clinicStaffSignupPanel;

    private VerifyResetCodePanel verifyResetCodePanel;
    private NewPasswordPanel newPasswordPanel;
    private SignupVerifyCodePanel signupVerifyCodePanel;

    public AppFrame() {
        super("MediTicket");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null); // center on screen

        ImageManager.getIcon(ImageManager.LOGO_ICON, 64, 64);
        setIconImage(ImageManager.getImage(ImageManager.LOGO_ICON));

        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);
        contentContainer.setBackground(AppTheme.BACKGROUND);

        setContentPane(contentContainer);

        registerScreens();
        showScreen(SCREEN_LOGIN);
    }


    private void registerScreens() {
        contentContainer.add(new LoginPanel(this), SCREEN_LOGIN);
        contentContainer.add(new PatientSignupPanel(this), SCREEN_PATIENT_SIGNUP);
        contentContainer.add(new EmployeeAccessRequestPanel(this), SCREEN_ACCESS_REQUEST);
        contentContainer.add(new RequestSubmittedPanel(this), SCREEN_REQUEST_SUBMITTED);
        contentContainer.add(new VerifyInviteCodePanel(this), SCREEN_VERIFY_INVITE);

        doctorSignupPanel = new DoctorSignupPanel(this);
        clinicStaffSignupPanel = new ClinicStaffSignupPanel(this);
        contentContainer.add(doctorSignupPanel, SCREEN_DOCTOR_SIGNUP);
        contentContainer.add(clinicStaffSignupPanel, SCREEN_CLINICSTAFF_SIGNUP);
        contentContainer.add(new EmployeeSignupSuccessPanel(this), SCREEN_EMPLOYEE_SIGNUP_SUCCESS);

        contentContainer.add(new ForgotPasswordPanel(this), SCREEN_FORGOT_PASSWORD);
        verifyResetCodePanel = new VerifyResetCodePanel(this);
        contentContainer.add(verifyResetCodePanel, SCREEN_VERIFY_RESET_CODE);
        newPasswordPanel = new NewPasswordPanel(this);
        contentContainer.add(newPasswordPanel, SCREEN_NEW_PASSWORD);

        signupVerifyCodePanel = new SignupVerifyCodePanel(this);
        contentContainer.add(signupVerifyCodePanel, SCREEN_SIGNUP_VERIFY);
    }


    public void showScreen(String screenKey) {
        cardLayout.show(contentContainer, screenKey);
    }

    public DoctorSignupPanel getDoctorSignupPanel() { return doctorSignupPanel; }
    public ClinicStaffSignupPanel getClinicStaffSignupPanel() { return clinicStaffSignupPanel; }
    public VerifyResetCodePanel getVerifyResetCodePanel() { return verifyResetCodePanel; }
    public NewPasswordPanel getNewPasswordPanel() { return newPasswordPanel; }
    public SignupVerifyCodePanel getSignupVerifyCodePanel() { return signupVerifyCodePanel; }


    public void addScreen(String screenKey, JComponent screen) {
        contentContainer.add(screen, screenKey);
    }

    private JComponent buildPlaceholder(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(AppTheme.BACKGROUND);
        JLabel label = new JLabel(message);
        label.setFont(za.ac.cput.ui.theme.FontManager.bodyFont(Font.PLAIN, 16));
        label.setForeground(AppTheme.TEXT_SECONDARY);
        panel.add(label);
        return panel;
    }
}