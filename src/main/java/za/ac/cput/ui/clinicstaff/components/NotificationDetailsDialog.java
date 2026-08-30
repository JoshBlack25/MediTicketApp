package za.ac.cput.ui.clinicstaff.components;

import za.ac.cput.model.domain.Notification;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
//Raul Everts 230270565
public class NotificationDetailsDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    public static void show(Component parent, Notification notification, String recipientName,
                            Runnable onSendFollowUp, Runnable onMarkAsRead) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Notification Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 420);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(field("Recipient", recipientName));
        content.add(field("Type", notification.getNotificationType()));

        JLabel statusValue = new JLabel(notification.getNotificationStatus() != null ? notification.getNotificationStatus() : "—");
        statusValue.setFont(FontManager.bodyFont(Font.BOLD, 14));
        statusValue.setForeground(AppTheme.statusColor(notification.getNotificationStatus()));
        content.add(labeledRow("Status", statusValue));

        content.add(field("Sent", notification.getNotificationDate() != null
                ? notification.getNotificationDate().format(DATE_FMT) : "—"));

        if (notification.getTicket() != null) {
            content.add(field("Ticket", "TK-" + String.format("%03d", notification.getTicket().getTicketId())));
        }

        JLabel messageTitle = new JLabel("Message");
        messageTitle.setFont(FontManager.bodyFont(Font.BOLD, 11));
        messageTitle.setForeground(AppTheme.TEXT_MUTED);
        messageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageTitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 4, 0));
        content.add(messageTitle);

        JTextArea messageArea = new JTextArea(notification.getNotificationMessage() != null ? notification.getNotificationMessage() : "—");
        messageArea.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        messageArea.setForeground(AppTheme.TEXT_PRIMARY);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        messageArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
        messageScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageScroll.setPreferredSize(new Dimension(0, 100));
        content.add(messageScroll);

        content.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_SM, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton close = new JButton("Close");
        close.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dialog.dispose());
        buttonRow.add(close);

        if (onSendFollowUp != null && notification.getTicket() != null) {
            JButton followUp = new JButton("Send Follow-up");
            followUp.setFont(FontManager.bodyFont(Font.BOLD, 13));
            followUp.setForeground(AppTheme.TEXT_ON_PRIMARY);
            followUp.setBackground(AppTheme.PRIMARY);
            followUp.setFocusPainted(false);
            followUp.setBorderPainted(false);
            followUp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            followUp.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            followUp.addActionListener(e -> {
                dialog.dispose();
                onSendFollowUp.run();
            });
            buttonRow.add(followUp);
        }

        if (onMarkAsRead != null && !"READ".equals(notification.getNotificationStatus())) {
            JButton markRead = new JButton("Mark as Read");
            markRead.setFont(FontManager.bodyFont(Font.BOLD, 13));
            markRead.setForeground(AppTheme.TEXT_ON_PRIMARY);
            markRead.setBackground(AppTheme.PRIMARY);
            markRead.setFocusPainted(false);
            markRead.setBorderPainted(false);
            markRead.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            markRead.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            markRead.addActionListener(e -> {
                dialog.dispose();
                onMarkAsRead.run();
            });
            buttonRow.add(markRead);
        }

        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private static JComponent field(String label, String value) {
        JLabel valueLabel = new JLabel(value != null && !value.isBlank() ? value : "—");
        valueLabel.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        valueLabel.setForeground(AppTheme.TEXT_PRIMARY);
        return labeledRow(label, valueLabel);
    }

    private static JComponent labeledRow(String label, JComponent valueComponent) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueComponent.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(labelComp);
        block.add(valueComponent);
        return block;
    }
}