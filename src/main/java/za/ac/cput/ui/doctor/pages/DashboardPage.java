// za.ac.cput.ui.doctor.pages.DashboardPage
package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.PatientTicket;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.patient.components.ElevatedCard;
import za.ac.cput.ui.patient.components.StatusBadge;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class DashboardPage extends JPanel {

    private SummaryCard todaysAppointmentsCard;
    private SummaryCard pendingTicketsCard;
    private SummaryCard totalPatientsCard;
    private SummaryCard notificationsCard;

    private JPanel scheduleBody;
    private JPanel queueBody;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    public DashboardPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildGreeting());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildSummaryCards());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildTwoColumnSection());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    private JComponent buildGreeting() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String name = SessionManager.getInstance().getFullName();
        String lastName = name != null && !name.isBlank() ? name.split(" ")[name.split(" ").length - 1] : "there";

        JLabel greeting = new JLabel(
                "<html>" + greetingForTime() + ", Dr. " + lastName
                        + " <span style='font-family:" + Font.SANS_SERIF + ";'>\uD83D\uDC4B</span></html>"
        );
        greeting.setFont(FontManager.headlineFont(Font.BOLD, 26));
        greeting.setForeground(AppTheme.TEXT_PRIMARY);
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Here's what's on your plate today.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(greeting);
        panel.add(subtitle);
        return panel;
    }

    private String greetingForTime() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    private JComponent buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        todaysAppointmentsCard = new SummaryCard("Today's Appointments", "\u2014", AppTheme.PRIMARY);
        pendingTicketsCard = new SummaryCard("Open Tickets", "\u2014", AppTheme.STATUS_INFO);
        totalPatientsCard = new SummaryCard("My Patients", "\u2014", AppTheme.ACCENT_DARK);
        notificationsCard = new SummaryCard("Notifications", "\u2014", AppTheme.STATUS_DANGER);

        grid.add(todaysAppointmentsCard);
        grid.add(pendingTicketsCard);
        grid.add(totalPatientsCard);
        grid.add(notificationsCard);
        return grid;
    }

    private JComponent buildTwoColumnSection() {
        JPanel columns = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_LG, 0));
        columns.setOpaque(false);
        columns.setAlignmentX(Component.LEFT_ALIGNMENT);
        columns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        columns.add(buildScheduleCard());
        columns.add(buildQueueCard());
        return columns;
    }


    private JComponent buildScheduleCard() {
        ElevatedCard card = new ElevatedCard(AppTheme.RADIUS_LG);
        card.setLayout(new BorderLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                card.getBorder(),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)
        ));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        JLabel title = new JLabel("Today's Schedule");
        title.setFont(FontManager.bodyFont(Font.BOLD, 15));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));
        wrapper.add(title);

        scheduleBody = new JPanel();
        scheduleBody.setLayout(new BoxLayout(scheduleBody, BoxLayout.Y_AXIS));
        scheduleBody.setOpaque(false);
        scheduleBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(scheduleBody);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void renderSchedule(List<Appointment> todaysAppointments) {
        scheduleBody.removeAll();

        if (todaysAppointments.isEmpty()) {
            JLabel empty = new JLabel("<html>Nothing on your schedule for today.</html>");
            empty.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            scheduleBody.add(empty);
            scheduleBody.revalidate();
            scheduleBody.repaint();
            return;
        }

        List<Appointment> sorted = todaysAppointments.stream()
                .sorted(Comparator.comparing(a -> a.getAppointmentTime() != null ? a.getAppointmentTime() : java.time.LocalTime.MIN))
                .toList();

        for (Appointment appt : sorted) {
            scheduleBody.add(scheduleRow(appt));
            scheduleBody.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        }

        scheduleBody.revalidate();
        scheduleBody.repaint();
    }

    private JComponent scheduleRow(Appointment appt) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        JLabel timeLabel = new JLabel(appt.getAppointmentTime() != null ? appt.getAppointmentTime().format(TIME_FMT) : "\u2014");
        timeLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        timeLabel.setForeground(AppTheme.PRIMARY);
        timeLabel.setPreferredSize(new Dimension(72, 20));

        JLabel patientLabel = new JLabel(patientName(appt));
        patientLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        patientLabel.setForeground(AppTheme.TEXT_PRIMARY);

        row.add(timeLabel, BorderLayout.WEST);
        row.add(patientLabel, BorderLayout.CENTER);
        row.add(new StatusBadge(appt.getConfirmationStatus()), BorderLayout.EAST);

        JPanel wrapped = new JPanel(new BorderLayout());
        wrapped.setOpaque(false);
        wrapped.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapped.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrapped.add(row, BorderLayout.CENTER);

        JSeparator divider = new JSeparator();
        divider.setForeground(AppTheme.DIVIDER);
        wrapped.add(divider, BorderLayout.SOUTH);
        return wrapped;
    }


    private JComponent buildQueueCard() {
        ElevatedCard card = new ElevatedCard(AppTheme.RADIUS_LG);
        card.setLayout(new BorderLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                card.getBorder(),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)
        ));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        JLabel title = new JLabel("Ticket Queue");
        title.setFont(FontManager.bodyFont(Font.BOLD, 15));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));
        wrapper.add(title);

        queueBody = new JPanel();
        queueBody.setLayout(new BoxLayout(queueBody, BoxLayout.Y_AXIS));
        queueBody.setOpaque(false);
        queueBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(queueBody);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void renderQueue(List<PatientTicket> tickets) {
        queueBody.removeAll();

        List<PatientTicket> active = tickets.stream()
                .filter(t -> "OPEN".equals(t.getCurrentStatus()) || "IN_PROGRESS".equals(t.getCurrentStatus()))
                .sorted(Comparator.comparing(
                        (PatientTicket t) -> t.getTicketCreatedDate() != null ? t.getTicketCreatedDate() : java.time.LocalDateTime.MAX
                ))
                .toList();

        if (active.isEmpty()) {
            JLabel empty = new JLabel("<html>Your ticket queue is clear \u2014 nice work.</html>");
            empty.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            queueBody.add(empty);
            queueBody.revalidate();
            queueBody.repaint();
            return;
        }

        for (PatientTicket ticket : active) {
            queueBody.add(queueRow(ticket));
            queueBody.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        }

        queueBody.revalidate();
        queueBody.repaint();
    }

    private JComponent queueRow(PatientTicket ticket) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        String description = ticket.getTicketDescription() != null && !ticket.getTicketDescription().isBlank()
                ? ticket.getTicketDescription() : "No description";
        String label = "TK-" + String.format("%03d", ticket.getTicketId()) + "  \u2022  " + ticketPatientName(ticket);

        JLabel textLabel = new JLabel(label.length() > 42 ? label.substring(0, 42) + "\u2026" : label);
        textLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        textLabel.setForeground(AppTheme.TEXT_PRIMARY);
        textLabel.setToolTipText(description);

        row.add(textLabel, BorderLayout.CENTER);
        row.add(new StatusBadge(ticket.getCurrentStatus()), BorderLayout.EAST);

        JPanel wrapped = new JPanel(new BorderLayout());
        wrapped.setOpaque(false);
        wrapped.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapped.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrapped.add(row, BorderLayout.CENTER);

        JSeparator divider = new JSeparator();
        divider.setForeground(AppTheme.DIVIDER);
        wrapped.add(divider, BorderLayout.SOUTH);
        return wrapped;
    }

    private String patientName(Appointment appt) {
        if (appt.getPatient() == null || appt.getPatient().getName() == null) return "\u2014";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last : "");
    }

    private String ticketPatientName(PatientTicket ticket) {
        if (ticket.getPatient() == null || ticket.getPatient().getName() == null) return "\u2014";
        String first = ticket.getPatient().getName().getFirstName();
        String last = ticket.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last.charAt(0) + "." : "");
    }


    private void loadData() {
        int doctorId = SessionManager.getInstance().getUserId();

        BaseApiClient.ApiResult<List<Appointment>> apptResult =
                ApiClientProvider.getInstance().appointments().findByDoctor(doctorId);
        List<Appointment> myAppointments = apptResult.isSuccess() ? apptResult.getData() : List.of();

        BaseApiClient.ApiResult<List<PatientTicket>> ticketResult =
                ApiClientProvider.getInstance().patientTickets().getAll();
        List<PatientTicket> allTickets = ticketResult.isSuccess() ? ticketResult.getData() : List.of();
        List<PatientTicket> myTickets = allTickets.stream()
                .filter(t -> t.getAppointment() != null
                        && t.getAppointment().getDoctor() != null
                        && t.getAppointment().getDoctor().getUserId() == doctorId)
                .collect(Collectors.toList());

        int notificationCount = 0;
        var notifResult = ApiClientProvider.getInstance().notifications().findByDoctor(doctorId);
        if (notifResult.isSuccess()) notificationCount = notifResult.getData().size();

        LocalDate today = LocalDate.now();
        List<Appointment> todaysAppointments = myAppointments.stream()
                .filter(a -> today.equals(a.getAppointmentDate()))
                .filter(a -> !"CANCELLED".equals(a.getConfirmationStatus()) && !"REJECTED".equals(a.getConfirmationStatus()))
                .collect(Collectors.toList());

        long openTicketCount = myTickets.stream()
                .filter(t -> "OPEN".equals(t.getCurrentStatus()) || "IN_PROGRESS".equals(t.getCurrentStatus()))
                .count();

        long uniquePatients = myAppointments.stream()
                .filter(a -> a.getPatient() != null)
                .map(a -> a.getPatient().getUserId())
                .distinct()
                .count();

        todaysAppointmentsCard.setValue(String.valueOf(todaysAppointments.size()));
        pendingTicketsCard.setValue(String.valueOf(openTicketCount));
        totalPatientsCard.setValue(String.valueOf(uniquePatients));
        notificationsCard.setValue(String.valueOf(notificationCount));

        renderSchedule(todaysAppointments);
        renderQueue(myTickets);
    }
}