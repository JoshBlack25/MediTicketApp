package za.ac.cput.ui.patient.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Notification;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.patient.components.ElevatedCard;
import za.ac.cput.ui.patient.components.IconBadge;
import za.ac.cput.ui.patient.components.StatusBadge;
import za.ac.cput.ui.patient.components.WrappingLabel;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class NotificationsPage extends JPanel {

    private JPanel listContainer;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    public NotificationsPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));

        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);
        listContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        listContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        content.add(listContainer);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Notifications");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Updates about your appointments, tickets, and payments.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }



    private void loadData() {
        int patientId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<List<Notification>> result =
                ApiClientProvider.getInstance().notifications().findByPatient(patientId);

        List<Notification> notifications = result.isSuccess() ? result.getData() : List.of();
        renderList(notifications);
    }

    private void renderList(List<Notification> notifications) {
        listContainer.removeAll();

        if (notifications.isEmpty()) {
            listContainer.add(emptyState());
            listContainer.revalidate();
            listContainer.repaint();
            return;
        }

        List<Notification> sorted = notifications.stream()
                .sorted(Comparator.comparing(
                        (Notification n) -> n.getNotificationDate() != null ? n.getNotificationDate() : java.time.LocalDateTime.MIN
                ).reversed())
                .toList();

        // Group into Today / Yesterday / Earlier so a long history doesn't
        // read as one undifferentiated wall of cards.
        Map<String, List<Notification>> groups = new LinkedHashMap<>();
        groups.put("Today", new java.util.ArrayList<>());
        groups.put("Yesterday", new java.util.ArrayList<>());
        groups.put("Earlier", new java.util.ArrayList<>());

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        for (Notification n : sorted) {
            LocalDate d = n.getNotificationDate() != null ? n.getNotificationDate().toLocalDate() : null;
            if (d != null && d.isEqual(today)) {
                groups.get("Today").add(n);
            } else if (d != null && d.isEqual(yesterday)) {
                groups.get("Yesterday").add(n);
            } else {
                groups.get("Earlier").add(n);
            }
        }

        boolean firstGroup = true;
        for (Map.Entry<String, List<Notification>> group : groups.entrySet()) {
            if (group.getValue().isEmpty()) continue;

            if (!firstGroup) {
                listContainer.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
            }
            firstGroup = false;

            listContainer.add(groupHeader(group.getKey()));
            listContainer.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

            for (Notification notification : group.getValue()) {
                listContainer.add(notificationCard(notification));
                listContainer.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JComponent groupHeader(String label) {
        JLabel header = new JLabel(label.toUpperCase());
        header.setFont(FontManager.bodyFont(Font.BOLD, 11));
        header.setForeground(AppTheme.TEXT_MUTED);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    private JComponent notificationCard(Notification notification) {
        ElevatedCard card = new ElevatedCard(AppTheme.RADIUS_MD);
        card.setLayout(new BorderLayout(AppTheme.SPACE_MD, 0));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setBorder(BorderFactory.createCompoundBorder(
                card.getBorder(),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD)
        ));

        IconBadge icon = new IconBadge(iconFor(notification), colorFor(notification));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel typeLabel = new JLabel(categoryFor(notification));
        typeLabel.setFont(FontManager.bodyFont(Font.BOLD, 11));
        typeLabel.setForeground(AppTheme.TEXT_MUTED);
        topRow.add(typeLabel, BorderLayout.WEST);
        topRow.add(new StatusBadge(notification.getNotificationStatus()), BorderLayout.EAST);

        String message = notification.getNotificationMessage() != null && !notification.getNotificationMessage().isBlank()
                ? notification.getNotificationMessage() : "No message content";
        WrappingLabel messageLabel = new WrappingLabel(message, FontManager.bodyFont(Font.PLAIN, 13), AppTheme.TEXT_PRIMARY);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, AppTheme.SPACE_XS, 0));

        String dateText = notification.getNotificationDate() != null
                ? notification.getNotificationDate().format(DATE_FMT) : "\u2014";
        String channelText = notification.getNotificationType() != null
                ? "  \u2022  Sent via " + notification.getNotificationType() : "";

        JLabel metaLabel = new JLabel(dateText + channelText);
        metaLabel.setFont(FontManager.bodyFont(Font.PLAIN, 11));
        metaLabel.setForeground(AppTheme.TEXT_MUTED);
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textStack.add(topRow);
        textStack.add(messageLabel);
        textStack.add(metaLabel);

        card.add(icon, BorderLayout.WEST);
        card.add(textStack, BorderLayout.CENTER);
        return card;
    }

    private String categoryFor(Notification notification) {
        if (notification.getAppointment() != null) return "APPOINTMENT";
        if (notification.getTicket() != null) return "TICKET";
        return "GENERAL";
    }

    private String iconFor(Notification notification) {
        if (notification.getAppointment() != null) return "\uD83D\uDCC5"; // 📅
        if (notification.getTicket() != null) return "\uD83C\uDFAB";      // 🎫
        return "\uD83D\uDD14";                                            // 🔔
    }

    private Color colorFor(Notification notification) {
        if (notification.getAppointment() != null) return AppTheme.PRIMARY_LIGHT;
        if (notification.getTicket() != null) return AppTheme.STATUS_INFO_BG;
        return AppTheme.STATUS_NEUTRAL_BG;
    }

    private JComponent emptyState() {
        ElevatedCard card = new ElevatedCard(AppTheme.RADIUS_MD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setBorder(BorderFactory.createCompoundBorder(
                card.getBorder(),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_XL, AppTheme.SPACE_LG, AppTheme.SPACE_XL, AppTheme.SPACE_LG)
        ));

        JLabel icon = new JLabel("\uD83D\uDD14");
        icon.setFont(FontManager.bodyFont(Font.PLAIN, 28));
        icon.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("No notifications yet");
        label.setFont(FontManager.bodyFont(Font.BOLD, 15));
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_XS, 0));

        JLabel sub = new JLabel("You'll see updates about appointments, tickets, and payments here.");
        sub.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        sub.setForeground(AppTheme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(icon);
        card.add(label);
        card.add(sub);
        return card;
    }
}