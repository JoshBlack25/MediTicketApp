package za.ac.cput.ui.auth;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.ForgotPasswordRequest;
import za.ac.cput.model.auth.VerifyResetCodeRequest;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.auth.components.LabeledTextField;
import za.ac.cput.ui.auth.components.PrimaryButton;
import za.ac.cput.ui.theme.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class VerifyResetCodePanel extends JPanel {

    private final AppFrame appFrame;
    private String email;

    private JLabel emailLabel;
    private LabeledTextField codeField;
    private JLabel errorLabel;

    public VerifyResetCodePanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new GridBagLayout());
        setBackground(AppTheme.BACKGROUND);
        add(buildCard());
    }

    public void setEmail(String email) {
        this.email = email;
        if (emailLabel != null) {
            emailLabel.setText("Code sent to " + email);
        }
        if (codeField != null) {
            codeField.getField().setText("");
        }
    }

    private JComponent buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL)
        ));
        card.setPreferredSize(new Dimension(440, 340));

        JLabel title = new JLabel("Check Your Email");
        title.setFont(FontManager.headlineFont(Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        emailLabel = new JLabel("Code sent to your email");
        emailLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        emailLabel.setForeground(AppTheme.TEXT_SECONDARY);
        emailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emailLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_LG, 0));

        codeField = new LabeledTextField("6-Digit Code");
        codeField.setAlignmentX(Component.CENTER_ALIGNMENT);
        codeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        codeField.getField().putClientProperty("JTextField.placeholderText", "e.g. 482913");

        errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        PrimaryButton verifyButton = new PrimaryButton("Verify Code");
        verifyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        verifyButton.addActionListener(e -> onVerify());

        JLabel resend = new JLabel("Didn't receive it? Resend code");
        resend.setFont(FontManager.bodyFont(Font.BOLD, 13));
        resend.setForeground(AppTheme.PRIMARY);
        resend.setAlignmentX(Component.CENTER_ALIGNMENT);
        resend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resend.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));
        resend.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { onResend(); }
        });

        JLabel backToLogin = new JLabel("← Back to Login");
        backToLogin.setFont(FontManager.bodyFont(Font.BOLD, 13));
        backToLogin.setForeground(AppTheme.TEXT_SECONDARY);
        backToLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        backToLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToLogin.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));
        backToLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { appFrame.showScreen(AppFrame.SCREEN_LOGIN); }
        });

        card.add(title);
        card.add(emailLabel);
        card.add(codeField);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(verifyButton);
        card.add(resend);
        card.add(backToLogin);

        return card;
    }

    private void onVerify() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            errorLabel.setText("Please enter the code.");
            return;
        }
        errorLabel.setText(" ");

        BaseApiClient.ApiResult<String> result = ApiClientProvider.getInstance()
                .auth().verifyResetCode(new VerifyResetCodeRequest(email, code));

        if (result.isSuccess()) {
            String sessionToken = result.getData();
            appFrame.getNewPasswordPanel().setResetContext(email, sessionToken);
            appFrame.showScreen(AppFrame.SCREEN_NEW_PASSWORD);
        } else {
            errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Invalid or expired code.");
        }
    }

    private void onResend() {
        if (email == null || email.isBlank()) {
            appFrame.showScreen(AppFrame.SCREEN_FORGOT_PASSWORD);
            return;
        }
        BaseApiClient.ApiResult<String> result =
                ApiClientProvider.getInstance().auth().forgotPassword(new ForgotPasswordRequest(email));

        if (result.isSuccess()) {
            errorLabel.setForeground(AppTheme.STATUS_SUCCESS);
            errorLabel.setText("A new code has been sent.");
        } else {
            errorLabel.setForeground(AppTheme.STATUS_DANGER);
            errorLabel.setText("Something went wrong. Please try again.");
        }
    }

    // Small helper since AppFrame doesn't currently expose a getter for
    // NewPasswordPanel the way it does for VerifyResetCodePanel — see
    // note in AppFrame wiring instructions below for the cleaner fix.
    private Component findNewPasswordPanel() {
        return appFrame.getNewPasswordPanel();
    }
}