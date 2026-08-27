package za.ac.cput.ui.clinicstaff.admin.pages;

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
 *
 * The page displays notification history.
 * Follow-ups are composed in a popup opened from the
 * Notification Details dialog rather than in a permanent
 * right-side compose panel.
 */
public class NotificationsPage extends JPanel {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM, HH:mm");

    // Left side — history
    private JPanel historyList;
    private String activeFilter = "ALL";
    private JPanel filterBarContainer;
    private List<Notification> allNotifications = List.of();

    private static final String TICKET_PLACEHOLDER = "-- Select a ticket --";
    private static final String RECIPIENT_PLACEHOLDER = "-- Select a recipient --";
    private static final String TYPE_PLACEHOLDER = "-- Select notification type --";

    public NotificationsPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        /*
         * The old page used a 50/50 split with the compose form
         * permanently visible on the right.
         *
         * The compose form is now shown only when the admin clicks
         * "Send Follow-Up" from a notification.
         */
        add(buildLeftPanel(), BorderLayout.CENTER);

        loadHistory();
    }

    // ══════════════════════════════ HISTORY ══════════════════════════════

    private JComponent buildLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel title = new JLabel("Sent Notifications");
        title.setFont(FontManager.headlineFont(Font.BOLD, 20));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "History of notifications sent to patients and staff."
        );
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(
                2, 0, AppTheme.SPACE_MD, 0
        ));

        filterBarContainer = new JPanel(new BorderLayout());
        filterBarContainer.setOpaque(false);
        filterBarContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBarContainer.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, 40
        ));
        filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);

        historyList = new JPanel();
        historyList.setLayout(new BoxLayout(
                historyList, BoxLayout.Y_AXIS
        ));
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
        JPanel bar = new JPanel(new FlowLayout(
                FlowLayout.LEFT,
                AppTheme.SPACE_SM,
                0
        ));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"},
                {"PATIENT", "Patient"},
                {"DOCTOR", "Doctor"},
                {"STAFF", "Staff"}
        };

        for (String[] f : filters) {
            JButton btn = new JButton(f[1]);

            btn.setFont(FontManager.bodyFont(Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
            ));

            btn.setBackground(
                    f[0].equals(activeFilter)
                            ? AppTheme.PRIMARY
                            : AppTheme.SURFACE
            );

            btn.setForeground(
                    f[0].equals(activeFilter)
                            ? AppTheme.TEXT_ON_PRIMARY
                            : AppTheme.TEXT_PRIMARY
            );

            btn.setBorder(BorderFactory.createLineBorder(
                    AppTheme.BORDER,
                    1,
                    true
            ));

            btn.setMargin(new Insets(2, 8, 2, 8));

            btn.addActionListener(e -> {
                activeFilter = f[0];

                renderHistory();

                filterBarContainer.removeAll();
                filterBarContainer.add(
                        buildFilterBar(),
                        BorderLayout.WEST
                );

                filterBarContainer.revalidate();
                filterBarContainer.repaint();
            });

            bar.add(btn);
        }

        return bar;
    }

    private void loadHistory() {
        BaseApiClient.ApiResult<List<Notification>> result =
                ApiClientProvider.getInstance()
                        .notifications()
                        .getAll();

        allNotifications = result.isSuccess()
                ? result.getData()
                : List.of();

        allNotifications = allNotifications.stream()
                .sorted((a, b) -> {
                    if (a.getNotificationDate() == null) return 1;
                    if (b.getNotificationDate() == null) return -1;

                    return b.getNotificationDate()
                            .compareTo(a.getNotificationDate());
                })
                .collect(Collectors.toList());

        renderHistory();
    }

    private void renderHistory() {
        historyList.removeAll();

        List<Notification> filtered =
                allNotifications.stream()
                        .filter(this::matchesFilter)
                        .collect(Collectors.toList());

        if (filtered.isEmpty()) {

            JLabel empty = new JLabel("No notifications yet.");

            empty.setFont(FontManager.bodyFont(
                    Font.PLAIN,
                    13
            ));

            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);

            empty.setBorder(BorderFactory.createEmptyBorder(
                    AppTheme.SPACE_MD,
                    0,
                    0,
                    0
            ));

            historyList.add(empty);

        } else {

            for (Notification n : filtered) {

                historyList.add(buildHistoryCard(n));

                historyList.add(
                        Box.createVerticalStrut(
                                AppTheme.SPACE_SM
                        )
                );
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

        card.setLayout(new BoxLayout(
                card,
                BoxLayout.Y_AXIS
        ));

        card.setBackground(AppTheme.SURFACE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        AppTheme.BORDER,
                        1,
                        true
                ),
                BorderFactory.createEmptyBorder(
                        AppTheme.SPACE_SM,
                        AppTheme.SPACE_MD,
                        AppTheme.SPACE_SM,
                        AppTheme.SPACE_MD
                )
        ));

        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        /*
         * Since there is no longer a right-hand panel,
         * allow the notification cards to use the available width.
         */
        card.setMaximumSize(new Dimension(
                Integer.MAX_VALUE,
                100
        ));

        card.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
        ));

        JLabel toLabel = new JLabel(
                "To: " + recipientName(n)
        );

        toLabel.setFont(FontManager.bodyFont(
                Font.BOLD,
                13
        ));

        toLabel.setForeground(AppTheme.TEXT_PRIMARY);
        toLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel metaRow = new JPanel(new FlowLayout(
                FlowLayout.LEFT,
                6,
                0
        ));

        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        metaRow.setBorder(BorderFactory.createEmptyBorder(
                2,
                0,
                4,
                0
        ));

        JLabel typeTag = new JLabel(
                n.getNotificationType() != null
                        ? n.getNotificationType()
                        : "—"
        );

        typeTag.setFont(FontManager.bodyFont(
                Font.BOLD,
                10
        ));

        typeTag.setForeground(AppTheme.TEXT_MUTED);

        JLabel statusTag = new JLabel(
                "\u00B7 " +
                        (n.getNotificationStatus() != null
                                ? n.getNotificationStatus()
                                : "—")
        );

        statusTag.setFont(FontManager.bodyFont(
                Font.BOLD,
                10
        ));

        statusTag.setForeground(
                AppTheme.statusColor(
                        n.getNotificationStatus()
                )
        );

        JLabel dateTag = new JLabel(
                "\u00B7 " +
                        (n.getNotificationDate() != null
                                ? n.getNotificationDate()
                                .format(DATE_FMT)
                                : "—")
        );

        dateTag.setFont(FontManager.bodyFont(
                Font.PLAIN,
                10
        ));

        dateTag.setForeground(AppTheme.TEXT_MUTED);

        metaRow.add(typeTag);
        metaRow.add(statusTag);
        metaRow.add(dateTag);

        String preview =
                n.getNotificationMessage() != null
                        ? n.getNotificationMessage()
                        : "";

        if (preview.length() > 80) {
            preview = preview.substring(0, 80) + "...";
        }

        JLabel messagePreview = new JLabel(
                "<html>" + preview + "</html>"
        );

        messagePreview.setFont(FontManager.bodyFont(
                Font.PLAIN,
                12
        ));

        messagePreview.setForeground(
                AppTheme.TEXT_SECONDARY
        );

        messagePreview.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        card.add(toLabel);
        card.add(metaRow);
        card.add(messagePreview);

        /*
         * Clicking a notification still opens the existing
         * NotificationDetailsDialog.
         *
         * Its existing "Send Follow-Up" action calls
         * prefillFollowUp(n), which now opens our popup.
         */
        card.addMouseListener(
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            java.awt.event.MouseEvent e
                    ) {

                        NotificationDetailsDialog.show(
                                NotificationsPage.this,
                                n,
                                recipientName(n),
                                () -> prefillFollowUp(n),
                                null
                        );
                    }
                }
        );

        return card;
    }

    private String recipientName(Notification n) {

        try {

            if (n.getPatient() != null) {
                return fullName(
                        n.getPatient().getName()
                );
            }

            if (n.getDoctor() != null) {
                return "Dr. " +
                        fullName(
                                n.getDoctor().getName()
                        );
            }

            if (n.getClinicStaff() != null) {
                return fullName(
                        n.getClinicStaff().getName()
                );
            }

        } catch (Exception ignored) {
        }

        return "Unknown";
    }

    // ══════════════════════════════ FOLLOW-UP POPUP ══════════════════════════════

    /**
     * Opens the follow-up form in a popup.
     *
     * The ticket and recipient are automatically selected
     * based on the original notification.
     */
    private void prefillFollowUp(Notification original) {

        if (original.getTicket() == null) {
            AppDialog.show(
                    this,
                    "Follow-Up Unavailable",
                    "This notification is not linked to a ticket.",
                    AppDialog.Type.ERROR
            );
            return;
        }

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Send Follow-Up",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );

        JPanel panel = buildFollowUpPanel(
                original,
                dialog
        );

        dialog.setContentPane(panel);

        dialog.setSize(520, 600);
        dialog.setMinimumSize(new Dimension(480, 550));
        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }

    private JPanel buildFollowUpPanel(
            Notification original,
            JDialog dialog
    ) {

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(
                panel,
                BoxLayout.Y_AXIS
        ));

        panel.setBackground(AppTheme.SURFACE);

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        AppTheme.BORDER,
                        1,
                        true
                ),
                BorderFactory.createEmptyBorder(
                        AppTheme.SPACE_LG,
                        AppTheme.SPACE_LG,
                        AppTheme.SPACE_LG,
                        AppTheme.SPACE_LG
                )
        ));

        JLabel title = new JLabel("Send Follow-Up");

        title.setFont(FontManager.headlineFont(
                Font.BOLD,
                20
        ));

        title.setForeground(
                AppTheme.TEXT_PRIMARY
        );

        title.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        panel.add(title);

        JLabel note = new JLabel(
                "<html>Send a follow-up notification related to "
                        + "this notification's ticket.</html>"
        );

        note.setFont(FontManager.bodyFont(
                Font.PLAIN,
                12
        ));

        note.setForeground(
                AppTheme.TEXT_MUTED
        );

        note.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        note.setBorder(
                BorderFactory.createEmptyBorder(
                        4,
                        0,
                        AppTheme.SPACE_MD,
                        0
                )
        );

        panel.add(note);

        // ───────────────── Ticket ─────────────────

        panel.add(fieldLabel("Ticket"));

        JComboBox<PatientTicket> ticketCombo =
                new JComboBox<>();

        styleCombo(ticketCombo);

        ticketCombo.setRenderer(
                new DefaultListCellRenderer() {

                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus
                    ) {

                        super.getListCellRendererComponent(
                                list,
                                value,
                                index,
                                isSelected,
                                cellHasFocus
                        );

                        if (value instanceof PatientTicket t) {

                            setText(
                                    "TK-" +
                                            String.format(
                                                    "%03d",
                                                    t.getTicketId()
                                            ) +
                                            " — " +
                                            describeTicket(t)
                            );
                        }

                        return this;
                    }
                }
        );

        PatientTicket originalTicket =
                original.getTicket();

        ticketCombo.addItem(originalTicket);
        ticketCombo.setSelectedIndex(0);
        ticketCombo.setEnabled(false);

        panel.add(ticketCombo);

        panel.add(
                Box.createVerticalStrut(
                        AppTheme.SPACE_SM
                )
        );

        // ───────────────── Recipient ─────────────────

        panel.add(fieldLabel("Recipient"));

        JComboBox<String> recipientCombo =
                new JComboBox<>();

        styleCombo(recipientCombo);

        Appointment appt =
                originalTicket.getAppointment();

        if (appt != null) {

            if (appt.getPatient() != null) {
                recipientCombo.addItem(
                        "Patient — " +
                                fullName(
                                        appt.getPatient().getName()
                                )
                );
            }

            if (appt.getDoctor() != null) {
                recipientCombo.addItem(
                        "Doctor — Dr. " +
                                fullName(
                                        appt.getDoctor().getName()
                                )
                );
            }

            if (appt.getStaff() != null) {
                recipientCombo.addItem(
                        "Staff — " +
                                fullName(
                                        appt.getStaff().getName()
                                )
                );
            }
        }

        /*
         * Automatically select the same recipient as the
         * original notification.
         */
        if (original.getPatient() != null) {
            selectRecipientStartingWith(
                    recipientCombo,
                    "Patient"
            );

        } else if (original.getDoctor() != null) {
            selectRecipientStartingWith(
                    recipientCombo,
                    "Doctor"
            );

        } else if (original.getClinicStaff() != null) {
            selectRecipientStartingWith(
                    recipientCombo,
                    "Staff"
            );
        }

        panel.add(recipientCombo);

        panel.add(
                Box.createVerticalStrut(
                        AppTheme.SPACE_SM
                )
        );

        // ───────────────── Notification Type ─────────────────

        panel.add(fieldLabel("Notification Type"));

        JComboBox<String> notificationTypeCombo =
                new JComboBox<>(
                        new String[]{
                                TYPE_PLACEHOLDER,
                                "EMAIL",
                                "SMS"
                        }
                );

        styleCombo(notificationTypeCombo);

        panel.add(notificationTypeCombo);

        panel.add(
                Box.createVerticalStrut(
                        AppTheme.SPACE_SM
                )
        );

        // ───────────────── Message ─────────────────

        panel.add(fieldLabel("Message"));

        JTextArea messageArea =
                new JTextArea(8, 20);

        messageArea.setFont(
                FontManager.bodyFont(
                        Font.PLAIN,
                        13
                )
        );

        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        messageArea.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                AppTheme.BORDER,
                                1,
                                true
                        ),
                        BorderFactory.createEmptyBorder(
                                8,
                                10,
                                8,
                                10
                        )
                )
        );

        JScrollPane messageScroll =
                new JScrollPane(messageArea);

        messageScroll.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        messageScroll.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        180
                )
        );

        panel.add(messageScroll);

        // ───────────────── Error ─────────────────

        JLabel errorLabel =
                new JLabel(" ");

        errorLabel.setFont(
                FontManager.bodyFont(
                        Font.PLAIN,
                        12
                )
        );

        errorLabel.setForeground(
                AppTheme.STATUS_DANGER
        );

        errorLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        errorLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        AppTheme.SPACE_SM,
                        0,
                        0,
                        0
                )
        );

        panel.add(errorLabel);

        // ───────────────── Buttons ─────────────────

        JPanel buttonRow =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                AppTheme.SPACE_SM,
                                0
                        )
                );

        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JButton cancelButton =
                new JButton("Cancel");

        cancelButton.setFont(
                FontManager.bodyFont(
                        Font.BOLD,
                        13
                )
        );

        cancelButton.setFocusPainted(false);

        cancelButton.addActionListener(
                e -> dialog.dispose()
        );

        JButton sendButton =
                new JButton("Send Follow-Up");

        sendButton.setFont(
                FontManager.bodyFont(
                        Font.BOLD,
                        13
                )
        );

        sendButton.setForeground(
                AppTheme.TEXT_ON_PRIMARY
        );

        sendButton.setBackground(
                AppTheme.PRIMARY
        );

        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);

        sendButton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        sendButton.addActionListener(e -> {

            String recipientChoice =
                    (String) recipientCombo
                            .getSelectedItem();

            String notificationType =
                    (String) notificationTypeCombo
                            .getSelectedItem();

            String message =
                    messageArea.getText().trim();

            // ───────── Validation ─────────

            if (appt == null) {

                errorLabel.setText(
                        "This ticket has no appointment."
                );

                return;
            }

            if (recipientChoice == null ||
                    recipientChoice.equals(
                            RECIPIENT_PLACEHOLDER
                    )) {

                errorLabel.setText(
                        "Please select a recipient."
                );

                return;
            }

            if (notificationType == null ||
                    notificationType.equals(
                            TYPE_PLACEHOLDER
                    )) {

                errorLabel.setText(
                        "Please select a notification type."
                );

                return;
            }

            if (message.isEmpty()) {

                errorLabel.setText(
                        "Please enter a message."
                );

                return;
            }

            errorLabel.setText(" ");

            // ───────── Build notification ─────────

            Notification notification =
                    new Notification();

            notification.setNotificationType(
                    notificationType
            );

            notification.setNotificationStatus(
                    "PENDING"
            );

            notification.setNotificationMessage(
                    message
            );

            notification.setNotificationDate(
                    LocalDateTime.now()
            );

            notification.setTicket(
                    originalTicket
            );

            notification.setAppointment(
                    appt
            );

            if (recipientChoice.startsWith(
                    "Patient"
            )) {

                notification.setPatient(
                        appt.getPatient()
                );

            } else if (recipientChoice.startsWith(
                    "Doctor"
            )) {

                notification.setDoctor(
                        appt.getDoctor()
                );

            } else if (recipientChoice.startsWith(
                    "Staff"
            )) {

                notification.setClinicStaff(
                        appt.getStaff()
                );
            }

            // ───────── Send to backend ─────────

            BaseApiClient.ApiResult<Notification> result =
                    ApiClientProvider
                            .getInstance()
                            .notifications()
                            .create(notification);

            if (result.isSuccess()) {

                dialog.dispose();

                AppDialog.show(
                        this,
                        "Follow-Up Sent",
                        "The follow-up notification has been recorded for "
                                + recipientChoice
                                + ".",
                        AppDialog.Type.SUCCESS
                );

                loadHistory();

            } else {

                errorLabel.setText(
                        result.getMessage() != null
                                ? result.getMessage()
                                : "Unable to send follow-up."
                );
            }
        });

        buttonRow.add(cancelButton);
        buttonRow.add(sendButton);

        panel.add(
                Box.createVerticalStrut(
                        AppTheme.SPACE_SM
                )
        );

        panel.add(buttonRow);

        return panel;
    }

    // ══════════════════════════════ HELPERS ══════════════════════════════

    private JLabel fieldLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(
                FontManager.bodyFont(
                        Font.BOLD,
                        12
                )
        );

        label.setForeground(
                AppTheme.TEXT_PRIMARY
        );

        label.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        label.setBorder(
                BorderFactory.createEmptyBorder(
                        AppTheme.SPACE_SM,
                        0,
                        4,
                        0
                )
        );

        return label;
    }

    private void styleCombo(JComboBox<?> combo) {

        combo.setFont(
                FontManager.bodyFont(
                        Font.PLAIN,
                        13
                )
        );

        combo.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        combo.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        38
                )
        );
    }

    private void selectRecipientStartingWith(
            JComboBox<String> combo,
            String prefix
    ) {

        for (int i = 0;
             i < combo.getItemCount();
             i++) {

            String item =
                    combo.getItemAt(i);

            if (item != null &&
                    item.startsWith(prefix)) {

                combo.setSelectedIndex(i);
                break;
            }
        }
    }

    private String describeTicket(
            PatientTicket t
    ) {

        Appointment appt =
                t.getAppointment();

        if (appt == null) {
            return "(no appointment)";
        }

        String patientName =
                appt.getPatient() != null
                        ? fullName(
                        appt.getPatient().getName()
                )
                        : "—";

        return patientName +
                " · " +
                (t.getCurrentStatus() != null
                        ? t.getCurrentStatus()
                        : "—");
    }

    private String fullName(Object name) {

        if (name == null) {
            return "—";
        }

        try {

            var firstMethod =
                    name.getClass()
                            .getMethod("getFirstName");

            var lastMethod =
                    name.getClass()
                            .getMethod("getLastName");

            String first =
                    (String) firstMethod.invoke(name);

            String last =
                    (String) lastMethod.invoke(name);

            return (first != null ? first : "")
                    + " "
                    + (last != null ? last : "");

        } catch (Exception e) {

            return "—";
        }
    }
}