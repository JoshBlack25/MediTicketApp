package za.ac.cput.ui.patient.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.patient.components.ElevatedCard;
import za.ac.cput.ui.patient.components.StatusBadge;
import za.ac.cput.ui.patient.components.SummaryCard;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DashboardPage extends JPanel {

    private SummaryCard pendingAppointmentsCard;
    private SummaryCard activeTicketsCard;
    private SummaryCard outstandingPaymentsCard;
    private SummaryCard notificationsCard;

    private JPanel timelineBody;
    private JPanel nextAppointmentBody;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
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

        SessionManager session = SessionManager.getInstance();
        String firstName = extractFirstName(session.getFullName());


        JLabel greeting = new JLabel(
                "<html>" + greetingForTime() + ", " + firstName
                        + " <span style='font-family:" + Font.SANS_SERIF + ";'>\uD83D\uDC4B</span></html>"
        );
        greeting.setFont(FontManager.headlineFont(Font.BOLD, 26));
        greeting.setForeground(AppTheme.TEXT_PRIMARY);
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Welcome back to MediTicket. Everything you need to manage your healthcare is right here.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, AppTheme.SPACE_MD, 0));

        panel.add(greeting);
        panel.add(subtitle);
        panel.add(buildQuickActions());
        return panel;
    }

    private JComponent buildQuickActions() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        row.add(quickActionButton("\uD83D\uDCC5", "Appointment", AppTheme.PRIMARY, AppTheme.TEXT_ON_PRIMARY));
        row.add(quickActionButton("\uD83C\uDFAB", "Tickets", AppTheme.SURFACE, AppTheme.TEXT_PRIMARY));
        row.add(quickActionButton("\uD83D\uDCB3", "Payments", AppTheme.ACCENT_DARK, AppTheme.TEXT_ON_PRIMARY));
        return row;
    }

    private JButton quickActionButton(String emoji, String label, Color background, Color foreground) {

        String html = "<html><span style='font-family:" + Font.SANS_SERIF + ";'>" + emoji + "</span> " + label + "</html>";

        JButton button = new JButton(html) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), AppTheme.RADIUS_MD, AppTheme.RADIUS_MD);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(FontManager.bodyFont(Font.BOLD, 13));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setBorderPainted(background == AppTheme.SURFACE); // outline only the white one
        button.setBorder(BorderFactory.createCompoundBorder(
                background == AppTheme.SURFACE ? BorderFactory.createLineBorder(AppTheme.BORDER, 1, true) : null,
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private String greetingForTime() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "there";
        return fullName.split(" ")[0];
    }



    private JComponent buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        pendingAppointmentsCard = new SummaryCard("Pending Appointments", "\u2014", AppTheme.PRIMARY);
        activeTicketsCard = new SummaryCard("Active Tickets", "\u2014", AppTheme.STATUS_INFO);
        outstandingPaymentsCard = new SummaryCard("Outstanding Payments", "\u2014", AppTheme.ACCENT_DARK);
        notificationsCard = new SummaryCard("Notifications", "\u2014", AppTheme.STATUS_DANGER);

        grid.add(pendingAppointmentsCard);
        grid.add(activeTicketsCard);
        grid.add(outstandingPaymentsCard);
        grid.add(notificationsCard);
        return grid;
    }



    private JComponent buildTwoColumnSection() {
        JPanel columns = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_LG, 0));
        columns.setOpaque(false);
        columns.setAlignmentX(Component.LEFT_ALIGNMENT);
        columns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));

        columns.add(buildTimelineCard());
        columns.add(buildNextAppointmentCard());
        return columns;
    }

    // ── Appointment Timeline card ─────────────────────────────────

    private JComponent buildTimelineCard() {
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

        JLabel titleLabel = new JLabel("Appointment Timeline");
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 15));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));
        wrapper.add(titleLabel);

        timelineBody = new JPanel();
        timelineBody.setLayout(new BoxLayout(timelineBody, BoxLayout.Y_AXIS));
        timelineBody.setOpaque(false);
        timelineBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(timelineBody);

        card.add(wrapper, BorderLayout.CENTER);
        return card;
    }

    private void renderTimeline(Appointment appointment) {
        timelineBody.removeAll();

        if (appointment == null) {
            JLabel empty = new JLabel("<html>No upcoming appointments. Book one from the Appointments tab.</html>");
            empty.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            timelineBody.add(empty);
            timelineBody.revalidate();
            timelineBody.repaint();
            return;
        }

        String status = appointment.getConfirmationStatus();
        boolean requested = true; // always true if it exists
        boolean underReview = "PENDING".equals(status);
        boolean confirmed = "CONFIRMED".equals(status) || "COMPLETED".equals(status);
        boolean completed = "COMPLETED".equals(status);

        List<Object[]> steps = List.of(
                new Object[]{"Appointment Requested", "Request received and awaiting triage.", requested},
                new Object[]{"Clinical Review", underReview ? "Medical team reviewing your request." : "Complete.", requested},
                new Object[]{"Doctor Assigned", confirmed ? "You've been matched with a specialist." : "Matching you with the right specialist.", confirmed},
                new Object[]{"Final Confirmation", completed ? "Appointment completed." : "Ready for your arrival.", completed}
        );

        for (int i = 0; i < steps.size(); i++) {
            Object[] step = steps.get(i);
            timelineBody.add(timelineStep((String) step[0], (String) step[1], (boolean) step[2], i == steps.size() - 1));
        }

        timelineBody.revalidate();
        timelineBody.repaint();
    }

    private JComponent timelineStep(String title, String subtitle, boolean done, boolean isLast) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isLast ? 44 : 58));

        JComponent rail = timelineRail(done, isLast);
        row.add(rail, BorderLayout.WEST);

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);
        textStack.setBorder(BorderFactory.createEmptyBorder(0, 0, isLast ? 0 : AppTheme.SPACE_SM, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        titleLabel.setForeground(done ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_MUTED);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);

        textStack.add(titleLabel);
        textStack.add(subtitleLabel);

        row.add(textStack, BorderLayout.CENTER);
        return row;
    }

    /** A dot (filled + checkmark when done, hollow otherwise) with a connecting line down to the next step. */
    private JComponent timelineRail(boolean done, boolean isLast) {
        JPanel rail = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int dotSize = 16;
                int cx = getWidth() / 2;
                int cy = 2;

                if (!isLast) {
                    g2.setColor(done ? AppTheme.PRIMARY_LIGHT : AppTheme.DIVIDER);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(cx, cy + dotSize, cx, getHeight());
                }

                if (done) {
                    g2.setColor(AppTheme.PRIMARY);
                    g2.fillOval(cx - dotSize / 2, cy, dotSize, dotSize);
                    g2.setColor(Color.WHITE);
                    g2.setFont(FontManager.bodyFont(Font.BOLD, 9));
                    FontMetrics fm = g2.getFontMetrics();
                    String check = "\u2713";
                    g2.drawString(check, cx - fm.stringWidth(check) / 2, cy + dotSize / 2 + fm.getAscent() / 2 - 1);
                } else {
                    g2.setColor(AppTheme.SURFACE);
                    g2.fillOval(cx - dotSize / 2, cy, dotSize, dotSize);
                    g2.setColor(AppTheme.BORDER);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(cx - dotSize / 2, cy, dotSize, dotSize);
                }

                g2.dispose();
            }
        };
        rail.setOpaque(false);
        rail.setPreferredSize(new Dimension(24, isLast ? 44 : 58));
        return rail;
    }

    // ── Next Appointment card ──────────────────────────────────────

    private JComponent buildNextAppointmentCard() {
        ElevatedCard card = new ElevatedCard(AppTheme.RADIUS_LG);
        card.setLayout(new BorderLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBackground(AppTheme.PRIMARY);
        card.setDrawBorder(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                card.getBorder(),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)
        ));

        nextAppointmentBody = new JPanel();
        nextAppointmentBody.setLayout(new BoxLayout(nextAppointmentBody, BoxLayout.Y_AXIS));
        nextAppointmentBody.setOpaque(false);
        card.add(nextAppointmentBody, BorderLayout.CENTER);

        return card;
    }

    private void renderNextAppointment(Appointment appointment) {
        nextAppointmentBody.removeAll();

        if (appointment == null) {
            JLabel emptyIcon = new JLabel("\uD83D\uDCC5");
            emptyIcon.setFont(FontManager.bodyFont(Font.PLAIN, 28));
            emptyIcon.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel empty = new JLabel("No upcoming appointments");
            empty.setFont(FontManager.bodyFont(Font.BOLD, 15));
            empty.setForeground(Color.WHITE);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_XS, 0));

            JLabel sub = new JLabel("Book one from the Appointments tab.");
            sub.setFont(FontManager.bodyFont(Font.PLAIN, 13));
            sub.setForeground(AppTheme.PRIMARY_LIGHT);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);

            nextAppointmentBody.add(emptyIcon);
            nextAppointmentBody.add(empty);
            nextAppointmentBody.add(sub);
            nextAppointmentBody.revalidate();
            nextAppointmentBody.repaint();
            return;
        }

        JPanel tagRow = new JPanel(new BorderLayout());
        tagRow.setOpaque(false);
        tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel tag = new JLabel("NEXT APPOINTMENT");
        tag.setFont(FontManager.bodyFont(Font.BOLD, 10));
        tag.setForeground(AppTheme.PRIMARY_LIGHT);
        tagRow.add(tag, BorderLayout.WEST);
        tagRow.add(new StatusBadge(appointment.getConfirmationStatus()), BorderLayout.EAST);

        String doctorText = appointment.getDoctor() != null && appointment.getDoctor().getName() != null
                ? "Dr. " + appointment.getDoctor().getName().getFullName()
                : "Doctor to be assigned";
        String specialty = appointment.getDoctor() != null && appointment.getDoctor().getSpecialty() != null
                ? appointment.getDoctor().getSpecialty()
                : appointment.getReason() != null ? appointment.getReason() : "General Consultation";

        JLabel headline = new JLabel("<html>" + specialty + "</html>");
        headline.setFont(FontManager.headlineFont(Font.BOLD, 21));
        headline.setForeground(Color.WHITE);
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);
        headline.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, 0, AppTheme.SPACE_XS, 0));

        JLabel doctorLabel = new JLabel(doctorText);
        doctorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        doctorLabel.setForeground(AppTheme.PRIMARY_LIGHT);
        doctorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        doctorLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));

        JPanel dateTimeRow = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_SM, 0));
        dateTimeRow.setOpaque(false);
        dateTimeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateTimeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        dateTimeRow.add(dateTimeChip("\uD83D\uDCC5", "DATE", appointment.getAppointmentDate() != null
                ? appointment.getAppointmentDate().format(DATE_FMT) : "\u2014"));
        dateTimeRow.add(dateTimeChip("\uD83D\uDD52", "TIME", appointment.getAppointmentTime() != null
                ? appointment.getAppointmentTime().format(TIME_FMT) : "\u2014"));

        nextAppointmentBody.add(tagRow);
        nextAppointmentBody.add(headline);
        nextAppointmentBody.add(doctorLabel);
        nextAppointmentBody.add(dateTimeRow);

        nextAppointmentBody.revalidate();
        nextAppointmentBody.repaint();
    }

    /** A translucent white "chip" holding a DATE/TIME label + value — reads much cleaner than plain stacked text on a solid color background. */
    private JComponent dateTimeChip(String emoji, String label, String value) {
        JPanel chip = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), AppTheme.RADIUS_SM, AppTheme.RADIUS_SM);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        chip.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_SM, AppTheme.SPACE_SM, AppTheme.SPACE_SM));

        JLabel labelComp = new JLabel(emoji + "  " + label);
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 10));
        labelComp.setForeground(AppTheme.PRIMARY_LIGHT);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(FontManager.bodyFont(Font.BOLD, 14));
        valueComp.setForeground(Color.WHITE);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueComp.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        chip.add(labelComp);
        chip.add(valueComp);
        return chip;
    }



    private void loadData() {
        int patientId = SessionManager.getInstance().getUserId();

        List<Appointment> appointments = List.of();
        BaseApiClient.ApiResult<List<Appointment>> apptResult =
                ApiClientProvider.getInstance().appointments().findByPatient(patientId);
        if (apptResult.isSuccess()) appointments = apptResult.getData();

        int activeTicketCount = 0;
        var ticketResult = ApiClientProvider.getInstance().patientTickets().findByPatientUserId(patientId);
        if (ticketResult.isSuccess()) {
            activeTicketCount = (int) ticketResult.getData().stream()
                    .filter(t -> t.getCurrentStatus() != null
                            && !t.getCurrentStatus().equals("RESOLVED")
                            && !t.getCurrentStatus().equals("CLOSED"))
                    .count();
        }

        int notificationCount = 0;
        var notifResult = ApiClientProvider.getInstance().notifications().findByPatient(patientId);
        if (notifResult.isSuccess()) notificationCount = notifResult.getData().size();


        int outstandingPaymentCount = 0;
        var paymentResult = ApiClientProvider.getInstance().payments().getAll();
        if (paymentResult.isSuccess()) {
            outstandingPaymentCount = (int) paymentResult.getData().stream()
                    .filter(p -> p.getAppointment() != null
                            && p.getAppointment().getPatient() != null
                            && p.getAppointment().getPatient().getUserId() == patientId
                            && "PENDING".equals(p.getPaymentStatus()))
                    .count();
        }

        long pendingCount = appointments.stream()
                .filter(a -> "PENDING".equals(a.getConfirmationStatus()))
                .count();

        pendingAppointmentsCard.setValue(String.valueOf(pendingCount));
        activeTicketsCard.setValue(String.valueOf(activeTicketCount));
        outstandingPaymentsCard.setValue(String.valueOf(outstandingPaymentCount));
        notificationsCard.setValue(String.valueOf(notificationCount));

        Optional<Appointment> nextAppointment = appointments.stream()
                .filter(a -> a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(LocalDate.now()))
                .filter(a -> !"CANCELLED".equals(a.getConfirmationStatus()) && !"REJECTED".equals(a.getConfirmationStatus()))
                .min(Comparator.comparing(Appointment::getAppointmentDate));

        renderTimeline(nextAppointment.orElse(null));
        renderNextAppointment(nextAppointment.orElse(null));
    }
}