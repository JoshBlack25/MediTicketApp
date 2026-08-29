package za.ac.cput.ui.auth;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.ResetPasswordRequest;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.auth.components.PrimaryButton;
import za.ac.cput.ui.auth.components.ToggleablePasswordField;
import za.ac.cput.ui.theme.*;

import javax.swing.*;
import java.awt.*;


public class NewPasswordPanel extends JPanel {

    private final AppFrame appFrame;
    private String email;
    private String resetSessionToken;

    private ToggleablePasswordField newPasswordField;
    private ToggleablePasswordField confirmPasswordField;
    private JLabel errorLabel;

    public NewPasswordPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new GridBagLayout());
        setBackground(AppTheme.BACKGROUND);
        add(buildCard());
    }

    public void setResetContext(String email, String resetSessionToken) {
        this.email = email;
        this.resetSessionToken = resetSessionToken;
        if (newPasswordField != null) newPasswordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        if (errorLabel != null) errorLabel.setText(" ");
    }

    private JComponent buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_XL)
        ));
        card.setPreferredSize(new Dimension(440, 360));

        JLabel title = new JLabel("Create New Password");
        title.setFont(FontManager.headlineFont(Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Choose a new password for your account.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_LG, 0));

        newPasswordField = new ToggleablePasswordField("New Password");
        newPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        newPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        confirmPasswordField = new ToggleablePasswordField("Confirm New Password");
        confirmPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        PrimaryButton resetButton = new PrimaryButton("Reset Password");
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        resetButton.addActionListener(e -> onReset());

        card.add(title);
        card.add(subtitle);
        card.add(newPasswordField);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        card.add(confirmPasswordField);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(resetButton);

        return card;
    }

    private void onReset() {
        if (resetSessionToken == null || resetSessionToken.isBlank()) {
            errorLabel.setText("Your session has expired. Please start over.");
            return;
        }

        String newPass = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            errorLabel.setText("Please fill in both fields.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }
        if (newPass.length() < 8) {
            errorLabel.setText("Password must be at least 8 characters.");
            return;
        }
        errorLabel.setText(" ");

        BaseApiClient.ApiResult<String> result = ApiClientProvider.getInstance()
                .auth().resetPassword(new ResetPasswordRequest(email, resetSessionToken, newPass));

        if (result.isSuccess()) {
            newPasswordField.clear();
            confirmPasswordField.clear();
            AppDialog.show(this, "Password Reset",
                    "Your password has been reset successfully. You can now log in.", AppDialog.Type.SUCCESS);
            appFrame.showScreen(AppFrame.SCREEN_LOGIN);
        } else {
            errorLabel.setText(result.getMessage() != null ? result.getMessage()
                    : "Unable to reset password. Please request a new code.");
        }
    }
}