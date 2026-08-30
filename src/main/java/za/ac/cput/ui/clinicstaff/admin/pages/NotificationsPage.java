package za.ac.cput.ui.clinicstaff.admin.pages;
//Raul Everts 230270565
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
 * Admin notification history page.
 * Displays notification history and allows the admin to send
 * a new notification/feedback message from a popup.
 * Notifications are tied to a PatientTicket and its Appointment.
 * The ticket selector includes ALL tickets, including closed tickets.
 */
public class NotificationsPage extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM, HH:mm");

    // ══════════════════════════════ HISTORY ══════════════════════════════
    private static final String TICKET_PLACEHOLDER = "-- Select a ticket --";
    private static final String RECIPIENT_PLACEHOLDER = "-- Select a recipient --";
    private static final String TYPE_PLACEHOLDER = "-- Select notification type --";
    private JPanel historyList;

    // ══════════════════════════════ CONSTANTS ══════════════════════════════
    private String activeFilter = "ALL";
    private JPanel filterBarContainer;
    private List<Notification> allNotifications = List.of();

    public NotificationsPage() {

        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        add(buildMainPanel(), BorderLayout.CENTER);

        loadHistory();
    }

    // ══════════════════════════════ MAIN PAGE ══════════════════════════════

    private JComponent buildMainPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.setOpaque(false);

        panel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        // ───────────────── Header ─────────────────

        JPanel header = new JPanel(new BorderLayout());

        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JPanel titlePanel = new JPanel();

        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Sent Notifications");

        title.setFont(FontManager.headlineFont(Font.BOLD, 20));

        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("History of notifications sent to patients and staff.");

        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));

        subtitle.setForeground(AppTheme.TEXT_SECONDARY);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);

        // ───────────────── Send Feedback Button ─────────────────

        JButton sendFeedbackButton = new JButton("Send Feedback");

        sendFeedbackButton.setFont(FontManager.bodyFont(Font.BOLD, 13));

        sendFeedbackButton.setForeground(AppTheme.TEXT_ON_PRIMARY);

        sendFeedbackButton.setBackground(AppTheme.PRIMARY);

        sendFeedbackButton.setFocusPainted(false);
        sendFeedbackButton.setBorderPainted(false);

        sendFeedbackButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sendFeedbackButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        /*
         * THIS is the action that was missing before.
         *
         * Clicking the page-level button opens the same
         * notification composition functionality, but with
         * the ticket selector unlocked so the admin can choose
         * any ticket.
         */
        sendFeedbackButton.addActionListener(e -> openFeedbackPopup());

        header.add(sendFeedbackButton, BorderLayout.EAST);

        panel.add(header);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        // ───────────────── Filters ─────────────────

        filterBarContainer = new JPanel(new BorderLayout());

        filterBarContainer.setOpaque(false);

        filterBarContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        filterBarContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);

        panel.add(filterBarContainer);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        // ───────────────── History ─────────────────

        historyList = new JPanel();

        historyList.setLayout(new BoxLayout(historyList, BoxLayout.Y_AXIS));

        historyList.setOpaque(false);

        JScrollPane scroll = new JScrollPane(historyList);

        scroll.setBorder(null);

        scroll.getVerticalScrollBar().setUnitIncrement(16);

        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(scroll);

        return panel;
    }

    // ══════════════════════════════ FILTERS ══════════════════════════════

    private JComponent buildFilterBar() {

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));

        bar.setOpaque(false);

        String[][] filters = {{"ALL", "All"}, {"PATIENT", "Patient"}, {"DOCTOR", "Doctor"}, {"STAFF", "Staff"}};

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

    // ══════════════════════════════ LOAD HISTORY ══════════════════════════════

    private void loadHistory() {

        BaseApiClient.ApiResult<List<Notification>> result = ApiClientProvider.getInstance().notifications().getAll();

        allNotifications = result.isSuccess() ? result.getData() : List.of();

        allNotifications = allNotifications.stream().sorted((a, b) -> {

            if (a.getNotificationDate() == null) return 1;

            if (b.getNotificationDate() == null) return -1;

            return b.getNotificationDate().compareTo(a.getNotificationDate());
        }).collect(Collectors.toList());

        renderHistory();
    }

    private void renderHistory() {

        historyList.removeAll();

        List<Notification> filtered = allNotifications.stream().filter(this::matchesFilter).collect(Collectors.toList());

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

    // ══════════════════════════════ HISTORY CARD ══════════════════════════════

    private JComponent buildHistoryCard(Notification n) {

        JPanel card = new JPanel();

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.setBackground(AppTheme.SURFACE);

        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true), BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_MD, AppTheme.SPACE_SM, AppTheme.SPACE_MD)));

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

        if (preview.length() > 80) {

            preview = preview.substring(0, 80) + "...";
        }

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

                NotificationDetailsDialog.show(NotificationsPage.this, n, recipientName(n), () -> prefillFollowUp(n), null);
            }
        });

        return card;
    }

    // ══════════════════════════════ RECIPIENT NAME ══════════════════════════════

    private String recipientName(Notification n) {

        try {

            if (n.getPatient() != null) {

                return fullName(n.getPatient().getName());
            }

            if (n.getDoctor() != null) {

                return "Dr. " + fullName(n.getDoctor().getName());
            }

            if (n.getClinicStaff() != null) {

                return fullName(n.getClinicStaff().getName());
            }

        } catch (Exception ignored) {
        }

        return "Unknown";
    }

    // ══════════════════════════════ FEEDBACK POPUP ══════════════════════════════

    /**
     * Opens the page-level Send Feedback popup.
     * Unlike the notification-details follow-up popup,
     * the ticket is NOT locked to an existing notification.
     * All tickets returned by patientTickets().getAll() are
     * available, including closed tickets.
     */
    private void openFeedbackPopup() {

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Send Feedback", Dialog.ModalityType.APPLICATION_MODAL);

        dialog.setDefaultCloseOperation(2);

        JPanel panel = buildFeedbackPanel(dialog);

        dialog.setContentPane(panel);

        dialog.setSize(520, 600);

        dialog.setMinimumSize(new Dimension(480, 550));

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }

    // ══════════════════════════════ FEEDBACK FORM ══════════════════════════════

    private JPanel buildFeedbackPanel(JDialog dialog) {

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.setBackground(AppTheme.SURFACE);

        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true), BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)));

        // ───────────────── Title ─────────────────

        JLabel title = new JLabel("Send Feedback");

        title.setFont(FontManager.headlineFont(Font.BOLD, 20));

        title.setForeground(AppTheme.TEXT_PRIMARY);

        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(title);

        JLabel note = new JLabel("<html>Choose any ticket and then select " + "who should receive the feedback.</html>");

        note.setFont(FontManager.bodyFont(Font.PLAIN, 12));

        note.setForeground(AppTheme.TEXT_MUTED);

        note.setAlignmentX(Component.LEFT_ALIGNMENT);

        note.setBorder(BorderFactory.createEmptyBorder(4, 0, AppTheme.SPACE_MD, 0));

        panel.add(note);

        // ══════════════════════════════ TICKET ══════════════════════════════

        panel.add(fieldLabel("Ticket"));

        JComboBox<PatientTicket> ticketCombo = new JComboBox<>();

        styleCombo(ticketCombo);

        ticketCombo.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof PatientTicket t) {

                    setText("TK-" + String.format("%03d", t.getTicketId()) + " — " + describeTicket(t));
                }

                return this;
            }
        });

        panel.add(ticketCombo);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        // ══════════════════════════════ RECIPIENT ══════════════════════════════

        panel.add(fieldLabel("Recipient"));

        JComboBox<String> recipientCombo = new JComboBox<>();

        styleCombo(recipientCombo);

        panel.add(recipientCombo);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        // ══════════════════════════════ TYPE ══════════════════════════════

        panel.add(fieldLabel("Notification Type"));

        JComboBox<String> notificationTypeCombo = new JComboBox<>(new String[]{TYPE_PLACEHOLDER, "EMAIL", "SMS"});

        styleCombo(notificationTypeCombo);

        panel.add(notificationTypeCombo);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        // ══════════════════════════════ MESSAGE ══════════════════════════════

        panel.add(fieldLabel("Message"));

        JTextArea messageArea = new JTextArea(8, 20);

        messageArea.setFont(FontManager.bodyFont(Font.PLAIN, 13));

        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        messageArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true), BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JScrollPane messageScroll = new JScrollPane(messageArea);

        messageScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        panel.add(messageScroll);

        // ══════════════════════════════ ERROR ══════════════════════════════

        JLabel errorLabel = new JLabel(" ");

        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));

        errorLabel.setForeground(AppTheme.STATUS_DANGER);

        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        errorLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));

        panel.add(errorLabel);

        // ══════════════════════════════ BUTTONS ══════════════════════════════

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_SM, 0));

        buttonRow.setOpaque(false);

        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelButton = new JButton("Cancel");

        cancelButton.setFont(FontManager.bodyFont(Font.BOLD, 13));

        cancelButton.setFocusPainted(false);

        cancelButton.addActionListener(e -> dialog.dispose());

        JButton sendButton = new JButton("Send Feedback");

        sendButton.setFont(FontManager.bodyFont(Font.BOLD, 13));

        sendButton.setForeground(AppTheme.TEXT_ON_PRIMARY);

        sendButton.setBackground(AppTheme.PRIMARY);

        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);

        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        // ══════════════════════════════ LOAD TICKETS ══════════════════════════════

        BaseApiClient.ApiResult<List<PatientTicket>> ticketResult = ApiClientProvider.getInstance().patientTickets().getAll();

        List<PatientTicket> tickets = ticketResult.isSuccess() ? ticketResult.getData() : List.of();

        /*
         * IMPORTANT:
         *
         * There is intentionally NO check here for ticket status.
         *
         * This means CLOSED tickets remain selectable.
         *
         * This is what the old right-hand compose form did.
         */
        ticketCombo.addItem(null);

        for (PatientTicket ticket : tickets) {

            if (ticket.getAppointment() != null) {

                ticketCombo.addItem(ticket);
            }
        }

        // ══════════════════════════════ TICKET CHANGE ══════════════════════════════

        ticketCombo.addActionListener(e -> {

            refreshRecipientOptions(ticketCombo, recipientCombo);
        });

        /*
         * Default to the FIRST real ticket.
         *
         * This is deliberately not the first notification.
         * It is the first ticket returned by the backend,
         * and the admin can change it immediately.
         */
        if (ticketCombo.getItemCount() > 1) {

            ticketCombo.setSelectedIndex(1);

        } else {

            ticketCombo.setSelectedIndex(0);
        }

        // ══════════════════════════════ SEND ══════════════════════════════

        sendButton.addActionListener(e -> {

            PatientTicket ticket = (PatientTicket) ticketCombo.getSelectedItem();

            String recipientChoice = (String) recipientCombo.getSelectedItem();

            String notificationType = (String) notificationTypeCombo.getSelectedItem();

            String message = messageArea.getText().trim();

            // ───────── Validation ─────────

            if (ticket == null || ticket.getAppointment() == null) {

                errorLabel.setText("Please select a ticket.");

                return;
            }

            if (recipientChoice == null || RECIPIENT_PLACEHOLDER.equals(recipientChoice)) {

                errorLabel.setText("Please select a recipient.");

                return;
            }

            if (notificationType == null || TYPE_PLACEHOLDER.equals(notificationType)) {

                errorLabel.setText("Please select a notification type.");

                return;
            }

            if (message.isEmpty()) {

                errorLabel.setText("Please enter a message.");

                return;
            }

            errorLabel.setText(" ");

            Appointment appt = ticket.getAppointment();

            // ───────── Build Notification ─────────

            Notification notification = new Notification();

            notification.setNotificationType(notificationType);

            notification.setNotificationStatus("PENDING");

            notification.setNotificationMessage(message);

            notification.setNotificationDate(LocalDateTime.now());

            notification.setTicket(ticket);

            notification.setAppointment(appt);

            if (recipientChoice.startsWith("Patient")) {

                notification.setPatient(appt.getPatient());

            } else if (recipientChoice.startsWith("Doctor")) {

                notification.setDoctor(appt.getDoctor());

            } else if (recipientChoice.startsWith("Staff")) {

                notification.setClinicStaff(appt.getStaff());
            }

            // ───────── Send to Backend ─────────

            BaseApiClient.ApiResult<Notification> result = ApiClientProvider.getInstance().notifications().create(notification);

            if (result.isSuccess()) {

                dialog.dispose();

                AppDialog.show(this, "Feedback Sent", "The feedback has been recorded for " + recipientChoice + ".", AppDialog.Type.SUCCESS);

                loadHistory();

            } else {

                errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Unable to send feedback.");
            }
        });

        buttonRow.add(cancelButton);
        buttonRow.add(sendButton);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        panel.add(buttonRow);

        return panel;
    }

    // ══════════════════════════════ RECIPIENT OPTIONS ══════════════════════════════

    private void refreshRecipientOptions(JComboBox<PatientTicket> ticketCombo, JComboBox<String> recipientCombo) {

        recipientCombo.removeAllItems();

        recipientCombo.addItem(RECIPIENT_PLACEHOLDER);

        PatientTicket ticket = (PatientTicket) ticketCombo.getSelectedItem();

        if (ticket == null || ticket.getAppointment() == null) {

            recipientCombo.setSelectedIndex(0);
            return;
        }

        Appointment appt = ticket.getAppointment();

        if (appt.getPatient() != null) {

            recipientCombo.addItem("Patient — " + fullName(appt.getPatient().getName()));
        }

        if (appt.getDoctor() != null) {

            recipientCombo.addItem("Doctor — Dr. " + fullName(appt.getDoctor().getName()));
        }

        if (appt.getStaff() != null) {

            recipientCombo.addItem("Staff — " + fullName(appt.getStaff().getName()));
        }

        /*
         * Default to the first available recipient.
         */
        if (recipientCombo.getItemCount() > 1) {

            recipientCombo.setSelectedIndex(1);

        } else {

            recipientCombo.setSelectedIndex(0);
        }
    }

    // ══════════════════════════════ FOLLOW-UP FROM DETAILS ══════════════════════════════

    /**
     * Existing Send Follow-Up functionality from the
     * Notification Details popup.
     * This remains separate from the page-level Send Feedback
     * button because a follow-up is supposed to start with
     * the notification's existing ticket.
     */
    private void prefillFollowUp(Notification original) {

        if (original.getTicket() == null) {

            AppDialog.show(this, "Follow-Up Unavailable", "This notification is not linked to a ticket.", AppDialog.Type.ERROR);

            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Send Follow-Up", Dialog.ModalityType.APPLICATION_MODAL);

        dialog.setDefaultCloseOperation(2);

        JPanel panel = buildFollowUpPanel(original, dialog);

        dialog.setContentPane(panel);

        dialog.setSize(520, 600);

        dialog.setMinimumSize(new Dimension(480, 550));

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }

    // ══════════════════════════════ FOLLOW-UP FORM ══════════════════════════════

    private JPanel buildFollowUpPanel(Notification original, JDialog dialog) {

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.setBackground(AppTheme.SURFACE);

        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true), BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)));

        JLabel title = new JLabel("Send Follow-Up");

        title.setFont(FontManager.headlineFont(Font.BOLD, 20));

        title.setForeground(AppTheme.TEXT_PRIMARY);

        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(title);

        JLabel note = new JLabel("<html>Send a follow-up notification related to " + "this notification's ticket.</html>");

        note.setFont(FontManager.bodyFont(Font.PLAIN, 12));

        note.setForeground(AppTheme.TEXT_MUTED);

        note.setAlignmentX(Component.LEFT_ALIGNMENT);

        note.setBorder(BorderFactory.createEmptyBorder(4, 0, AppTheme.SPACE_MD, 0));

        panel.add(note);

        // ───────────────── Ticket ─────────────────

        panel.add(fieldLabel("Ticket"));

        JComboBox<PatientTicket> ticketCombo = new JComboBox<>();

        styleCombo(ticketCombo);

        ticketCombo.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof PatientTicket t) {

                    setText("TK-" + String.format("%03d", t.getTicketId()) + " — " + describeTicket(t));
                }

                return this;
            }
        });

        PatientTicket originalTicket = original.getTicket();

        ticketCombo.addItem(originalTicket);

        ticketCombo.setSelectedIndex(0);

        /*
         * Follow-up from a notification remains tied to
         * the original ticket, so this combo stays disabled.
         */
        ticketCombo.setEnabled(false);

        panel.add(ticketCombo);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        // ───────────────── Recipient ─────────────────

        panel.add(fieldLabel("Recipient"));

        JComboBox<String> recipientCombo = new JComboBox<>();

        styleCombo(recipientCombo);

        Appointment appt = originalTicket.getAppointment();

        if (appt != null) {

            if (appt.getPatient() != null) {

                recipientCombo.addItem("Patient — " + fullName(appt.getPatient().getName()));
            }

            if (appt.getDoctor() != null) {

                recipientCombo.addItem("Doctor — Dr. " + fullName(appt.getDoctor().getName()));
            }

            if (appt.getStaff() != null) {

                recipientCombo.addItem("Staff — " + fullName(appt.getStaff().getName()));
            }
        }

        if (original.getPatient() != null) {

            selectRecipientStartingWith(recipientCombo, "Patient");

        } else if (original.getDoctor() != null) {

            selectRecipientStartingWith(recipientCombo, "Doctor");

        } else if (original.getClinicStaff() != null) {

            selectRecipientStartingWith(recipientCombo, "Staff");
        }

        panel.add(recipientCombo);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        // ───────────────── Type ─────────────────

        panel.add(fieldLabel("Notification Type"));

        JComboBox<String> notificationTypeCombo = new JComboBox<>(new String[]{TYPE_PLACEHOLDER, "EMAIL", "SMS"});

        styleCombo(notificationTypeCombo);

        panel.add(notificationTypeCombo);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        // ───────────────── Message ─────────────────

        panel.add(fieldLabel("Message"));

        JTextArea messageArea = new JTextArea(8, 20);

        messageArea.setFont(FontManager.bodyFont(Font.PLAIN, 13));

        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        messageArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true), BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JScrollPane messageScroll = new JScrollPane(messageArea);

        messageScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        panel.add(messageScroll);

        // ───────────────── Error ─────────────────

        JLabel errorLabel = new JLabel(" ");

        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));

        errorLabel.setForeground(AppTheme.STATUS_DANGER);

        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        errorLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, 0, 0));

        panel.add(errorLabel);

        // ───────────────── Buttons ─────────────────

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACE_SM, 0));

        buttonRow.setOpaque(false);

        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelButton = new JButton("Cancel");

        cancelButton.setFont(FontManager.bodyFont(Font.BOLD, 13));

        cancelButton.setFocusPainted(false);

        cancelButton.addActionListener(e -> dialog.dispose());

        JButton sendButton = new JButton("Send Follow-Up");

        sendButton.setFont(FontManager.bodyFont(Font.BOLD, 13));

        sendButton.setForeground(AppTheme.TEXT_ON_PRIMARY);

        sendButton.setBackground(AppTheme.PRIMARY);

        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);

        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        sendButton.addActionListener(e -> {

            String recipientChoice = (String) recipientCombo.getSelectedItem();

            String notificationType = (String) notificationTypeCombo.getSelectedItem();

            String message = messageArea.getText().trim();

            if (appt == null) {

                errorLabel.setText("This ticket has no appointment.");

                return;
            }

            if (recipientChoice == null || recipientChoice.equals(RECIPIENT_PLACEHOLDER)) {

                errorLabel.setText("Please select a recipient.");

                return;
            }

            if (notificationType == null || notificationType.equals(TYPE_PLACEHOLDER)) {

                errorLabel.setText("Please select a notification type.");

                return;
            }

            if (message.isEmpty()) {

                errorLabel.setText("Please enter a message.");

                return;
            }

            errorLabel.setText(" ");

            Notification notification = new Notification();

            notification.setNotificationType(notificationType);

            notification.setNotificationStatus("PENDING");

            notification.setNotificationMessage(message);

            notification.setNotificationDate(LocalDateTime.now());

            notification.setTicket(originalTicket);

            notification.setAppointment(appt);

            if (recipientChoice.startsWith("Patient")) {

                notification.setPatient(appt.getPatient());

            } else if (recipientChoice.startsWith("Doctor")) {

                notification.setDoctor(appt.getDoctor());

            } else if (recipientChoice.startsWith("Staff")) {

                notification.setClinicStaff(appt.getStaff());
            }

            BaseApiClient.ApiResult<Notification> result = ApiClientProvider.getInstance().notifications().create(notification);

            if (result.isSuccess()) {

                dialog.dispose();

                AppDialog.show(this, "Follow-Up Sent", "The follow-up notification has been recorded for " + recipientChoice + ".", AppDialog.Type.SUCCESS);

                loadHistory();

            } else {

                errorLabel.setText(result.getMessage() != null ? result.getMessage() : "Unable to send follow-up.");
            }
        });

        buttonRow.add(cancelButton);
        buttonRow.add(sendButton);

        panel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));

        panel.add(buttonRow);

        return panel;
    }

    // ══════════════════════════════ HELPERS ══════════════════════════════

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

    private void selectRecipientStartingWith(JComboBox<String> combo, String prefix) {

        for (int i = 0; i < combo.getItemCount(); i++) {

            String item = combo.getItemAt(i);

            if (item != null && item.startsWith(prefix)) {

                combo.setSelectedIndex(i);
                break;
            }
        }
    }

    private String describeTicket(PatientTicket t) {

        Appointment appt = t.getAppointment();

        if (appt == null) {

            return "(no appointment)";
        }

        String patientName = appt.getPatient() != null ? fullName(appt.getPatient().getName()) : "—";

        return patientName + " · " + (t.getCurrentStatus() != null ? t.getCurrentStatus() : "—");
    }

    private String fullName(Object name) {

        if (name == null) {
            return "—";
        }

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
}

