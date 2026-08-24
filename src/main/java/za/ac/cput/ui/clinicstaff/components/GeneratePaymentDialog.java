package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Generates a PENDING payment request for a RESOLVED ticket. The payment
 * becomes visible on both the Admin/Nurse Payments page and the Patient's
 * Payments page from this point.
 *
 * Payment method determines how settlement happens: EFT routes the
 * patient through their own self-checkout (FakeCheckoutDialog);
 * MEDICAL_AID lets the patient submit their claim details themselves
 * (MedicalAidDialog), but the payment stays PENDING until the scheme
 * authorises the claim and staff confirm it; CASH and CARD are settled
 * in person at the clinic, so staff confirm those once collected.
 */
public class GeneratePaymentDialog {

    public static void show(Component parent, PatientTicket ticket, Runnable onGenerated) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Generate Payment Request", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 360);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        JLabel subtitle = new JLabel("<html>Creates a pending payment request for this ticket. "
                + "It will appear on the patient's Payments page for them to settle.</html>");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));

        JLabel amountLabel = new JLabel("Consultation Fee (R)");
        amountLabel.setFont(FontManager.bodyFont(Font.BOLD, 12));
        amountLabel.setForeground(AppTheme.TEXT_PRIMARY);
        amountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        amountLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JTextField amountField = new JTextField();
        amountField.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JLabel methodLabel = new JLabel("Expected Payment Method");
        methodLabel.setFont(FontManager.bodyFont(Font.BOLD, 12));
        methodLabel.setForeground(AppTheme.TEXT_PRIMARY);
        methodLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 4, 0));

        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"CASH", "CARD", "EFT", "MEDICAL_AID"});
        methodCombo.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        methodCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_SM, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));

        JButton cancel = new JButton("Cancel");
        cancel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> dialog.dispose());

        JButton generate = new JButton("Generate Request");
        generate.setFont(FontManager.bodyFont(Font.BOLD, 13));
        generate.setForeground(AppTheme.TEXT_ON_PRIMARY);
        generate.setBackground(AppTheme.PRIMARY);
        generate.setFocusPainted(false);
        generate.setBorderPainted(false);
        generate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        generate.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        generate.addActionListener(e -> {
            String amountStr = amountField.getText().trim();
            if (amountStr.isEmpty()) {
                errorLabel.setText("Please enter a fee amount.");
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errorLabel.setText("Amount must be greater than zero.");
                    return;
                }
            } catch (NumberFormatException ex) {
                errorLabel.setText("Please enter a valid number.");
                return;
            }
            errorLabel.setText(" ");

            Payment payment = new Payment();
            payment.setAppointment(ticket.getAppointment());
            payment.setPaymentAmount(amount);
            payment.setPaymentMethod((String) methodCombo.getSelectedItem());
            payment.setPaymentStatus("PENDING");

            BaseApiClient.ApiResult<Payment> result = ApiClientProvider.getInstance().payments().create(payment);

            if (result.isSuccess()) {
                dialog.dispose();
                AppDialog.show(parent, "Payment Request Generated",
                        "A payment request for R" + amount + " is now pending on the patient's Payments page.",
                        AppDialog.Type.SUCCESS);
                if (onGenerated != null) onGenerated.run();
            } else {
                errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Unable to generate payment.");
            }
        });

        buttonRow.add(cancel);
        buttonRow.add(generate);

        content.add(subtitle);
        content.add(amountLabel);
        content.add(amountField);
        content.add(methodLabel);
        content.add(methodCombo);
        content.add(errorLabel);
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }
}