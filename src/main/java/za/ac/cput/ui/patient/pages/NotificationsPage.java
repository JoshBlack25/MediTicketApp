package za.ac.cput.ui.patient.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Notification;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.NotificationDetailsDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationsPage extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM, HH:mm");

    private JPanel historyList;
    private String activeFilter = "ALL";
    private JPanel filterBarContainer;
    private JPanel summaryBarContainer;
    private List<Notification> allNotifications = List.of();

    public NotificationsPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        JLabel title = new JLabel("Notifications");
        title.setFont(FontManager.headlineFont(Font.BOLD, 20));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Your notification history.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(2, 0, AppTheme.SPACE_MD, 0));

        summaryBarContainer = new JPanel(new BorderLayout());
        summaryBarContainer.setOpaque(false);
        summaryBarContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryBarContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        filterBarContainer = new JPanel(new BorderLayout());
        filterBarContainer.setOpaque(false);
        filterBarContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBarContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);

        historyList = new JPanel();
        historyList.setLayout(new BoxLayout(historyList, BoxLayout.Y_AXIS));
        historyList.setOpaque(false);

        JScrollPane scroll = new JScrollPane(historyList);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(title);
        content.add(subtitle);
        content.add(summaryBarContainer);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(filterBarContainer);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(scroll);

        add(content, BorderLayout.CENTER);

        loadNotifications();
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"PENDING", "Pending"}, {"SENT", "Sent"},
                {"FAILED", "Failed"}, {"READ", "Read"}
        };

        for (String[] f : filters) {
            JButton btn = new JButton(f[1]);
            btn.setFont(FontManager.bodyFont(Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBackground(f[0].equals(activeFilter) ? AppTheme.PRIMARY : AppTheme.SURFACE);
            btn.setForeground(f[0].equals(activeFilter) ? AppTheme.TEXT_ON_PRIMARY : AppTheme.TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
            btn.setMargin(new Insets(2, 8, 2, 8));
            btn.addActionListener(e -> {
                activeFilter = f[0];
                renderNotifications();
                filterBarContainer.removeAll();
                filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
                filterBarContainer.revalidate();
                filterBarContainer.repaint();
            });
            bar.add(btn);
        }
        return bar;
    }

    private void loadNotifications() {
        int userId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<List<Notification>> result =
                ApiClientProvider.getInstance().notifications().findByPatient(userId);
        allNotifications = result.isSuccess() && result.getData() != null ? result.getData() : List.of();
        allNotifications = allNotifications.stream()
                .sorted((a, b) -> {
                    if (a.getNotificationDate() == null) return 1;
                    if (b.getNotificationDate() == null) return -1;
                    return b.getNotificationDate().compareTo(a.getNotificationDate());
                })
                .collect(Collectors.toList());
        renderNotifications();
        summaryBarContainer.removeAll();
        summaryBarContainer.add(buildSummaryBar(allNotifications), BorderLayout.WEST);
        summaryBarContainer.revalidate();
        summaryBarContainer.repaint();
    }

    private void renderNotifications() {
        historyList.removeAll();

        List<Notification> filtered = allNotifications.stream()
                .filter(n -> activeFilter.equals("ALL") || activeFilter.equals(n.getNotificationStatus()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("No notifications found.");
            empty.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));
            historyList.add(empty);
        } else {
            for (Notification n : filtered) {
                historyList.add(buildNotificationCard(n));
                historyList.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
            }
        }

        historyList.revalidate();
        historyList.repaint();
    }

    private JPanel buildSummaryBar(List<Notification> notifications) {
        long pending = notifications.stream()
                .filter(n -> "PENDING".equals(n.getNotificationStatus())).count();
        long failed = notifications.stream()
                .filter(n -> "FAILED".equals(n.getNotificationStatus())).count();
        long read = notifications.stream()
                .filter(n -> "READ".equals(n.getNotificationStatus())).count();

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_MD, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (pending > 0) bar.add(buildDot(AppTheme.STATUS_WARNING, "Pending", pending));
        if (failed > 0) bar.add(buildDot(AppTheme.STATUS_DANGER, "Failed", failed));
        if (read > 0) bar.add(buildDot(AppTheme.STATUS_SUCCESS, "Read", read));
        if (pending == 0 && failed == 0 && read == 0) {
            JLabel none = new JLabel("No active notifications");
            none.setFont(FontManager.bodyFont(Font.PLAIN, 12));
            none.setForeground(AppTheme.TEXT_MUTED);
            bar.add(none);
        }

        return bar;
    }

    private JComponent buildDot(Color color, String label, long count) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        dot.setForeground(color);

        JLabel text = new JLabel(label + " (" + count + ")");
        text.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        text.setForeground(AppTheme.TEXT_SECONDARY);

        item.add(dot);
        item.add(text);
        return item;
    }

    private JComponent buildNotificationCard(Notification n) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_MD, AppTheme.SPACE_SM, AppTheme.SPACE_MD)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaRow.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));

        JLabel typeTag = new JLabel(n.getNotificationType() != null ? n.getNotificationType() : "—");
        typeTag.setFont(FontManager.bodyFont(Font.BOLD, 10));
        typeTag.setForeground(AppTheme.TEXT_MUTED);

        JLabel statusTag = new JLabel("\u00B7 " + (n.getNotificationStatus() != null ? n.getNotificationStatus() : "—"));
        statusTag.setFont(FontManager.bodyFont(Font.BOLD, 10));
        statusTag.setForeground(AppTheme.statusColor(n.getNotificationStatus()));

        JLabel dateTag = new JLabel("\u00B7 " + (n.getNotificationDate() != null ? n.getNotificationDate().format(DATE_FMT) : "—"));
        dateTag.setFont(FontManager.bodyFont(Font.PLAIN, 10));
        dateTag.setForeground(AppTheme.TEXT_MUTED);

        metaRow.add(typeTag);
        metaRow.add(statusTag);
        metaRow.add(dateTag);

        String preview = n.getNotificationMessage() != null ? n.getNotificationMessage() : "";
        if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
        JLabel messageLabel = new JLabel("<html>" + preview + "</html>");
        messageLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        messageLabel.setForeground(AppTheme.TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(metaRow);
        card.add(messageLabel);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                NotificationDetailsDialog.show(NotificationsPage.this, n, "You", null,
                        () ->{
                            var result = ApiClientProvider.getInstance().notifications().markAsRead(n.getNotificationId());
                            System.out.println("markAsRead: success=" + result.isSuccess() + " status=" + result.getStatusCode() + " msg=" + result.getMessage());
                            loadNotifications();
                        });
            }
        });

        return card;
    }
}