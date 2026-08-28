/*
 MedicalAidDialog.java

 MedicalAidDialog  — component.

 Author: Abdullahi Farah (230971091)

 Date: 23 August 2026
*/
package za.ac.cput.ui.patient.components;

import za.ac.cput.model.domain.Payment;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;

/**
 * Medical aid claim submission for a PENDING payment whose method is
 * MEDICAL_AID.

 * Deliberately does NOT settle the payment, which is the key difference
 * from FakeCheckoutDialog. A real medical aid claim is submitted to the
 * scheme and then sits awaiting authorisation before the clinic can
 * confirm it, so the payment correctly stays PENDING after a successful
 * submission — that's why nothing is written back through
 * PaymentApiClient here, and why show() takes no refresh callback: there
 * is no state change for the Payments table to re-read.

 */
public class MedicalAidDialog {

    private static final String SCHEME_PROMPT = "Select your scheme";

    private static final String[] SCHEMES = {
            SCHEME_PROMPT, "Discovery Health", "Momentum Health",
            "Bonitas", "Medihelp", "Other"
    };

    public static void show(Component parent, Payment payment) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Submit Medical Aid Claim", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 510);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        JLabel amountLabel = new JLabel("Amount to Claim");
        amountLabel.setFont(FontManager.bodyFont(Font.BOLD, 12));
        amountLabel.setForeground(AppTheme.TEXT_MUTED);
        amountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel amountValue = new JLabel("R" + (payment.getPaymentAmount() != null
                ? payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP) : "0.00"));
        amountValue.setFont(FontManager.headlineFont(Font.BOLD, 28));
        amountValue.setForeground(AppTheme.TEXT_PRIMARY);
        amountValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        amountValue.setBorder(BorderFactory.createEmptyBorder(2, 0, AppTheme.SPACE_MD, 0));

        JLabel subtitle = new JLabel("Submit or update your medical aid details.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        JLabel disclaimer = new JLabel("<html><i>Simulated submission for demonstration purposes. "
                + "No details are transmitted to a medical aid scheme or stored.</i></html>");
        disclaimer.setFont(FontManager.bodyFont(Font.PLAIN, 11));
        disclaimer.setForeground(AppTheme.TEXT_MUTED);
        disclaimer.setAlignmentX(Component.LEFT_ALIGNMENT);
        disclaimer.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));

        JComboBox<String> schemeCombo = new JComboBox<>(SCHEMES);
        schemeCombo.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        schemeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        schemeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JTextField memberNumber = styledField("123456789");
        JTextField dependentCode = styledField("e.g. 01 — leave blank if main member");

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));

        JButton submitButton = new JButton("Submit Claim Details");
        submitButton.setFont(FontManager.bodyFont(Font.BOLD, 14));
        submitButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
        submitButton.setBackground(AppTheme.PRIMARY);
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        submitButton.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        submitButton.addActionListener(e -> {
            String scheme = (String) schemeCombo.getSelectedItem();
            if (scheme == null || SCHEME_PROMPT.equals(scheme)) {
                errorLabel.setText("Please select your medical aid scheme.");
                return;
            }

            // Members routinely type the number in spaced groups — strip
            // whitespace before checking the 9-digit shape.
            String member = memberNumber.getText().replaceAll("\\s", "");
            if (member.isEmpty()) {
                errorLabel.setText("Please enter your membership number.");
                return;
            }
            if (!member.matches("\\d{9}")) {
                errorLabel.setText("Membership number must be exactly 9 digits.");
                return;
            }

            String dependent = dependentCode.getText().trim();
            if (!dependent.isEmpty() && !dependent.matches("\\d{1,2}")) {
                errorLabel.setText("Dependent code must be 1 or 2 digits.");
                return;
            }
            errorLabel.setText(" ");

            dialog.dispose();
            // AppDialog is a fixed 420x220 box that clips the OK button if the
            // message runs long, and drops trailing words on any line wider than
            // roughly 50 characters. It maps \n to <br>, so the breaks are placed
            // explicitly here rather than left to its own wrapping.
            AppDialog.show(parent, "Claim Submitted",
                    "Sent to " + scheme + ".\nThis payment stays pending until approved.",
                    AppDialog.Type.SUCCESS);
        });

        content.add(amountLabel);
        content.add(amountValue);
        content.add(subtitle);
        content.add(disclaimer);
        content.add(fieldLabel("Medical Aid Scheme"));
        content.add(schemeCombo);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(fieldLabel("Membership Number"));
        content.add(memberNumber);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(fieldLabel("Dependent Code (optional)"));
        content.add(dependentCode);
        content.add(errorLabel);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        content.add(submitButton);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.bodyFont(Font.BOLD, 11));
        label.setForeground(AppTheme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Java under-measures the Inter variable font's advance width, so a
        // label sized to its exact preferred width drops its final glyph
        // ("(optional)" renders as "(optional"). The right inset buys it back.
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 6));
        return label;
    }

    private static JTextField styledField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }
}
