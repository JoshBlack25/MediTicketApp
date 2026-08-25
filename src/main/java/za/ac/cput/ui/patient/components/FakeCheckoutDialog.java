package za.ac.cput.ui.patient.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;


public class FakeCheckoutDialog {

    public static void show(Component parent, Payment payment, Runnable onPaid) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Pay Now", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 460);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        JLabel amountLabel = new JLabel("Amount Due");
        amountLabel.setFont(FontManager.bodyFont(Font.BOLD, 12));
        amountLabel.setForeground(AppTheme.TEXT_MUTED);
        amountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel amountValue = new JLabel("R" + (payment.getPaymentAmount() != null
                ? payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP) : "0.00"));
        amountValue.setFont(FontManager.headlineFont(Font.BOLD, 28));
        amountValue.setForeground(AppTheme.TEXT_PRIMARY);
        amountValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        amountValue.setBorder(BorderFactory.createEmptyBorder(2, 0, AppTheme.SPACE_LG, 0));

        JLabel disclaimer = new JLabel("<html><i>This is a simulated payment for demonstration purposes. "
                + "No real card details are transmitted.</i></html>");
        disclaimer.setFont(FontManager.bodyFont(Font.PLAIN, 11));
        disclaimer.setForeground(AppTheme.TEXT_MUTED);
        disclaimer.setAlignmentX(Component.LEFT_ALIGNMENT);
        disclaimer.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));

        JLabel cardLabel = new JLabel("Card Number");
        cardLabel.setFont(FontManager.bodyFont(Font.BOLD, 11));
        cardLabel.setForeground(AppTheme.TEXT_MUTED);
        cardLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JTextField cardNumber = new JTextField();
        cardNumber.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        cardNumber.putClientProperty("JTextField.placeholderText", "4242 4242 4242 4242");
        cardNumber.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardNumber.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cardNumber.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JTextField expiry = new JTextField();
        expiry.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        expiry.putClientProperty("JTextField.placeholderText", "MM/YY");
        expiry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JTextField cvv = new JTextField();
        cvv.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        cvv.putClientProperty("JTextField.placeholderText", "CVV");
        cvv.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JPanel row = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row.add(wrapWithLabel("Expiry", expiry));
        row.add(wrapWithLabel("CVV", cvv));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));

        JButton payButton = new JButton("Pay R" + (payment.getPaymentAmount() != null
                ? payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP) : "0.00"));
        payButton.setFont(FontManager.bodyFont(Font.BOLD, 14));
        payButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
        payButton.setBackground(AppTheme.PRIMARY);
        payButton.setFocusPainted(false);
        payButton.setBorderPainted(false);
        payButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        payButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        payButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        payButton.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        payButton.addActionListener(e -> {
            if (cardNumber.getText().trim().isEmpty() || expiry.getText().trim().isEmpty() || cvv.getText().trim().isEmpty()) {
                errorLabel.setText("Please fill in all card details.");
                return;
            }
            errorLabel.setText(" ");

            payment.setPaymentStatus("PAID");
            BaseApiClient.ApiResult<Payment> result = ApiClientProvider.getInstance().payments().update(payment);

            if (result.isSuccess()) {
                dialog.dispose();
                AppDialog.show(parent, "Payment Successful",
                        "Your payment has been received. Thank you!", AppDialog.Type.SUCCESS);
                if (onPaid != null) onPaid.run();
            } else {
                errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Payment failed. Please try again.");
            }
        });

        content.add(amountLabel);
        content.add(amountValue);
        content.add(disclaimer);
        content.add(cardLabel);
        content.add(cardNumber);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(row);
        content.add(errorLabel);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        content.add(payButton);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static JTextField styledField(String label, String placeholder) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

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

    private static JTextField styledFieldNoLabel(String placeholder) {
        return styledField(null, placeholder);
    }

    private static JPanel wrapWithLabel(String label, JTextField field) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelComp.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(field);
        return block;
    }
}