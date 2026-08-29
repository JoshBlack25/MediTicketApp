// za.ac.cput.ui.doctor.pages.DashboardPage
package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient.ApiResult;
import za.ac.cput.model.domain.*;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.ActivityRow;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.doctor.DoctorDashboard;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Doctor home dashboard. Pulls this doctor's appointments, tickets,
 * and notifications, and renders them as stat cards + scannable
 * sections. All network calls run off the EDT via SwingWorker, since
 * BaseApiClient's HttpClient.send() is blocking.
 */
public class DashboardPage extends JPanel {

    private final Consumer<String> onNavigate;
    private final int doctorId;

    private SummaryCard todayCard;
    private SummaryCard pendingTicketsCard;
    private SummaryCard totalPatientsCard;
    private SummaryCard thisWeekCard;

    private JPanel todayAppointmentsContainer;
    private JPanel pendingTicketsContainer;
    private JPanel recentPatientsContainer;
    private JPanel recentActivityContainer;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMM");

    /**
     * @param onNavigate called with a DoctorDashboard.PAGE_* key when a quick
     *                    action is clicked, so the dashboard shell can switch
     *                    pages and update the sidebar selection.
     */
    public DashboardPage(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        this.doctorId = SessionManager.getInstance().getUserId();

        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildQuickActions());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildStatsRow());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildMainGrid());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND);

        add(scroll, BorderLayout.CENTER);

        loadDashboardData();
    }

    // ───────────────────────── Header ─────────────────────────

    private JComponent buildHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String fullName = SessionManager.getInstance().getFullName();
        String displayName = (fullName != null && !fullName.isBlank()) ? lastToken(fullName) : "Doctor";

        JLabel greeting = new JLabel(greetingPrefix() + ", Dr. " + displayName);
        greeting.setFont(FontManager.headlineFont(Font.BOLD, 26));
        greeting.setForeground(AppTheme.TEXT_PRIMARY);
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Here's what's on your schedule today.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        panel.add(greeting);
        panel.add(subtitle);
        return panel;
    }

    private String greetingPrefix() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private String lastToken(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    // ───────────────────────── Quick actions ─────────────────────────

    private JComponent buildQuickActions() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(quickActionButton("\uD83D\uDCC5  View Appointments", DoctorDashboard.PAGE_APPOINTMENTS));
        row.add(quickActionButton("\uD83C\uDFAB  View Tickets", DoctorDashboard.PAGE_TICKETS));
        row.add(quickActionButton("\uD83D\uDC65  View Patients", DoctorDashboard.PAGE_PATIENTS));

        return row;
    }

    private JButton quickActionButton(String label, String pageKey) {
        JButton button = new JButton(label);
        button.setFont(FontManager.bodyFont(Font.BOLD, 13));
        button.setForeground(AppTheme.TEXT_ON_PRIMARY);
        button.setBackground(AppTheme.PRIMARY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> onNavigate.accept(pageKey));
        return button;
    }

    // ───────────────────────── Stat cards ─────────────────────────

    private JComponent buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, AppTheme.SPACE_MD, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        todayCard = new SummaryCard("Today's Appointments", "—", AppTheme.PRIMARY);
        pendingTicketsCard = new SummaryCard("Pending Tickets", "—", AppTheme.STATUS_WARNING);
        totalPatientsCard = new SummaryCard("Total Patients", "—", AppTheme.STATUS_INFO);
        thisWeekCard = new SummaryCard("This Week", "—", AppTheme.ACCENT_DARK);

        row.add(todayCard);
        row.add(pendingTicketsCard);
        row.add(totalPatientsCard);
        row.add(thisWeekCard);
        return row;
    }

    // ───────────────────────── Main grid ─────────────────────────

    private JComponent buildMainGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        todayAppointmentsContainer = new JPanel();
        todayAppointmentsContainer.setLayout(new BoxLayout(todayAppointmentsContainer, BoxLayout.Y_AXIS));
        todayAppointmentsContainer.setOpaque(false);
        left.add(buildSectionPanel("Today's Appointments", todayAppointmentsContainer));
        left.add(Box.createVerticalStrut(AppTheme.SPACE_LG));

        recentPatientsContainer = new JPanel();
        recentPatientsContainer.setLayout(new BoxLayout(recentPatientsContainer, BoxLayout.Y_AXIS));
        recentPatientsContainer.setOpaque(false);
        left.add(buildSectionPanel("Upcoming Patients", recentPatientsContainer));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        pendingTicketsContainer = new JPanel();
        pendingTicketsContainer.setLayout(new BoxLayout(pendingTicketsContainer, BoxLayout.Y_AXIS));
        pendingTicketsContainer.setOpaque(false);
        right.add(buildSectionPanel("Pending Tickets", pendingTicketsContainer));
        right.add(Box.createVerticalStrut(AppTheme.SPACE_LG));

        recentActivityContainer = new JPanel();
        recentActivityContainer.setLayout(new BoxLayout(recentActivityContainer, BoxLayout.Y_AXIS));
        recentActivityContainer.setOpaque(false);
        right.add(buildSectionPanel("Recent Activity", recentActivityContainer));

        gbc.gridx = 0;
        gbc.weightx = 0.62;
        gbc.insets = new Insets(0, 0, 0, AppTheme.SPACE_LG);
        grid.add(left, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.38;
        gbc.insets = new Insets(0, 0, 0, 0);
        grid.add(right, gbc);

        return grid;
    }

    private JComponent buildSectionPanel(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 15));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ───────────────────────── Data loading ─────────────────────────

    private void loadDashboardData() {
        setSectionLoading(todayAppointmentsContainer, "Loading appointments…");
        setSectionLoading(pendingTicketsContainer, "Loading tickets…");
        setSectionLoading(recentPatientsContainer, "Loading patients…");
        setSectionLoading(recentActivityContainer, "Loading activity…");

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() {
                DashboardData data = new DashboardData();
                data.todayAppointments = ApiClientProvider.getInstance().appointments()
                        .findByDoctorAndDate(doctorId, LocalDate.now());
                data.allAppointments = ApiClientProvider.getInstance().appointments()
                        .findByDoctor(doctorId);
                data.allTickets = ApiClientProvider.getInstance().patientTickets().getAll();
                data.notifications = ApiClientProvider.getInstance().notifications()
                        .findByDoctor(doctorId);
                return data;
            }

            @Override
            protected void done() {
                try {
                    applyData(get());
                } catch (Exception e) {
                    showLoadError();
                }
            }
        }.execute();
    }

    private static class DashboardData {
        ApiResult<List<Appointment>> todayAppointments;
        ApiResult<List<Appointment>> allAppointments;
        ApiResult<List<PatientTicket>> allTickets;
        ApiResult<List<Notification>> notifications;
    }

    private void applyData(DashboardData data) {
        applyTodayAppointments(data.todayAppointments);
        applyTicketsAndStats(data.allTickets);
        applyPatientsAndWeekStat(data.allAppointments);
        applyRecentActivity(data.notifications);
    }

    private void applyTodayAppointments(ApiResult<List<Appointment>> result) {
        todayAppointmentsContainer.removeAll();
        if (result == null || !result.isSuccess()) {
            todayAppointmentsContainer.add(emptyStateLabel("Couldn't load today's appointments."));
            todayCard.setValue("—");
        } else {
            List<Appointment> list = result.getData();
            todayCard.setValue(String.valueOf(list.size()));
            if (list.isEmpty()) {
                todayAppointmentsContainer.add(emptyStateLabel("No appointments scheduled for today."));
            } else {
                list.sort(Comparator.comparing(Appointment::getAppointmentTime,
                        Comparator.nullsLast(Comparator.<LocalTime>naturalOrder())));
                for (Appointment a : list) {
                    todayAppointmentsContainer.add(buildAppointmentRow(a));
                    todayAppointmentsContainer.add(Box.createVerticalStrut(AppTheme.SPACE_XS));
                }
            }
        }
        todayAppointmentsContainer.revalidate();
        todayAppointmentsContainer.repaint();
    }

    private void applyTicketsAndStats(ApiResult<List<PatientTicket>> result) {
        pendingTicketsContainer.removeAll();
        if (result == null || !result.isSuccess()) {
            pendingTicketsContainer.add(emptyStateLabel("Couldn't load tickets."));
            pendingTicketsCard.setValue("—");
        } else {
            List<PatientTicket> mine = result.getData().stream()
                    .filter(t -> t.getAppointment() != null
                            && t.getAppointment().getDoctor() != null
                            && t.getAppointment().getDoctor().getUserId() == doctorId)
                    .filter(t -> !"RESOLVED".equals(t.getCurrentStatus()) && !"CLOSED".equals(t.getCurrentStatus()))
                    .sorted(Comparator.comparing(PatientTicket::getTicketCreatedDate,
                            Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                    .collect(Collectors.toList());

            pendingTicketsCard.setValue(String.valueOf(mine.size()));
            if (mine.isEmpty()) {
                pendingTicketsContainer.add(emptyStateLabel("No pending tickets. You're all caught up."));
            } else {
                for (PatientTicket t : mine.stream().limit(6).collect(Collectors.toList())) {
                    pendingTicketsContainer.add(buildTicketRow(t));
                    pendingTicketsContainer.add(Box.createVerticalStrut(AppTheme.SPACE_XS));
                }
            }
        }
        pendingTicketsContainer.revalidate();
        pendingTicketsContainer.repaint();
    }

    private void applyPatientsAndWeekStat(ApiResult<List<Appointment>> result) {
        recentPatientsContainer.removeAll();
        if (result == null || !result.isSuccess()) {
            recentPatientsContainer.add(emptyStateLabel("Couldn't load patients."));
            totalPatientsCard.setValue("—");
            thisWeekCard.setValue("—");
        } else {
            List<Appointment> all = result.getData();

            long distinctPatients = all.stream()
                    .filter(a -> a.getPatient() != null)
                    .map(a -> a.getPatient().getUserId())
                    .distinct()
                    .count();
            totalPatientsCard.setValue(String.valueOf(distinctPatients));

            LocalDate today = LocalDate.now();
            LocalDate weekEnd = today.plusDays(7);
            long weekCount = all.stream()
                    .filter(a -> a.getAppointmentDate() != null)
                    .filter(a -> !a.getAppointmentDate().isBefore(today) && a.getAppointmentDate().isBefore(weekEnd))
                    .filter(a -> "CONFIRMED".equals(a.getConfirmationStatus()) || "PENDING".equals(a.getConfirmationStatus()))
                    .count();
            thisWeekCard.setValue(String.valueOf(weekCount));

            Map<Integer, Appointment> nextByPatient = new LinkedHashMap<>();
            all.stream()
                    .filter(a -> a.getPatient() != null && a.getAppointmentDate() != null)
                    .filter(a -> !a.getAppointmentDate().isBefore(today))
                    .sorted(Comparator.comparing(Appointment::getAppointmentDate))
                    .forEach(a -> nextByPatient.putIfAbsent(a.getPatient().getUserId(), a));

            List<Appointment> upcoming = nextByPatient.values().stream()
                    .sorted(Comparator.comparing(Appointment::getAppointmentDate))
                    .limit(6)
                    .collect(Collectors.toList());

            if (upcoming.isEmpty()) {
                recentPatientsContainer.add(emptyStateLabel("No upcoming patients."));
            } else {
                for (Appointment a : upcoming) {
                    String name = a.getPatient().getName() != null
                            ? a.getPatient().getName().getFullName() : "Unknown patient";
                    String date = a.getAppointmentDate().format(DATE_FMT);
                    recentPatientsContainer.add(new ActivityRow(name, date, AppTheme.TEXT_SECONDARY));
                }
            }
        }
        recentPatientsContainer.revalidate();
        recentPatientsContainer.repaint();
    }

    private void applyRecentActivity(ApiResult<List<Notification>> result) {
        recentActivityContainer.removeAll();
        if (result == null || !result.isSuccess()) {
            recentActivityContainer.add(emptyStateLabel("Couldn't load recent activity."));
        } else {
            List<Notification> list = result.getData().stream()
                    .filter(n -> n.getNotificationDate() != null)
                    .sorted(Comparator.comparing(Notification::getNotificationDate).reversed())
                    .limit(6)
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                recentActivityContainer.add(emptyStateLabel("No recent activity."));
            } else {
                for (Notification n : list) {
                    String text = (n.getNotificationMessage() != null && !n.getNotificationMessage().isBlank())
                            ? n.getNotificationMessage()
                            : n.getNotificationType() + " notification";
                    recentActivityContainer.add(ActivityRow.textRow(text + "  ·  " + relativeTime(n.getNotificationDate())));
                }
            }
        }
        recentActivityContainer.revalidate();
        recentActivityContainer.repaint();
    }

    private void showLoadError() {
        setSectionLoading(todayAppointmentsContainer, "Something went wrong loading your dashboard.");
        setSectionLoading(pendingTicketsContainer, "Something went wrong.");
        setSectionLoading(recentPatientsContainer, "Something went wrong.");
        setSectionLoading(recentActivityContainer, "Something went wrong.");
    }

    // ───────────────────────── Row builders ─────────────────────────

    private JComponent buildAppointmentRow(Appointment a) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        String time = a.getAppointmentTime() != null ? a.getAppointmentTime().format(TIME_FMT) : "—";
        String patientName = (a.getPatient() != null && a.getPatient().getName() != null)
                ? a.getPatient().getName().getFullName() : "Unknown patient";

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel nameLabel = new JLabel(patientName);
        nameLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);

        String detail = time + (a.getReason() != null && !a.getReason().isBlank() ? "  ·  " + a.getReason() : "");
        JLabel detailLabel = new JLabel(detail);
        detailLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        detailLabel.setForeground(AppTheme.TEXT_SECONDARY);

        textStack.add(nameLabel);
        textStack.add(detailLabel);

        row.add(textStack, BorderLayout.CENTER);
        row.add(buildStatusBadge(a.getConfirmationStatus()), BorderLayout.EAST);
        return row;
    }

    private JComponent buildTicketRow(PatientTicket t) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        String patientName = (t.getPatient() != null && t.getPatient().getName() != null)
                ? t.getPatient().getName().getFullName() : "Unknown patient";
        String description = t.getTicketDescription() != null ? t.getTicketDescription() : "";
        if (description.length() > 40) description = description.substring(0, 40) + "…";

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel nameLabel = new JLabel(patientName);
        nameLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        descLabel.setForeground(AppTheme.TEXT_SECONDARY);

        textStack.add(nameLabel);
        textStack.add(descLabel);

        row.add(textStack, BorderLayout.CENTER);
        row.add(buildStatusBadge(t.getCurrentStatus()), BorderLayout.EAST);
        return row;
    }

    private JComponent buildStatusBadge(String status) {
        String label = status != null ? status.replace('_', ' ') : "UNKNOWN";
        JLabel badge = new JLabel(label);
        badge.setOpaque(true);
        badge.setBackground(AppTheme.statusBackground(status));
        badge.setForeground(AppTheme.statusColor(status));
        badge.setFont(FontManager.bodyFont(Font.BOLD, 10));
        badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return badge;
    }

    private JComponent emptyStateLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        label.setForeground(AppTheme.TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_SM, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void setSectionLoading(JPanel container, String text) {
        container.removeAll();
        container.add(emptyStateLabel(text));
        container.revalidate();
        container.repaint();
    }

    private String relativeTime(LocalDateTime dt) {
        long minutes = ChronoUnit.MINUTES.between(dt, LocalDateTime.now());
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }
}