package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.doctor.DoctorDashboard;
import za.ac.cput.ui.patient.components.StatusBadge;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;
import za.ac.cput.ui.theme.ImageManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class DashboardPage extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private final DoctorDashboard owner;

    private JLabel greetingLabel;
    private JLabel subtitleLabel;
    private SummaryCard todaysAppointmentsCard, openTicketsCard, totalPatientsCard, notificationsCard;
    private JPanel scheduleList;
    private JLabel scheduleEmptyLabel;
    private JPanel ticketQueueList;
    private JLabel ticketQueueEmptyLabel;
    private JPanel activityList;
    private JLabel activityEmptyLabel;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JLabel loadErrorLabel;

    private List<Appointment> myAppointments = new ArrayList<>();
    private List<Appointment> todaysAppointments = new ArrayList<>();
    private List<PatientTicket> myTickets = new ArrayList<>();
    private int notificationCount = 0;

    private boolean loading = false;

    public DashboardPage(DoctorDashboard owner) {
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
                int doctorId = SessionManager.getInstance().getUserId();
                ApiClientProvider api = ApiClientProvider.getInstance();

                BaseApiClient.ApiResult<List<Appointment>> apptResult = api.appointments().findByDoctor(doctorId);
                if (apptResult.isSuccess()) {
                    myAppointments = apptResult.getData();
                } else {
                    errorMessage = "Couldn't reach the server. Make sure the backend is running.";
                    return null;
                }

                BaseApiClient.ApiResult<List<PatientTicket>> ticketResult = api.patientTickets().getAll();
                List<PatientTicket> allTickets = ticketResult.isSuccess() ? ticketResult.getData() : new ArrayList<>();
                myTickets = allTickets.stream()
                        .filter(t -> t.getAppointment() != null
                                && t.getAppointment().getDoctor() != null
                                && t.getAppointment().getDoctor().getUserId() == doctorId)
                        .collect(Collectors.toList());

                var notifResult = api.notifications().findByDoctor(doctorId);
                notificationCount = notifResult.isSuccess() ? notifResult.getData().size() : 0;

                LocalDate today = LocalDate.now();
                todaysAppointments = myAppointments.stream()
                        .filter(a -> today.equals(a.getAppointmentDate()))
                        .filter(a -> !"CANCELLED".equals(a.getConfirmationStatus()) && !"REJECTED".equals(a.getConfirmationStatus()))
                        .collect(Collectors.toList());

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
     * Bento-style overview grid, mirroring the nurse dashboard's layout but
     * re-mapped to a doctor's data: Today's Schedule is the big tile (a
     * doctor's equivalent of the nurse's work queue), Ticket Queue is a
     * medium tile (kept larger than a shortcut since doctors track it
     * closely), and Recent Activity / Consultation Progress / Patient
     * Directory round out the grid.
     */
    private JComponent buildBentoGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        int gap = AppTheme.SPACE_MD;

        // Row 0: four summary cards, one per column.
        todaysAppointmentsCard = new SummaryCard("Today's Appointments", "\u2014", AppTheme.PRIMARY);
        openTicketsCard = new SummaryCard("Open Tickets", "\u2014", AppTheme.STATUS_INFO);
        totalPatientsCard = new SummaryCard("My Patients", "\u2014", AppTheme.ACCENT_DARK);
        notificationsCard = new SummaryCard("Notifications", "\u2014", AppTheme.STATUS_DANGER);
        JComponent[] summaryCards = { todaysAppointmentsCard, openTicketsCard, totalPatientsCard, notificationsCard };
        for (int col = 0; col < 4; col++) {
            GridBagConstraints c = bentoConstraints(col, 0, 1, 1, gap);
            c.weighty = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(summaryCards[col], c);
        }

        // Row 1-2, col 0-1: Today's Schedule -- the big tile.
        GridBagConstraints sched = bentoConstraints(0, 1, 2, 2, gap);
        grid.add(buildScheduleCard(), sched);

        // Row 1-2, col 2: Ticket Queue -- medium tile, taller than a shortcut.
        GridBagConstraints tickets = bentoConstraints(2, 1, 1, 2, gap);
        grid.add(buildTicketQueueCard(), tickets);

        // Row 1, col 3: Recent Activity.
        grid.add(buildActivityTile(), bentoConstraints(3, 1, 1, 1, gap));

        // Row 2, col 3: Consultation Progress.
        grid.add(buildProgressTile(), bentoConstraints(3, 2, 1, 1, gap));

        // Row 3: Patient Directory shortcut, full width.
        GridBagConstraints dir = bentoConstraints(0, 3, 4, 1, gap);
        dir.weighty = 0;
        dir.fill = GridBagConstraints.HORIZONTAL;
        grid.add(buildPatientDirectoryTile(), dir);

        // Row 4: tip banner, full width.
        GridBagConstraints tip = bentoConstraints(0, 4, 4, 1, gap);
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

        subtitleLabel = new JLabel("Here's what's on your plate today.");
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

        JButton appts = outlineButton("Appointments");
        appts.addActionListener(e -> owner.navigateTo("APPOINTMENTS"));

        JButton tickets = outlineButton("Tickets");
        tickets.addActionListener(e -> owner.navigateTo("TICKETS"));

        JButton patients = outlineButton("Patients");
        patients.addActionListener(e -> owner.navigateTo("PATIENTS"));

        row.add(appts);
        row.add(tickets);
        row.add(patients);
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

    private JComponent buildScheduleCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout());

        JLabel title = new JLabel("Today's Schedule");
        title.setFont(FontManager.headlineFont(Font.BOLD, 16));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));
        card.add(title, BorderLayout.NORTH);

        scheduleList = new JPanel();
        scheduleList.setLayout(new BoxLayout(scheduleList, BoxLayout.Y_AXIS));
        scheduleList.setOpaque(false);

        scheduleEmptyLabel = new JLabel("Nothing on your schedule for today.");
        scheduleEmptyLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        scheduleEmptyLabel.setForeground(AppTheme.TEXT_MUTED);
        scheduleEmptyLabel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));
        scheduleEmptyLabel.setVisible(false);
        scheduleList.add(scheduleEmptyLabel);

        JScrollPane scroll = new JScrollPane(scheduleList);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JComponent buildTicketQueueCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout());

        JLabel title = new JLabel("Ticket Queue");
        title.setFont(FontManager.headlineFont(Font.BOLD, 15));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));
        card.add(title, BorderLayout.NORTH);

        ticketQueueList = new JPanel();
        ticketQueueList.setLayout(new BoxLayout(ticketQueueList, BoxLayout.Y_AXIS));
        ticketQueueList.setOpaque(false);

        ticketQueueEmptyLabel = new JLabel("<html>Your ticket queue is clear.</html>");
        ticketQueueEmptyLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        ticketQueueEmptyLabel.setForeground(AppTheme.TEXT_MUTED);
        ticketQueueEmptyLabel.setVisible(false);
        ticketQueueList.add(ticketQueueEmptyLabel);

        JScrollPane scroll = new JScrollPane(ticketQueueList);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        card.add(scroll, BorderLayout.CENTER);

        JButton viewAllBtn = smallActionButton("View All Tickets", AppTheme.PRIMARY);
        viewAllBtn.addActionListener(e -> owner.navigateTo("TICKETS"));
        card.add(wrapRight(viewAllBtn), BorderLayout.SOUTH);

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

        JLabel progressTitle = new JLabel("Consultation Progress");
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
        JLabel tipText = new JLabel("<html><b>Tip:</b> Keep consultation notes up to date so tickets close promptly.</html>");
        tipText.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        tipText.setForeground(AppTheme.PRIMARY_DARK);

        tipCard.add(tipIcon, BorderLayout.WEST);
        tipCard.add(tipText, BorderLayout.CENTER);
        return tipCard;
    }

    /**
     * Shortcut tile into the Patient directory, matching the nurse
     * dashboard's pattern of surfacing navigation, not just read-only data.
     */
    private JComponent buildPatientDirectoryTile() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(AppTheme.SPACE_MD, 0));

        JPanel header = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        header.setOpaque(false);
        JLabel icon = new JLabel(ImageManager.getIcon(ImageManager.ICON_PATIENT, 22, 22));
        JLabel title = new JLabel("Patient Directory");
        title.setFont(FontManager.bodyFont(Font.BOLD, 13));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        header.add(icon, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        card.add(header, BorderLayout.WEST);

        JLabel desc = new JLabel("<html>Search records for patients under your care.</html>");
        desc.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        desc.setForeground(AppTheme.TEXT_SECONDARY);
        card.add(desc, BorderLayout.CENTER);

        JButton openBtn = smallActionButton("Open Patients", AppTheme.PRIMARY);
        openBtn.addActionListener(e -> owner.navigateTo("PATIENTS"));
        card.add(wrapRight(openBtn), BorderLayout.EAST);

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
        String name = SessionManager.getInstance().getFullName();
        String lastName = extractLastName(name);
        greetingLabel.setText("Good " + timeOfDayGreeting() + (lastName.isEmpty() ? "" : ", Dr. " + lastName) + " \uD83D\uDC4B");

        int todayCount = todaysAppointments.size();
        subtitleLabel.setText(todayCount > 0
                ? "You have " + todayCount + " appointment" + (todayCount == 1 ? "" : "s") + " on your schedule today."
                : "Nothing on your schedule for today.");

        long openTicketCount = myTickets.stream()
                .filter(t -> "OPEN".equals(t.getCurrentStatus()) || "IN_PROGRESS".equals(t.getCurrentStatus()))
                .count();

        long uniquePatients = myAppointments.stream()
                .filter(a -> a.getPatient() != null)
                .map(a -> a.getPatient().getUserId())
                .distinct()
                .count();

        todaysAppointmentsCard.setValue(String.valueOf(todayCount));
        openTicketsCard.setValue(String.valueOf(openTicketCount));
        totalPatientsCard.setValue(String.valueOf(uniquePatients));
        notificationsCard.setValue(String.valueOf(notificationCount));

        rebuildSchedule();
        rebuildTicketQueue();
        rebuildActivity();
        rebuildProgress();
    }

    private void rebuildSchedule() {
        scheduleList.removeAll();

        List<Appointment> sorted = todaysAppointments.stream()
                .sorted(Comparator.comparing(a -> a.getAppointmentTime() != null ? a.getAppointmentTime() : LocalTime.MIN))
                .toList();

        for (Appointment appt : sorted) {
            scheduleList.add(buildScheduleRow(appt));
            scheduleList.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        }

        boolean empty = sorted.isEmpty();
        scheduleEmptyLabel.setVisible(empty);
        if (empty) scheduleList.add(scheduleEmptyLabel);

        scheduleList.revalidate();
        scheduleList.repaint();
    }

    private JComponent buildScheduleRow(Appointment appt) {
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

        JLabel name = new JLabel("Patient: " + patientName(appt));
        name.setFont(FontManager.bodyFont(Font.BOLD, 13));
        name.setForeground(AppTheme.TEXT_PRIMARY);

        String timeStr = appt.getAppointmentTime() != null ? appt.getAppointmentTime().format(TIME_FMT) : "Time TBD";
        JLabel meta = new JLabel("Time: " + timeStr);
        meta.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        meta.setForeground(AppTheme.TEXT_SECONDARY);

        textCol.add(name);
        textCol.add(meta);

        JPanel eastCol = new JPanel();
        eastCol.setOpaque(false);
        eastCol.setLayout(new BoxLayout(eastCol, BoxLayout.Y_AXIS));
        eastCol.add(new StatusBadge(appt.getConfirmationStatus()));

        row.add(textCol, BorderLayout.CENTER);
        row.add(wrapRight(eastCol), BorderLayout.EAST);
        return row;
    }

    private void rebuildTicketQueue() {
        ticketQueueList.removeAll();

        List<PatientTicket> active = myTickets.stream()
                .filter(t -> "OPEN".equals(t.getCurrentStatus()) || "IN_PROGRESS".equals(t.getCurrentStatus()))
                .sorted(Comparator.comparing(
                        (PatientTicket t) -> t.getTicketCreatedDate() != null ? t.getTicketCreatedDate() : java.time.LocalDateTime.MAX
                ))
                .toList();

        for (PatientTicket ticket : active) {
            ticketQueueList.add(buildTicketRow(ticket));
            ticketQueueList.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        }

        boolean empty = active.isEmpty();
        ticketQueueEmptyLabel.setVisible(empty);
        if (empty) ticketQueueList.add(ticketQueueEmptyLabel);

        ticketQueueList.revalidate();
        ticketQueueList.repaint();
    }

    private JComponent buildTicketRow(PatientTicket ticket) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 2));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.DIVIDER),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_SM, 0)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        String label = "TK-" + String.format("%03d", ticket.getTicketId()) + "  \u2022  " + ticketPatientName(ticket);
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        textLabel.setForeground(AppTheme.TEXT_PRIMARY);
        String description = ticket.getTicketDescription() != null && !ticket.getTicketDescription().isBlank()
                ? ticket.getTicketDescription() : "No description";
        textLabel.setToolTipText(description);

        row.add(textLabel, BorderLayout.CENTER);
        row.add(wrapRight(new StatusBadge(ticket.getCurrentStatus())), BorderLayout.EAST);
        return row;
    }

    private void rebuildActivity() {
        // Doctor notifications aren't fetched as full objects in this dashboard
        // (only a count), so show a simple placeholder list sized to the count.
        activityList.removeAll();

        boolean empty = notificationCount == 0;
        activityEmptyLabel.setVisible(empty);
        if (empty) {
            activityList.add(activityEmptyLabel);
        } else {
            JLabel summary = new JLabel(notificationCount + " unread notification" + (notificationCount == 1 ? "" : "s") + ".");
            summary.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            summary.setForeground(AppTheme.TEXT_PRIMARY);
            summary.setAlignmentX(Component.LEFT_ALIGNMENT);
            activityList.add(summary);
        }

        activityList.revalidate();
        activityList.repaint();
    }

    private void rebuildProgress() {
        int total = todaysAppointments.size();
        if (total == 0) {
            progressBar.setValue(0);
            progressLabel.setText("No appointments scheduled today.");
            return;
        }
        long completed = todaysAppointments.stream()
                .filter(a -> a.getConfirmationStatus() != null && !a.getConfirmationStatus().equals("PENDING"))
                .count();
        int percent = (int) Math.round((completed * 100.0) / total);
        progressBar.setValue(percent);
        progressLabel.setText(percent + "% of today's appointments handled (" + completed + " of " + total + ")");
    }

    // -- Formatting helpers ------------------------------------------

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

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private String timeOfDayGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Morning";
        if (hour < 17) return "Afternoon";
        return "Evening";
    }

    private String patientName(Appointment appt) {
        if (appt.getPatient() == null || appt.getPatient().getName() == null) return "Unknown patient";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }

    private String ticketPatientName(PatientTicket ticket) {
        if (ticket.getPatient() == null || ticket.getPatient().getName() == null) return "Unknown patient";
        String first = ticket.getPatient().getName().getFirstName();
        String last = ticket.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last.charAt(0) + "." : "");
    }
}