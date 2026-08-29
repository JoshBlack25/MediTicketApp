package za.ac.cput.ui.clinicstaff.nurse.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Notification;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.auth.components.PrimaryButton;
import za.ac.cput.ui.clinicstaff.nurse.NurseDashboard;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;
import za.ac.cput.ui.theme.ImageManager;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class DashboardPage extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMMM uuuu");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private final NurseDashboard owner;

    private JLabel greetingLabel;
    private JLabel subtitleLabel;
    private SummaryCard pendingCard, openTicketsCard, paymentsCard, notificationsCard;
    private JPanel workQueueList;
    private JLabel workQueueEmptyLabel;
    private JPanel activityList;
    private JLabel activityEmptyLabel;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JLabel loadErrorLabel;
    private JLabel todaysApptCountLabel;
    private JLabel todaysApptSubLabel;

    private List<Appointment> pendingAppointments = new ArrayList<>();
    private List<Payment> pendingPayments = new ArrayList<>();
    private List<PatientTicket> allTickets = new ArrayList<>();
    private List<Notification> staffNotifications = new ArrayList<>();
    private List<Appointment> todaysAppointments = new ArrayList<>();

    private boolean loading = false;

    public DashboardPage(NurseDashboard owner) {
        this.owner = owner;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(buildContent());
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(AppTheme.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    /** Re-pulls all dashboard data from the backend. Safe to call repeatedly. */
    public void refresh() {
        if (loading) return;
        loading = true;
        loadErrorLabel.setVisible(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String errorMessage = null;

            @Override
            protected Void doInBackground() {
                SessionManager session = SessionManager.getInstance();
                ApiClientProvider api = ApiClientProvider.getInstance();

                BaseApiClient.ApiResult<List<Appointment>> pendingApptResult = api.appointments().findByStatus("PENDING");
                if (pendingApptResult.isSuccess()) {
                    pendingAppointments = pendingApptResult.getData();
                } else {
                    errorMessage = "Couldn't reach the server. Make sure the backend is running.";
                    return null;
                }

                BaseApiClient.ApiResult<List<Payment>> pendingPayResult = api.payments().findByStatus("PENDING");
                pendingPayments = pendingPayResult.isSuccess() ? pendingPayResult.getData() : new ArrayList<>();

                BaseApiClient.ApiResult<List<PatientTicket>> ticketsResult = api.patientTickets().getAll();
                allTickets = ticketsResult.isSuccess() ? ticketsResult.getData() : new ArrayList<>();

                BaseApiClient.ApiResult<List<Notification>> notifResult = api.notifications().findByClinicStaff(session.getUserId());
                staffNotifications = notifResult.isSuccess() ? notifResult.getData() : new ArrayList<>();

                BaseApiClient.ApiResult<List<Appointment>> todayResult = api.appointments().findByDate(LocalDate.now());
                todaysAppointments = todayResult.isSuccess() ? todayResult.getData() : new ArrayList<>();

                return null;
            }

            @Override
            protected void done() {
                loading = false;
                if (errorMessage != null) {
                    loadErrorLabel.setText(errorMessage);
                    loadErrorLabel.setVisible(true);
                    return;
                }
                applyData();
            }
        };
        worker.execute();
    }

    // -- Layout ------------------------------------------------------

    private JComponent buildContent() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_XL, AppTheme.SPACE_LG));

        root.add(buildGreetingSection());
        root.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        root.add(buildQuickActions());
        root.add(Box.createVerticalStrut(AppTheme.SPACE_LG));

        loadErrorLabel = new JLabel(" ");
        loadErrorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        loadErrorLabel.setForeground(AppTheme.STATUS_DANGER);
        loadErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadErrorLabel.setVisible(false);
        root.add(loadErrorLabel);

        root.add(buildBentoGrid());

        return root;
    }

    /**
     * Bento-style overview grid: a 4-column GridBagLayout mixing the four
     * SummaryCards with variable-sized tiles (work queue is the big tile,
     * everything else is a supporting tile) so the dashboard reads as one
     * connected surface instead of a stack of full-width blocks. Two of the
     * tiles (Today's Appointments, Patient Directory) are shortcuts into
     * other parts of the app, built from data already fetched by refresh()
     * -- no extra API calls.
     */
    private JComponent buildBentoGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        int gap = AppTheme.SPACE_MD;

        // Row 0: four summary cards, one per column.
        pendingCard = new SummaryCard("Pending", "\u2014", AppTheme.STATUS_WARNING);
        openTicketsCard = new SummaryCard("Open Tickets", "\u2014", AppTheme.PRIMARY);
        paymentsCard = new SummaryCard("Payments", "\u2014", AppTheme.STATUS_INFO);
        notificationsCard = new SummaryCard("Notifications", "\u2014", AppTheme.STATUS_DANGER);
        JComponent[] summaryCards = { pendingCard, openTicketsCard, paymentsCard, notificationsCard };
        for (int col = 0; col < 4; col++) {
            GridBagConstraints c = bentoConstraints(col, 0, 1, 1, gap);
            c.weighty = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(summaryCards[col], c);
        }

        // Row 1-2, col 0-1: Work Queue -- the big tile.
        GridBagConstraints wq = bentoConstraints(0, 1, 2, 2, gap);
        grid.add(buildWorkQueueCard(), wq);

        // Row 1, col 2: Recent Activity.
        grid.add(buildActivityTile(), bentoConstraints(2, 1, 1, 1, gap));

        // Row 1, col 3: Today's Appointments shortcut (new).
        grid.add(buildTodaysAppointmentsTile(), bentoConstraints(3, 1, 1, 1, gap));

        // Row 2, col 2: Triage Progress.
        grid.add(buildProgressTile(), bentoConstraints(2, 2, 1, 1, gap));

        // Row 2, col 3: Patient Directory shortcut (new).
        grid.add(buildPatientDirectoryTile(), bentoConstraints(3, 2, 1, 1, gap));

        // Row 3: tip banner, full width.
        GridBagConstraints tip = bentoConstraints(0, 3, 4, 1, gap);
        tip.weighty = 0;
        tip.fill = GridBagConstraints.HORIZONTAL;
        grid.add(buildTipBanner(), tip);

        return grid;
    }

    private GridBagConstraints bentoConstraints(int gridx, int gridy, int gridwidth, int gridheight, int gap) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridx;
        c.gridy = gridy;
        c.gridwidth = gridwidth;
        c.gridheight = gridheight;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, gap, gap);
        return c;
    }

    private JComponent buildGreetingSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel eyebrow = new JLabel("CARE COORDINATION PORTAL");
        eyebrow.setFont(FontManager.bodyFont(Font.BOLD, 11));
        eyebrow.setForeground(AppTheme.PRIMARY);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        greetingLabel = new JLabel("Good Morning \uD83D\uDC4B");
        greetingLabel.setFont(FontManager.headlineFont(Font.BOLD, 30));
        greetingLabel.setForeground(AppTheme.TEXT_PRIMARY);
        greetingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        greetingLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));

        subtitleLabel = new JLabel("Welcome back to MediTicket.");
        subtitleLabel.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitleLabel.setForeground(AppTheme.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(eyebrow);
        panel.add(greetingLabel);
        panel.add(subtitleLabel);
        return panel;
    }

    private JComponent buildQuickActions() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton pending = new PrimaryButton("Appointments");
        pending.setPreferredSize(new Dimension(200, 44));
        pending.addActionListener(e -> owner.navigateTo("APPOINTMENTS"));

        JButton tickets = outlineButton("Tickets");
        tickets.addActionListener(e -> owner.navigateTo("TICKETS"));

        JButton payments = outlineButton("Payments");
        payments.addActionListener(e -> owner.navigateTo("PAYMENTS"));

        row.add(pending);
        row.add(tickets);
        row.add(payments);
        return row;
    }

    private JButton outlineButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FontManager.bodyFont(Font.BOLD, 13));
        btn.setForeground(AppTheme.PRIMARY);
        btn.setBackground(AppTheme.SURFACE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(text.length() * 8 + 40, 44));
        return btn;
    }

    private JComponent buildWorkQueueCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout());

        JLabel title = new JLabel("Today's Work Queue");
        title.setFont(FontManager.headlineFont(Font.BOLD, 16));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));
        card.add(title, BorderLayout.NORTH);

        workQueueList = new JPanel();
        workQueueList.setLayout(new BoxLayout(workQueueList, BoxLayout.Y_AXIS));
        workQueueList.setOpaque(false);

        workQueueEmptyLabel = new JLabel("Nothing needs your attention right now.");
        workQueueEmptyLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        workQueueEmptyLabel.setForeground(AppTheme.TEXT_MUTED);
        workQueueEmptyLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));
        workQueueEmptyLabel.setVisible(false);
        workQueueList.add(workQueueEmptyLabel);

        JScrollPane scroll = new JScrollPane(workQueueList);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JComponent buildActivityTile() {
        JPanel activityCard = cardPanel();
        activityCard.setLayout(new BorderLayout());
        activityCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel activityTitle = new JLabel("Recent Activity");
        activityTitle.setFont(FontManager.headlineFont(Font.BOLD, 15));
        activityTitle.setForeground(AppTheme.TEXT_PRIMARY);
        activityTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));
        activityCard.add(activityTitle, BorderLayout.NORTH);

        activityList = new JPanel();
        activityList.setLayout(new BoxLayout(activityList, BoxLayout.Y_AXIS));
        activityList.setOpaque(false);

        activityEmptyLabel = new JLabel("No recent activity yet.");
        activityEmptyLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        activityEmptyLabel.setForeground(AppTheme.TEXT_MUTED);
        activityEmptyLabel.setVisible(false);
        activityList.add(activityEmptyLabel);

        JScrollPane scroll = new JScrollPane(activityList);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        activityCard.add(scroll, BorderLayout.CENTER);

        return activityCard;
    }

    private JComponent buildProgressTile() {
        JPanel progressCard = cardPanel();
        progressCard.setLayout(new BoxLayout(progressCard, BoxLayout.Y_AXIS));
        progressCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel progressTitle = new JLabel("Today's Triage Progress");
        progressTitle.setFont(FontManager.bodyFont(Font.BOLD, 13));
        progressTitle.setForeground(AppTheme.TEXT_PRIMARY);
        progressTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setForeground(AppTheme.PRIMARY);
        progressBar.setBackground(AppTheme.SURFACE_ALT);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 10));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_XS, 0));

        progressLabel = new JLabel("No appointments scheduled today.");
        progressLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        progressLabel.setForeground(AppTheme.TEXT_SECONDARY);
        progressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressCard.add(progressTitle);
        progressCard.add(progressBar);
        progressCard.add(progressLabel);
        progressCard.add(Box.createVerticalGlue());

        return progressCard;
    }

    private JComponent buildTipBanner() {
        JPanel tipCard = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        tipCard.setBackground(AppTheme.PRIMARY_LIGHT);
        tipCard.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD));
        tipCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tipIcon = new JLabel(ImageManager.getIcon(ImageManager.ICON_HEARTBEAT, 26, 26));
        JLabel tipText = new JLabel("<html><b>Tip:</b> Approve or reject pending appointments promptly to keep patients informed.</html>");
        tipText.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        tipText.setForeground(AppTheme.PRIMARY_DARK);

        tipCard.add(tipIcon, BorderLayout.WEST);
        tipCard.add(tipText, BorderLayout.CENTER);
        return tipCard;
    }

    /**
     * New bento tile: surfaces today's appointment count (already fetched
     * for the triage-progress calculation, previously not shown directly)
     * with a one-tap shortcut into the full Appointments page.
     */
    private JComponent buildTodaysAppointmentsTile() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, AppTheme.SPACE_SM));

        JPanel header = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        header.setOpaque(false);
        JLabel icon = new JLabel(ImageManager.getIcon(ImageManager.ICON_CALENDAR, 22, 22));
        JLabel title = new JLabel("Today's Appointments");
        title.setFont(FontManager.bodyFont(Font.BOLD, 13));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        header.add(icon, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        todaysApptCountLabel = new JLabel("\u2014");
        todaysApptCountLabel.setFont(FontManager.headlineFont(Font.BOLD, 30));
        todaysApptCountLabel.setForeground(AppTheme.PRIMARY);
        todaysApptCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        todaysApptSubLabel = new JLabel("scheduled today");
        todaysApptSubLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        todaysApptSubLabel.setForeground(AppTheme.TEXT_SECONDARY);
        todaysApptSubLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        center.add(todaysApptCountLabel);
        center.add(todaysApptSubLabel);
        card.add(center, BorderLayout.CENTER);

        JButton viewBtn = smallActionButton("View Appointments", AppTheme.PRIMARY);
        viewBtn.addActionListener(e -> owner.navigateTo("APPOINTMENTS"));
        card.add(wrapRight(viewBtn), BorderLayout.SOUTH);

        return card;
    }

    /**
     * New bento tile: a static shortcut into the Patient directory so the
     * dashboard doubles as a jumping-off point into other parts of the app,
     * not just a read-only summary.
     */
    private JComponent buildPatientDirectoryTile() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, AppTheme.SPACE_SM));

        JPanel header = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        header.setOpaque(false);
        JLabel icon = new JLabel(ImageManager.getIcon(ImageManager.ICON_PATIENT, 22, 22));
        JLabel title = new JLabel("Patient Directory");
        title.setFont(FontManager.bodyFont(Font.BOLD, 13));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        header.add(icon, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        JLabel desc = new JLabel("<html>Search patient records and profiles.</html>");
        desc.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        desc.setForeground(AppTheme.TEXT_SECONDARY);
        card.add(desc, BorderLayout.CENTER);

        JButton openBtn = smallActionButton("Open Patients", AppTheme.PRIMARY);
        openBtn.addActionListener(e -> owner.navigateTo("PATIENTS"));
        card.add(wrapRight(openBtn), BorderLayout.SOUTH);

        return card;
    }

    private JPanel cardPanel() {
        JPanel card = new JPanel();
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD)
        ));
        return card;
    }

    // -- Data application ---------------------------------------------

    private void applyData() {
        SessionManager session = SessionManager.getInstance();
        String firstName = extractFirstName(session.getFullName());
        greetingLabel.setText("Good " + timeOfDayGreeting() + (firstName.isEmpty() ? "" : ", " + firstName) + " \uD83D\uDC4B");

        int pendingCount = pendingAppointments.size();
        subtitleLabel.setText("<html>Welcome back to MediTicket. Here's an overview of today's clinic activity. "
                + (pendingCount > 0
                    ? "You have <b>" + pendingCount + "</b> new appointment" + (pendingCount == 1 ? "" : "s") + " requiring immediate triaging."
                    : "There's nothing urgent waiting on you right now.")
                + "</html>");

        long openTickets = allTickets.stream()
                .filter(t -> t.getCurrentStatus() != null)
                .filter(t -> !t.getCurrentStatus().equals("CLOSED") && !t.getCurrentStatus().equals("RESOLVED"))
                .count();

        pendingCard.setValue(String.valueOf(pendingCount));
        openTicketsCard.setValue(String.valueOf(openTickets));
        paymentsCard.setValue(String.valueOf(pendingPayments.size()));
        notificationsCard.setValue(String.valueOf(staffNotifications.size()));

        rebuildWorkQueue();
        rebuildActivity();
        rebuildProgress();
        rebuildTodaysAppointmentsTile();

        owner.setHeaderUnreadCount(staffNotifications.size());
    }

    private void rebuildWorkQueue() {
        workQueueList.removeAll();

        List<Appointment> apptsSorted = new ArrayList<>(pendingAppointments);
        apptsSorted.sort(Comparator.comparing(Appointment::getAppointmentDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        for (Appointment appt : apptsSorted) {
            workQueueList.add(buildAppointmentRow(appt));
            workQueueList.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        }
        for (Payment payment : pendingPayments) {
            workQueueList.add(buildPaymentRow(payment));
            workQueueList.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        }

        boolean empty = apptsSorted.isEmpty() && pendingPayments.isEmpty();
        workQueueEmptyLabel.setVisible(empty);
        if (empty) workQueueList.add(workQueueEmptyLabel);

        workQueueList.revalidate();
        workQueueList.repaint();
    }

    private JComponent buildAppointmentRow(Appointment appt) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 2));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.DIVIDER),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_SM, 0)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));

        String patientName = appt.getPatient() != null && appt.getPatient().getName() != null
                ? appt.getPatient().getName().getFullName() : "Unknown patient";

        JLabel name = new JLabel("Patient: " + patientName);
        name.setFont(FontManager.bodyFont(Font.BOLD, 13));
        name.setForeground(AppTheme.TEXT_PRIMARY);

        String dateStr = appt.getAppointmentDate() != null ? appt.getAppointmentDate().format(DATE_FMT) : "Date TBD";
        JLabel meta1 = new JLabel("Requested Date: " + dateStr);
        meta1.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        meta1.setForeground(AppTheme.TEXT_SECONDARY);

        JLabel meta2 = new JLabel("Reason: " + (appt.getReason() != null ? appt.getReason() : "Not specified"));
        meta2.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        meta2.setForeground(AppTheme.TEXT_SECONDARY);

        textCol.add(name);
        textCol.add(meta1);
        textCol.add(meta2);

        JButton reviewBtn = smallActionButton("Review", AppTheme.PRIMARY);
        reviewBtn.addActionListener(e -> showReviewDialog(appt));

        row.add(textCol, BorderLayout.CENTER);
        row.add(wrapRight(reviewBtn), BorderLayout.EAST);
        return row;
    }

    private JComponent buildPaymentRow(Payment payment) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 2));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.DIVIDER),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_SM, 0)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));

        Patient patient = (payment.getAppointment() != null) ? payment.getAppointment().getPatient() : null;
        String patientName = (patient != null && patient.getName() != null) ? patient.getName().getFullName() : "Unknown patient";

        JLabel name = new JLabel("Patient: " + patientName);
        name.setFont(FontManager.bodyFont(Font.BOLD, 13));
        name.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel meta = new JLabel("Amount: " + formatCurrency(payment.getPaymentAmount()));
        meta.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        meta.setForeground(AppTheme.TEXT_SECONDARY);

        textCol.add(name);
        textCol.add(meta);

        JButton verifyBtn = smallActionButton("Verify Payment", AppTheme.STATUS_SUCCESS);
        verifyBtn.addActionListener(e -> showVerifyPaymentDialog(payment));

        row.add(textCol, BorderLayout.CENTER);
        row.add(wrapRight(verifyBtn), BorderLayout.EAST);
        return row;
    }

    private JComponent wrapRight(JComponent comp) {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.add(comp);
        return wrap;
    }

    private JButton smallActionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(FontManager.bodyFont(Font.BOLD, 14));
        btn.setForeground(color);
        btn.setBackground(AppTheme.SURFACE);
        btn.setBorder(BorderFactory.createLineBorder(color, 1, true));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(8, 16, 8, 16));
        return btn;
    }

    private void rebuildActivity() {
        activityList.removeAll();

        List<Notification> sorted = new ArrayList<>(staffNotifications);
        sorted.sort(Comparator.comparing(Notification::getNotificationDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int shown = 0;
        for (Notification n : sorted) {
            if (shown >= 4) break;
            activityList.add(buildActivityItem(n));
            activityList.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
            shown++;
        }

        activityEmptyLabel.setVisible(shown == 0);
        if (shown == 0) activityList.add(activityEmptyLabel);

        activityList.revalidate();
        activityList.repaint();
    }

    private JComponent buildActivityItem(Notification n) {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel message = new JLabel("<html>" + escapeHtml(
                n.getNotificationMessage() != null ? n.getNotificationMessage() : "Notification") + "</html>");
        message.setFont(FontManager.bodyFont(Font.BOLD, 12));
        message.setForeground(AppTheme.TEXT_PRIMARY);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel when = new JLabel(relativeTime(n.getNotificationDate()));
        when.setFont(FontManager.bodyFont(Font.PLAIN, 11));
        when.setForeground(AppTheme.TEXT_MUTED);
        when.setAlignmentX(Component.LEFT_ALIGNMENT);

        item.add(message);
        item.add(when);
        return item;
    }

    private void rebuildProgress() {
        int total = todaysAppointments.size();
        if (total == 0) {
            progressBar.setValue(0);
            progressLabel.setText("No appointments scheduled today.");
            return;
        }
        long triaged = todaysAppointments.stream()
                .filter(a -> a.getConfirmationStatus() != null && !a.getConfirmationStatus().equals("PENDING"))
                .count();
        int percent = (int) Math.round((triaged * 100.0) / total);
        progressBar.setValue(percent);
        progressLabel.setText(percent + "% of today's appointments triaged (" + triaged + " of " + total + ")");
    }

    private void rebuildTodaysAppointmentsTile() {
        int total = todaysAppointments.size();
        todaysApptCountLabel.setText(String.valueOf(total));
        todaysApptSubLabel.setText(total == 1 ? "appointment scheduled today" : "appointments scheduled today");
    }

    // -- Actions ---------------------------------------------------

    private void showReviewDialog(Appointment appt) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Review Appointment", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)
        ));

        JLabel title = new JLabel("Review Appointment");
        title.setFont(FontManager.headlineFont(Font.BOLD, 18));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        String patientName = appt.getPatient() != null && appt.getPatient().getName() != null
                ? appt.getPatient().getName().getFullName() : "Unknown patient";
        String doctorName = appt.getDoctor() != null && appt.getDoctor().getName() != null
                ? "Dr. " + appt.getDoctor().getName().getFullName() : "No doctor assigned";
        String dateStr = appt.getAppointmentDate() != null ? appt.getAppointmentDate().format(DATE_FMT) : "TBD";
        String timeStr = appt.getAppointmentTime() != null ? appt.getAppointmentTime().format(TIME_FMT) : "";

        JLabel details = new JLabel("<html><div style='width:340px;'>"
                + "<b>Patient:</b> " + escapeHtml(patientName) + "<br>"
                + "<b>Doctor:</b> " + escapeHtml(doctorName) + "<br>"
                + "<b>Date:</b> " + dateStr + " " + timeStr + "<br>"
                + "<b>Reason:</b> " + escapeHtml(appt.getReason() != null ? appt.getReason() : "Not specified")
                + "</div></html>");
        details.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        details.setForeground(AppTheme.TEXT_SECONDARY);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, AppTheme.SPACE_LG, 0));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean hasDoctor = appt.getDoctor() != null;

        JButton approveBtn = smallActionButton("Approve", AppTheme.STATUS_SUCCESS);
        approveBtn.setPreferredSize(new Dimension(110, 38));
        approveBtn.setEnabled(hasDoctor);
        if (!hasDoctor) approveBtn.setToolTipText("No doctor assigned to this appointment yet.");
        approveBtn.addActionListener(e -> {
            dialog.dispose();
            performApprove(appt);
        });

        JButton rejectBtn = smallActionButton("Reject", AppTheme.STATUS_DANGER);
        rejectBtn.setPreferredSize(new Dimension(100, 38));
        rejectBtn.addActionListener(e -> {
            dialog.dispose();
            performReject(appt);
        });

        JLabel cancel = new JLabel("Cancel");
        cancel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        cancel.setForeground(AppTheme.TEXT_SECONDARY);
        cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) { dialog.dispose(); }
        });

        buttonRow.add(approveBtn);
        buttonRow.add(rejectBtn);
        buttonRow.add(cancel);

        content.add(title);
        content.add(details);
        content.add(buttonRow);

        dialog.add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void performApprove(Appointment appt) {
        int staffId = SessionManager.getInstance().getUserId();
        int doctorId = appt.getDoctor().getUserId();

        SwingWorker<BaseApiClient.ApiResult<Appointment>, Void> worker = new SwingWorker<>() {
            @Override
            protected BaseApiClient.ApiResult<Appointment> doInBackground() {
                return ApiClientProvider.getInstance().appointments().approve(appt.getAppointmentId(), doctorId, staffId);
            }

            @Override
            protected void done() {
                try {
                    BaseApiClient.ApiResult<Appointment> result = get();
                    if (result.isSuccess()) {
                        AppDialog.show(DashboardPage.this, "Appointment Approved",
                                "The appointment has been confirmed and a ticket created.", AppDialog.Type.SUCCESS);
                        refresh();
                    } else {
                        AppDialog.show(DashboardPage.this, "Couldn't Approve",
                                "The server rejected this action. Please try again.", AppDialog.Type.ERROR);
                    }
                } catch (Exception ex) {
                    AppDialog.show(DashboardPage.this, "Network Error",
                            "Couldn't reach the server.", AppDialog.Type.ERROR);
                }
            }
        };
        worker.execute();
    }

    private void performReject(Appointment appt) {
        int staffId = SessionManager.getInstance().getUserId();
        String reason = JOptionPane.showInputDialog(this, "Reason for rejecting this appointment (optional):");
        if (reason == null) return;

        SwingWorker<BaseApiClient.ApiResult<Appointment>, Void> worker = new SwingWorker<>() {
            @Override
            protected BaseApiClient.ApiResult<Appointment> doInBackground() {
                return ApiClientProvider.getInstance().appointments().reject(appt.getAppointmentId(), staffId, reason);
            }

            @Override
            protected void done() {
                try {
                    BaseApiClient.ApiResult<Appointment> result = get();
                    if (result.isSuccess()) {
                        AppDialog.show(DashboardPage.this, "Appointment Rejected",
                                "The patient will be notified.", AppDialog.Type.SUCCESS);
                        refresh();
                    } else {
                        AppDialog.show(DashboardPage.this, "Couldn't Reject",
                                "The server rejected this action. Please try again.", AppDialog.Type.ERROR);
                    }
                } catch (Exception ex) {
                    AppDialog.show(DashboardPage.this, "Network Error",
                            "Couldn't reach the server.", AppDialog.Type.ERROR);
                }
            }
        };
        worker.execute();
    }

    private void showVerifyPaymentDialog(Payment payment) {
        Patient patient = (payment.getAppointment() != null) ? payment.getAppointment().getPatient() : null;
        String patientName = (patient != null && patient.getName() != null) ? patient.getName().getFullName() : "this patient";

        int choice = JOptionPane.showConfirmDialog(this,
                "Confirm that a payment of " + formatCurrency(payment.getPaymentAmount()) + " from " + patientName + " has been received?",
                "Verify Payment", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        payment.setPaymentStatus("PAID");

        SwingWorker<BaseApiClient.ApiResult<Payment>, Void> worker = new SwingWorker<>() {
            @Override
            protected BaseApiClient.ApiResult<Payment> doInBackground() {
                return ApiClientProvider.getInstance().payments().update(payment);
            }

            @Override
            protected void done() {
                try {
                    BaseApiClient.ApiResult<Payment> result = get();
                    if (result.isSuccess()) {
                        AppDialog.show(DashboardPage.this, "Payment Verified",
                                "The payment has been marked as paid.", AppDialog.Type.SUCCESS);
                        refresh();
                    } else {
                        AppDialog.show(DashboardPage.this, "Couldn't Verify Payment",
                                "The server rejected this action. Please try again.", AppDialog.Type.ERROR);
                    }
                } catch (Exception ex) {
                    AppDialog.show(DashboardPage.this, "Network Error",
                            "Couldn't reach the server.", AppDialog.Type.ERROR);
                }
            }
        };
        worker.execute();
    }

    // -- Formatting helpers ------------------------------------------

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        return fullName.trim().split("\\s+")[0];
    }

    private String timeOfDayGreeting() {
        int hour = LocalDateTime.now().getHour();
        if (hour < 12) return "Morning";
        if (hour < 17) return "Afternoon";
        return "Evening";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "R0.00";
        return "R" + String.format("%,.2f", amount.doubleValue());
    }

    private String relativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        Duration d = Duration.between(dateTime, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min" + (minutes == 1 ? "" : "s") + " ago";
        long hours = d.toHours();
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        long days = d.toDays();
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}