package za.ac.cput.ui.clinicstaff.nurse.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.*;
import za.ac.cput.ui.clinicstaff.components.NotificationDetailsDialog;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Split page: left = sent notification history (read-only, filterable),
 * right = compose form. Notifications must be tied to a PatientTicket
 * (and its Appointment) per NotificationFactory's validation rules, so
 * the form works by picking a ticket first, then a recipient drawn from
 * that ticket's appointment (patient/doctor/staff).
 *
 * No actual email/SMS dispatch happens on the backend today —
 * NotificationController#create just persists the row — so this is
 * effectively a manual notification log, not a live sender.
 * notificationStatus defaults to PENDING since nothing exists yet to
 * flip it to SENT/FAILED.
 */
public class NotificationsPage extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM, HH:mm");

    // Left side — history
    private JPanel historyList;
    private String activeFilter = "ALL";
    private JPanel filterBarContainer;
    private List<Notification> allNotifications = List.of();

    // Right side — compose form
    private JComboBox<Object> ticketCombo;
    private JComboBox<String> recipientRoleCombo;
    private JComboBox<String> notificationTypeCombo;
    private JTextArea messageArea;
    private JLabel formErrorLabel;
    private List<PatientTicket> allTickets = List.of();

    private static final String TICKET_PLACEHOLDER = "-- Select a ticket --";
    private static final String RECIPIENT_PLACEHOLDER = "-- Select a recipient --";
    private static final String TYPE_PLACEHOLDER = "-- Select notification type --";

    public NotificationsPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel split = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_LG, 0));
        split.setBackground(AppTheme.BACKGROUND);
        split.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        split.add(buildLeftPanel());
        split.add(buildRightPanel());

        add(split, BorderLayout.CENTER);

        loadHistory();
    }

    // ══════════════════════════════ LEFT — HISTORY ══════════════════════════════

    private JComponent buildLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel title = new JLabel("Sent Notifications");
        title.setFont(FontManager.headlineFont(Font.BOLD, 20));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("History of notifications sent to patients and staff.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(2, 0, AppTheme.SPACE_MD, 0));

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

        panel.add(title);
        panel.add(subtitle);
        panel.add(filterBarContainer);
        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        panel.add(scroll);
        return panel;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"PATIENT", "Patient"}, {"DOCTOR", "Doctor"}, {"STAFF", "Staff"}
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
                renderHistory();
                filterBarContainer.removeAll();
                filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
                filterBarContainer.revalidate();
                filterBarContainer.repaint();
            });
            bar.add(btn);
        }
        return bar;
    }

    private void loadHistory() {
        BaseApiClient.ApiResult<List<Notification>> result = ApiClientProvider.getInstance().notifications().getAll();
        allNotifications = result.isSuccess() ? result.getData() : List.of();
        allNotifications = allNotifications.stream()
                .sorted((a, b) -> {
                    if (a.getNotificationDate() == null) return 1;
                    if (b.getNotificationDate() == null) return -1;
                    return b.getNotificationDate().compareTo(a.getNotificationDate());
                })
                .collect(Collectors.toList());
        renderHistory();
    }

    private void renderHistory() {
        historyList.removeAll();

        List<Notification> filtered = allNotifications.stream()
                .filter(this::matchesFilter)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("No notifications yet.");
            empty.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));
            historyList.add(empty);
        } else {
            for (Notification n : filtered) {
                historyList.add(buildHistoryCard(n));
                historyList.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
            }
        }

        historyList.revalidate();
        historyList.repaint();
    }

    private boolean matchesFilter(Notification n) {
        return switch (activeFilter) {
            case "PATIENT" -> n.getPatient() != null;
            case "DOCTOR" -> n.getDoctor() != null;
            case "STAFF" -> n.getClinicStaff() != null;
            default -> true;
        };
    }

    private JComponent buildHistoryCard(Notification n) {
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

        JLabel toLabel = new JLabel("To: " + recipientName(n));
        toLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        toLabel.setForeground(AppTheme.TEXT_PRIMARY);
        toLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

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
        JLabel messagePreview = new JLabel("<html>" + preview + "</html>");
        messagePreview.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        messagePreview.setForeground(AppTheme.TEXT_SECONDARY);
        messagePreview.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(toLabel);
        card.add(metaRow);
        card.add(messagePreview);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                NotificationDetailsDialog.show(NotificationsPage.this, n, recipientName(n),
                        () -> prefillFollowUp(n));
            }
        });

        return card;
    }

    private String recipientName(Notification n) {
        try {
            if (n.getPatient() != null) return fullName(n.getPatient().getName());
            if (n.getDoctor() != null) return "Dr. " + fullName(n.getDoctor().getName());
            if (n.getClinicStaff() != null) return fullName(n.getClinicStaff().getName());
        } catch (Exception ignored) { }
        return "Unknown";
    }

    // ══════════════════════════════ RIGHT — COMPOSE ══════════════════════════════

    private JComponent buildRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)
        ));

        JLabel title = new JLabel("New Notification");
        title.setFont(FontManager.headlineFont(Font.BOLD, 20));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel note = new JLabel("<html>Notifications are tied to a ticket. Select the ticket, "
                + "then who on that ticket should receive it. No email/SMS is actually dispatched yet — "
                + "this just logs the record.</html>");
        note.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        note.setForeground(AppTheme.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setBorder(BorderFactory.createEmptyBorder(4, 0, AppTheme.SPACE_MD, 0));

        panel.add(title);
        panel.add(note);

        panel.add(fieldLabel("Ticket"));
        ticketCombo = new JComboBox<>();
        styleCombo(ticketCombo);
        ticketCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PatientTicket t) {
                    setText("TK-" + String.format("%03d", t.getTicketId()) + " — " + describeTicket(t));
                } else if (value instanceof String s) {
                    setText(s);
                    setForeground(AppTheme.TEXT_MUTED);
                }
                return this;
            }
        });
        ticketCombo.addActionListener(e -> refreshRecipientOptions());
        panel.add(ticketCombo);
        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        panel.add(fieldLabel("Recipient"));
        recipientRoleCombo = new JComboBox<>();
        styleCombo(recipientRoleCombo);
        panel.add(recipientRoleCombo);
        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        panel.add(fieldLabel("Notification Type"));
        notificationTypeCombo = new JComboBox<>(new String[]{TYPE_PLACEHOLDER, "EMAIL", "SMS"});
        styleCombo(notificationTypeCombo);
        panel.add(notificationTypeCombo);
        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        panel.add(fieldLabel("Message"));
        messageArea = new JTextArea(8, 20);
        messageArea.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        panel.add(messageScroll);

        formErrorLabel = new JLabel(" ");
        formErrorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        formErrorLabel.setForeground(AppTheme.STATUS_DANGER);
        formErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formErrorLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));
        panel.add(formErrorLabel);

        JButton sendButton = new JButton("Send Notification");
        sendButton.setFont(FontManager.bodyFont(Font.BOLD, 14));
        sendButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
        sendButton.setBackground(AppTheme.PRIMARY);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        sendButton.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        sendButton.addActionListener(e -> sendNotification());
        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        panel.add(sendButton);

        loadTickets();
        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.bodyFont(Font.BOLD, 12));
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 4, 0));
        return label;
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    private void loadTickets() {
        BaseApiClient.ApiResult<List<PatientTicket>> result = ApiClientProvider.getInstance().patientTickets().getAll();
        allTickets = result.isSuccess() ? result.getData() : List.of();
        ticketCombo.removeAllItems();
        ticketCombo.addItem(TICKET_PLACEHOLDER);
        for (PatientTicket t : allTickets) {
            if (t.getAppointment() != null) {
                ticketCombo.addItem(t);
            }
        }
        ticketCombo.setSelectedIndex(0);
        refreshRecipientOptions();
    }

    private void refreshRecipientOptions() {
        recipientRoleCombo.removeAllItems();
        recipientRoleCombo.addItem(RECIPIENT_PLACEHOLDER);

        Object selected = ticketCombo.getSelectedItem();
        if (!(selected instanceof PatientTicket ticket) || ticket.getAppointment() == null) {
            recipientRoleCombo.setSelectedIndex(0);
            return;
        }

        Appointment appt = ticket.getAppointment();
        if (appt.getPatient() != null) recipientRoleCombo.addItem("Patient — " + fullName(appt.getPatient().getName()));
        if (appt.getDoctor() != null) recipientRoleCombo.addItem("Doctor — Dr. " + fullName(appt.getDoctor().getName()));
        if (appt.getStaff() != null) recipientRoleCombo.addItem("Staff — " + fullName(appt.getStaff().getName()));
        recipientRoleCombo.setSelectedIndex(0);
    }

    private String describeTicket(PatientTicket t) {
        Appointment appt = t.getAppointment();
        if (appt == null) return "(no appointment)";
        String patientName = appt.getPatient() != null ? fullName(appt.getPatient().getName()) : "—";
        return patientName + " · " + (t.getCurrentStatus() != null ? t.getCurrentStatus() : "—");
    }

    private String fullName(Object name) {
        if (name == null) return "—";
        try {
            var firstMethod = name.getClass().getMethod("getFirstName");
            var lastMethod = name.getClass().getMethod("getLastName");
            String first = (String) firstMethod.invoke(name);
            String last = (String) lastMethod.invoke(name);
            return (first != null ? first : "") + " " + (last != null ? last : "");
        } catch (Exception e) {
            return "—";
        }
    }

    private void sendNotification() {
        Object ticketSelection = ticketCombo.getSelectedItem();
        String recipientChoice = (String) recipientRoleCombo.getSelectedItem();
        String notificationType = (String) notificationTypeCombo.getSelectedItem();
        String message = messageArea.getText().trim();

        if (!(ticketSelection instanceof PatientTicket ticket) || ticket.getAppointment() == null) {
            formErrorLabel.setText("Please select a ticket.");
            return;
        }
        if (recipientChoice == null || RECIPIENT_PLACEHOLDER.equals(recipientChoice)) {
            formErrorLabel.setText("Please select a recipient.");
            return;
        }
        if (notificationType == null || TYPE_PLACEHOLDER.equals(notificationType)) {
            formErrorLabel.setText("Please select a notification type.");
            return;
        }
        if (message.isEmpty()) {
            formErrorLabel.setText("Please enter a message.");
            return;
        }
        formErrorLabel.setText(" ");

        Appointment appt = ticket.getAppointment();

        Notification notification = new Notification();
        notification.setNotificationType(notificationType);
        notification.setNotificationStatus("PENDING");
        notification.setNotificationMessage(message);
        notification.setNotificationDate(LocalDateTime.now());
        notification.setTicket(ticket);
        notification.setAppointment(appt);

        if (recipientChoice.startsWith("Patient")) notification.setPatient(appt.getPatient());
        else if (recipientChoice.startsWith("Doctor")) notification.setDoctor(appt.getDoctor());
        else if (recipientChoice.startsWith("Staff")) notification.setClinicStaff(appt.getStaff());

        BaseApiClient.ApiResult<Notification> result = ApiClientProvider.getInstance().notifications().create(notification);

        if (result.isSuccess()) {
            AppDialog.show(this, "Notification Logged",
                    "The notification has been recorded for " + recipientChoice + ".", AppDialog.Type.SUCCESS);
            messageArea.setText("");
            loadTickets(); // resets ticket + recipient combos back to placeholders
            notificationTypeCombo.setSelectedIndex(0);
            loadHistory();
        } else {
            formErrorLabel.setText(result.getMessage() != null ? result.getMessage() : "Unable to send notification.");
        }
    }

    private void prefillFollowUp(Notification original) {
        if (original.getTicket() == null) return;

        for (int i = 0; i < ticketCombo.getItemCount(); i++) {
            Object item = ticketCombo.getItemAt(i);
            if (item instanceof PatientTicket t && t.getTicketId() == original.getTicket().getTicketId()) {
                ticketCombo.setSelectedIndex(i);
                break;
            }
        }

        if (original.getPatient() != null) selectRecipientStartingWith("Patient");
        else if (original.getDoctor() != null) selectRecipientStartingWith("Doctor");
        else if (original.getClinicStaff() != null) selectRecipientStartingWith("Staff");

        messageArea.requestFocusInWindow();
    }

    private void selectRecipientStartingWith(String prefix) {
        for (int i = 0; i < recipientRoleCombo.getItemCount(); i++) {
            String item = recipientRoleCombo.getItemAt(i);
            if (item != null && item.startsWith(prefix)) {
                recipientRoleCombo.setSelectedIndex(i);
                break;
            }
        }
    }
}