package za.ac.cput.ui.layout;

import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.AvatarManager;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TopHeader extends JPanel {

    private JLabel notificationBadge;
    private JLabel avatarLabel;
    private JLabel nameLabel;
    private JLabel roleLabel;

    public TopHeader() {
        this(null);
    }

    public TopHeader(Runnable onProfileClick) {
        setLayout(new BorderLayout());
        setBackground(AppTheme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_LG, AppTheme.SPACE_MD, AppTheme.SPACE_LG)
        ));
        setPreferredSize(new Dimension(0, 72));

        add(buildProfileSection(onProfileClick), BorderLayout.WEST);
        add(buildRightSection(), BorderLayout.EAST);
    }

    private JComponent buildProfileSection(Runnable onProfileClick) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        panel.setOpaque(false);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        SessionManager session = SessionManager.getInstance();

        avatarLabel = new JLabel(AvatarManager.getCircularAvatar(session.getUserId(), 40));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        String displayName = (session.getFullName() != null && !session.getFullName().isBlank())
                ? session.getFullName()
                : session.getEmail();

        nameLabel = new JLabel(displayName != null ? displayName : "—");
        nameLabel.setFont(FontManager.bodyFont(Font.BOLD, 14));
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        roleLabel = new JLabel(resolveRoleLabel(session));
        roleLabel.setFont(FontManager.bodyFont(Font.BOLD, 10));
        roleLabel.setForeground(AppTheme.TEXT_MUTED);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textStack.add(nameLabel);
        textStack.add(roleLabel);

        panel.add(avatarLabel);
        panel.add(textStack);

        if (onProfileClick != null) {
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onProfileClick.run();
                }
            });
        }

        return panel;
    }

    /**
     * Re-reads SessionManager and AvatarManager and refreshes the name,
     * role, and avatar shown here. Call this after a profile save or
     * avatar change elsewhere in the app so the header stays in sync
     * without needing SessionManager to broadcast changes itself.
     */
    public void refreshProfile() {
        SessionManager session = SessionManager.getInstance();

        String displayName = (session.getFullName() != null && !session.getFullName().isBlank())
                ? session.getFullName()
                : session.getEmail();

        nameLabel.setText(displayName != null ? displayName : "—");
        roleLabel.setText(resolveRoleLabel(session));
        avatarLabel.setIcon(AvatarManager.getCircularAvatar(session.getUserId(), 40));
    }

    private String resolveRoleLabel(SessionManager session) {
        if ("CLINIC_STAFF".equals(session.getUserType())) {
            return "ADMIN".equals(session.getStaffRole()) ? "ADMINISTRATOR" : "NURSE";
        }
        if ("DOCTOR".equals(session.getUserType())) return "DOCTOR";
        if ("PATIENT".equals(session.getUserType())) return "PATIENT";
        return "";
    }

    private JComponent buildRightSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_MD, 0));
        panel.setOpaque(false);

        panel.add(buildNotificationBell());
        panel.add(buildSearchField());
        return panel;
    }

    private JComponent buildNotificationBell() {
        JPanel wrapper = new JPanel(null);
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(36, 36));

        JLabel bell = new JLabel("\uD83D\uDD14");
        bell.setFont(FontManager.bodyFont(Font.PLAIN, 20));
        bell.setBounds(0, 0, 36, 36);
        bell.setHorizontalAlignment(SwingConstants.CENTER);
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        notificationBadge = new JLabel();
        notificationBadge.setOpaque(true);
        notificationBadge.setBackground(AppTheme.STATUS_DANGER);
        notificationBadge.setBounds(24, 2, 10, 10);
        notificationBadge.setVisible(false);

        wrapper.add(notificationBadge);
        wrapper.add(bell);
        return wrapper;
    }

    public void setUnreadCount(int count) {
        notificationBadge.setVisible(count > 0);
    }

    private JComponent buildSearchField() {
        JTextField search = new JTextField();
        search.putClientProperty("JTextField.placeholderText", "Search medical records...");
        search.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        search.setPreferredSize(new Dimension(260, 36));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        return search;
    }
}