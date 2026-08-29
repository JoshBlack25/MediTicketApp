package za.ac.cput.ui.theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Branded replacement for raw JOptionPane dialogs. Used anywhere the app
 * needs to show a success/error/info confirmation to the user, styled
 * consistently with AppTheme rather than the platform's default look.
 */
public class AppDialog {

    public enum Type { SUCCESS, ERROR, INFO }

    public static void show(Component parent, String title, String message, Type type) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0)); // lets the rounded-shadow panel show through

        ShadowPanel content = new ShadowPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(AppTheme.SPACE_XL, AppTheme.SPACE_XL, AppTheme.SPACE_LG, AppTheme.SPACE_XL));

        content.add(iconBadge(type));
        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontManager.headlineFont(Font.BOLD, 19));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        JLabel messageLabel = new JLabel("<html><div style='text-align:center;width:300px;'>"
                + message.replace("\n", "<br>") + "</div></html>");
        messageLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        messageLabel.setForeground(AppTheme.TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(messageLabel);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));

        JButton okButton = new JButton("OK");
        okButton.setFont(FontManager.bodyFont(Font.BOLD, 14));
        okButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
        okButton.setBackground(colorFor(type));
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setPreferredSize(new Dimension(120, 40));
        okButton.setMaximumSize(new Dimension(120, 40));
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okButton.addActionListener(e -> dialog.dispose());
        content.add(okButton);

        // Keyboard support — Enter and Esc both dismiss, matching how
        // every native OK/confirm dialog behaves.
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.getRootPane().registerKeyboardAction(
                (ActionEvent e) -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Draggable — undecorated dialogs have no title bar to grab, so
        // without this a mispositioned dialog is unmovable.
        DragHandler drag = new DragHandler(dialog);
        content.addMouseListener(drag);
        content.addMouseMotionListener(drag);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(400, 0));
        dialog.setLocationRelativeTo(parent);
        okButton.requestFocusInWindow();
        dialog.setVisible(true);
    }

    private static JComponent iconBadge(Type type) {
        JLabel icon = new JLabel(iconTextFor(type), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColorFor(type));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setFont(FontManager.bodyFont(Font.BOLD, 22));
        icon.setForeground(colorFor(type));
        icon.setPreferredSize(new Dimension(56, 56));
        icon.setMaximumSize(new Dimension(56, 56));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        return icon;
    }

    private static Color colorFor(Type type) {
        return switch (type) {
            case SUCCESS -> AppTheme.STATUS_SUCCESS;
            case ERROR -> AppTheme.STATUS_DANGER;
            case INFO -> AppTheme.PRIMARY;
        };
    }

    private static Color bgColorFor(Type type) {
        return switch (type) {
            case SUCCESS -> AppTheme.STATUS_SUCCESS_BG;
            case ERROR -> AppTheme.STATUS_DANGER_BG;
            case INFO -> AppTheme.PRIMARY_LIGHT;
        };
    }

    private static String iconTextFor(Type type) {
        return switch (type) {
            case SUCCESS -> "\u2713";
            case ERROR -> "\u2715";
            case INFO -> "\u2139";
        };
    }

    /** Rounded card with a soft drop shadow, painted manually since the dialog is undecorated. */
    private static class ShadowPanel extends JPanel {
        private static final int SHADOW_SIZE = 12;

        ShadowPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, SHADOW_SIZE, SHADOW_SIZE)); // reserve room for the shadow itself
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - SHADOW_SIZE;
            int h = getHeight() - SHADOW_SIZE;

            for (int i = SHADOW_SIZE; i > 0; i--) {
                g2.setColor(new Color(0, 0, 0, 2));
                g2.fillRoundRect(i / 2, i / 2, w + (SHADOW_SIZE - i), h + (SHADOW_SIZE - i), AppTheme.RADIUS_LG, AppTheme.RADIUS_LG);
            }

            g2.setColor(AppTheme.SURFACE);
            g2.fillRoundRect(0, 0, w, h, AppTheme.RADIUS_LG, AppTheme.RADIUS_LG);
            g2.setColor(AppTheme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, AppTheme.RADIUS_LG, AppTheme.RADIUS_LG);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class DragHandler extends MouseAdapter {
        private final JDialog dialog;
        private Point offset;

        DragHandler(JDialog dialog) { this.dialog = dialog; }

        @Override
        public void mousePressed(MouseEvent e) { offset = e.getPoint(); }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point loc = dialog.getLocation();
            dialog.setLocation(loc.x + e.getX() - offset.x, loc.y + e.getY() - offset.y);
        }
    }
}