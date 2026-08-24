package za.ac.cput.ui.patient.components;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fake checkout — no real payment gateway integration exists. Collects
 * cosmetic card details (never sent anywhere, never stored) purely to
 * simulate the feel of paying, then flips the Payment to PAID via the
 * same PaymentApiClient.update() call the old admin "Mark as Paid" used.
 * The backend's auto-close hook (PaymentService) doesn't care who
 * triggered it, so no backend changes were needed to move this action
 * from staff to patient.
 *
 * The three inputs are reformatted as they are typed (card number grouped
 * in fours, expiry slashed as MM/YY, CVV capped at three digits) via
 * DocumentFilters rather than key listeners, so paste and backspace behave.
 */
public class FakeCheckoutDialog {

    public static void show(Component parent, Payment payment, Runnable onPaid) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Pay Now", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 480);
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

        CardBrandStrip brandStrip = new CardBrandStrip();

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        cardHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        cardHeader.add(cardLabel, BorderLayout.WEST);
        cardHeader.add(brandStrip, BorderLayout.EAST);

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

        ((AbstractDocument) cardNumber.getDocument()).setDocumentFilter(new DigitFilter(
                cardNumber, 16, FakeCheckoutDialog::groupInFours,
                digits -> brandStrip.setBrand(detectBrand(digits))));
        ((AbstractDocument) expiry.getDocument()).setDocumentFilter(new DigitFilter(
                expiry, 4, FakeCheckoutDialog::asExpiry, null));
        ((AbstractDocument) cvv.getDocument()).setDocumentFilter(new DigitFilter(
                cvv, 3, digits -> digits, null));

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
            String digits = digitsOnly(cardNumber.getText());
            String exp = expiry.getText().trim();
            String code = cvv.getText().trim();

            if (digits.isEmpty() && exp.isEmpty() && code.isEmpty()) {
                errorLabel.setText("Please fill in all card details.");
                return;
            }
            if (digits.length() != 16) {
                errorLabel.setText("Card number must be 16 digits.");
                return;
            }
            if (!exp.matches("\\d{2}/\\d{2}")) {
                errorLabel.setText("Expiry must be in MM/YY format.");
                return;
            }
            int month = Integer.parseInt(exp.substring(0, 2));
            if (month < 1 || month > 12) {
                errorLabel.setText("Expiry month must be between 01 and 12.");
                return;
            }
            if (code.length() != 3) {
                errorLabel.setText("CVV must be 3 digits.");
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
        content.add(cardHeader);
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
        // Returning just the field for simplicity in the caller's layout —
        // caller adds the field directly; label omitted here intentionally
        // since amountLabel-style headers are used elsewhere in this dialog.
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

    // ── Input formatting ──────────────────────────────────────────

    /**
     * Reformats a digits-only field while the user types. This works at the
     * Document level rather than on key events so that pasting, backspacing
     * and clicking into the middle of the field all behave correctly. After
     * reformatting, the caret is re-anchored by digit index — without that it
     * jumps to the end of the field on every keystroke.
     */
    private static class DigitFilter extends DocumentFilter {

        private final JTextField field;
        private final int maxDigits;
        private final Function<String, String> formatter;
        private final Consumer<String> onDigits;

        DigitFilter(JTextField field, int maxDigits,
                    Function<String, String> formatter, Consumer<String> onDigits) {
            this.field = field;
            this.maxDigits = maxDigits;
            this.formatter = formatter;
            this.onDigits = onDigits;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            replace(fb, offset, length, "", null);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            Document doc = fb.getDocument();
            String current = doc.getText(0, doc.getLength());

            String head = digitsOnly(current.substring(0, offset));
            String inserted = digitsOnly(text == null ? "" : text);
            String tail = digitsOnly(current.substring(offset + length));

            String digits = head + inserted + tail;
            if (digits.length() > maxDigits) digits = digits.substring(0, maxDigits);

            String formatted = formatter.apply(digits);
            fb.replace(0, doc.getLength(), formatted, attrs);

            int caret = caretPositionFor(formatted, Math.min(head.length() + inserted.length(), digits.length()));
            SwingUtilities.invokeLater(() ->
                    field.setCaretPosition(Math.min(caret, field.getDocument().getLength())));

            if (onDigits != null) onDigits.accept(digits);
        }
    }

    private static String digitsOnly(String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    private static String groupInFours(String digits) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) out.append(' ');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    private static String asExpiry(String digits) {
        return digits.length() <= 2 ? digits : digits.substring(0, 2) + "/" + digits.substring(2);
    }

    /** Maps a digit index back to a caret offset in the formatted text. */
    private static int caretPositionFor(String formatted, int digitIndex) {
        if (digitIndex <= 0) return 0;
        int seen = 0;
        for (int i = 0; i < formatted.length(); i++) {
            if (Character.isDigit(formatted.charAt(i)) && ++seen == digitIndex) return i + 1;
        }
        return formatted.length();
    }

    /** Standard issuer ranges: Visa starts 4, Mastercard 51-55 or 2221-2720. */
    private static String detectBrand(String digits) {
        if (digits.isEmpty()) return null;
        if (digits.charAt(0) == '4') return "VISA";
        if (digits.length() >= 2) {
            int two = Integer.parseInt(digits.substring(0, 2));
            if (two >= 51 && two <= 55) return "MASTERCARD";
        }
        if (digits.length() >= 4) {
            int four = Integer.parseInt(digits.substring(0, 4));
            if (four >= 2221 && four <= 2720) return "MASTERCARD";
        }
        return null;
    }

    // ── Card brand marks ──────────────────────────────────────────

    /**
     * Visa and Mastercard marks drawn with Java2D. Swing cannot render SVG,
     * and pulling in an SVG library would mean a new Maven dependency for two
     * small graphics — so these are hand-drawn approximations of the brands'
     * artwork, not the official assets. Both sit dimmed until the card number
     * identifies one of them, the way a real checkout does.
     */
    private static class CardBrandStrip extends JComponent {

        private static final int BADGE_W = 40;
        private static final int BADGE_H = 26;
        private static final int GAP = 6;

        private String brand;

        void setBrand(String brand) {
            if (!Objects.equals(this.brand, brand)) {
                this.brand = brand;
                repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(BADGE_W * 2 + GAP, BADGE_H);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintBadge(g2, 0, "VISA");
            paintBadge(g2, BADGE_W + GAP, "MASTERCARD");
            g2.dispose();
        }

        private void paintBadge(Graphics2D g2, int x, String which) {
            Graphics2D b = (Graphics2D) g2.create();
            if (brand != null && !brand.equals(which)) {
                b.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            }

            Shape badge = new RoundRectangle2D.Double(x + 0.5, 0.5, BADGE_W - 1, BADGE_H - 1, 5, 5);
            b.setColor(Color.WHITE);
            b.fill(badge);
            b.setColor(AppTheme.BORDER);
            b.draw(badge);

            if ("VISA".equals(which)) {
                b.setColor(new Color(0x1A1F71));
                b.setFont(FontManager.bodyFont(Font.BOLD | Font.ITALIC, 12));
                FontMetrics fm = b.getFontMetrics();
                String text = "VISA";
                b.drawString(text,
                        x + (BADGE_W - fm.stringWidth(text)) / 2f,
                        (BADGE_H + fm.getAscent()) / 2f - 1.5f);
            } else {
                double d = 15;
                double top = (BADGE_H - d) / 2.0;
                double centre = x + BADGE_W / 2.0;
                Shape left = new Ellipse2D.Double(centre - d * 0.78, top, d, d);
                Shape right = new Ellipse2D.Double(centre - d * 0.22, top, d, d);

                b.setColor(new Color(0xEB001B));
                b.fill(left);
                b.setColor(new Color(0xF79E1B));
                b.fill(right);

                Area overlap = new Area(left);
                overlap.intersect(new Area(right));
                b.setColor(new Color(0xFF5F00));
                b.fill(overlap);
            }
            b.dispose();
        }
    }
}
