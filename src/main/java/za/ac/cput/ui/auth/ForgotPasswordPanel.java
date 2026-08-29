package za.ac.cput.ui.auth;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.ForgotPasswordRequest;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.auth.components.LabeledTextField;
import za.ac.cput.ui.auth.components.PrimaryButton;
import za.ac.cput.ui.theme.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class ForgotPasswordPanel extends JPanel {

    private final AppFrame appFrame;
    private LabeledTextField emailField;
    private JLabel errorLabel;

    public ForgotPasswordPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new GridBagLayout());
        setBackground(AppTheme.BACKGROUND);
        add(buildCard());
    }

    private JComponent buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL)
        ));
        card.setPreferredSize(new Dimension(440, 320));

        JLabel title = new JLabel("Forgot Your Password?");
        title.setFont(FontManager.headlineFont(Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("<html><div style='text-align:center;width:320px;'>"
                + "Enter the email address associated with your account. "
                + "We'll send you a code to reset your password.</div></html>");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_LG, 0));

        emailField = new LabeledTextField("Email");
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        PrimaryButton sendButton = new PrimaryButton("Send Reset Code");
        sendButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        sendButton.addActionListener(e -> onSendCode());

        JLabel backToLogin = new JLabel("← Back to Login");
        backToLogin.setFont(FontManager.bodyFont(Font.BOLD, 13));
        backToLogin.setForeground(AppTheme.TEXT_SECONDARY);
        backToLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        backToLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToLogin.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));
        backToLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { appFrame.showScreen(AppFrame.SCREEN_LOGIN); }
        });

        card.add(title);
        card.add(subtitle);
        card.add(emailField);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(sendButton);
        card.add(backToLogin);

        return card;
    }

    private void onSendCode() {
        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            errorLabel.setText("Please enter a valid email address.");
            return;
        }
        errorLabel.setText(" ");

        BaseApiClient.ApiResult<String> result =
                ApiClientProvider.getInstance().auth().forgotPassword(new ForgotPasswordRequest(email));

        if (result.isSuccess()) {
            appFrame.getVerifyResetCodePanel().setEmail(email);
            emailField.getField().setText("");
            appFrame.showScreen(AppFrame.SCREEN_VERIFY_RESET_CODE);
        } else {

            errorLabel.setText("Something went wrong. Please try again.");
        }
    }
}