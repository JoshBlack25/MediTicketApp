package za.ac.cput.ui.clinicstaff.admin.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.auth.EmployeeAccessRequest;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.ClinicStaff;
import za.ac.cput.model.domain.Doctor;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.clinicstaff.components.ActivityRow;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DashboardPage extends JPanel {

    private SummaryCard staffCard;
    private SummaryCard invitesCard;
    private SummaryCard appointmentsCard;
    private SummaryCard notificationsCard;

    private JPanel clinicActivitySection;
    private JPanel staffOverviewSection;
    private JPanel onboardingOverviewSection;

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

        JLabel greeting = new JLabel(greetingForTime() + ", " + firstName + " \uD83D\uDC4B");
        greeting.setFont(FontManager.headlineFont(Font.BOLD, 26));
        greeting.setForeground(AppTheme.TEXT_PRIMARY);
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Welcome back to MediTicket. Here's an overview of today's clinic activity.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(greeting);
        panel.add(subtitle);
        return panel;
    }

    private String greetingForTime() {
        int hour = LocalTime.now().getHour();
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

        staffCard = new SummaryCard("Staff", "—", AppTheme.PRIMARY);
        invitesCard = new SummaryCard("Invites", "—", AppTheme.STATUS_WARNING);
        appointmentsCard = new SummaryCard("Appointments", "—", AppTheme.STATUS_INFO);
        notificationsCard = new SummaryCard("Notifications", "—", AppTheme.STATUS_DANGER);

        grid.add(staffCard);
        grid.add(invitesCard);
        grid.add(appointmentsCard);
        grid.add(notificationsCard);
        return grid;
    }

    private JComponent buildTwoColumnSection() {
        JPanel columns = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_LG, 0));
        columns.setOpaque(false);
        columns.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);

        clinicActivitySection = buildCardSection("Today's Clinic Activity");
        staffOverviewSection = buildCardSection("Staff Overview");
        leftColumn.add(clinicActivitySection);
        leftColumn.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        leftColumn.add(staffOverviewSection);

        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);

        onboardingOverviewSection = buildCardSection("Pending Invitations");
        JPanel recentActivitySection = buildCardSection("Recent Activity");
        rightColumn.add(onboardingOverviewSection);
        rightColumn.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        rightColumn.add(recentActivitySection);

        columns.add(leftColumn);
        columns.add(rightColumn);
        return columns;
    }

    private JPanel buildCardSection(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 15));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        card.add(titleLabel);

        JLabel loading = new JLabel("Loading...");
        loading.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        loading.setForeground(AppTheme.TEXT_MUTED);
        loading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(loading);

        return card;
    }

    private void replaceSectionBody(JPanel section, List<JComponent> rows) {
        while (section.getComponentCount() > 1) {
            section.remove(1);
        }
        for (JComponent row : rows) {
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            section.add(row);
        }
        section.revalidate();
        section.repaint();
    }



    private void loadData() {
        List<Doctor> doctors = List.of();
        List<ClinicStaff> staff = List.of();
        List<Appointment> appointments = List.of();
        List<EmployeeAccessRequest> pendingRequests = List.of();

        BaseApiClient.ApiResult<List<Doctor>> doctorResult = ApiClientProvider.getInstance().doctors().getAll();
        if (doctorResult.isSuccess()) doctors = doctorResult.getData();

        BaseApiClient.ApiResult<List<ClinicStaff>> staffResult = ApiClientProvider.getInstance().clinicStaff().getAll();
        if (staffResult.isSuccess()) staff = staffResult.getData();

        BaseApiClient.ApiResult<List<Appointment>> apptResult = ApiClientProvider.getInstance().appointments().getAll();
        if (apptResult.isSuccess()) appointments = apptResult.getData();

        BaseApiClient.ApiResult<List<EmployeeAccessRequest>> requestResult =
                ApiClientProvider.getInstance().auth().getAccessRequests("PENDING");
        if (requestResult.isSuccess()) pendingRequests = requestResult.getData();

        long nurseCount = staff.stream().filter(s -> "NURSE".equals(s.getStaffRole())).count();
        long adminCount = staff.stream().filter(s -> "ADMIN".equals(s.getStaffRole())).count();
        int totalStaff = doctors.size() + staff.size();

        staffCard.setValue(String.valueOf(totalStaff));
        invitesCard.setValue(String.valueOf(pendingRequests.size()));
        appointmentsCard.setValue(String.valueOf(appointments.size()));
        notificationsCard.setValue(loadNotificationCount());

        renderClinicActivity(appointments);
        renderStaffOverview(doctors.size(), (int) nurseCount, totalStaff);
        renderOnboardingOverview(pendingRequests);

    }

    private String loadNotificationCount() {
        int userId = SessionManager.getInstance().getUserId();
        BaseApiClient.ApiResult<List<za.ac.cput.model.domain.Notification>> result =
                ApiClientProvider.getInstance().notifications().findByClinicStaff(userId);
        return result.isSuccess() ? String.valueOf(result.getData().size()) : "0";
    }

    private void renderClinicActivity(List<Appointment> appointments) {
        LocalDate today = LocalDate.now();

        long scheduled = appointments.stream().filter(a -> today.equals(a.getAppointmentDate())).count();
        long pending = appointments.stream().filter(a -> "PENDING".equals(a.getConfirmationStatus())).count();
        long completed = appointments.stream().filter(a -> "COMPLETED".equals(a.getConfirmationStatus())).count();
        long cancelled = appointments.stream().filter(a -> "CANCELLED".equals(a.getConfirmationStatus())).count();

        replaceSectionBody(clinicActivitySection, List.of(
                new ActivityRow("Scheduled Today", String.valueOf(scheduled), AppTheme.PRIMARY),
                new ActivityRow("Pending", String.valueOf(pending), AppTheme.STATUS_WARNING),
                new ActivityRow("Completed", String.valueOf(completed), AppTheme.STATUS_SUCCESS),
                new ActivityRow("Cancelled", String.valueOf(cancelled), AppTheme.STATUS_DANGER)
        ));
    }

    private void renderStaffOverview(int doctorCount, int nurseCount, int totalStaff) {
        replaceSectionBody(staffOverviewSection, List.of(
                new ActivityRow("Doctors", String.valueOf(doctorCount), null),
                new ActivityRow("Nurses", String.valueOf(nurseCount), null),
                new ActivityRow("Total Staff", String.valueOf(totalStaff), AppTheme.PRIMARY)
        ));
    }

    private void renderOnboardingOverview(List<EmployeeAccessRequest> pendingRequests) {
        long doctorInvites = pendingRequests.stream().filter(r -> "DOCTOR".equals(r.getRequestedUserType())).count();
        long nurseInvites = pendingRequests.stream().filter(r -> "CLINIC_STAFF".equals(r.getRequestedUserType())).count();

        replaceSectionBody(onboardingOverviewSection, List.of(
                new ActivityRow("Doctors", String.valueOf(doctorInvites), null),
                new ActivityRow("Nurses", String.valueOf(nurseInvites), null),
                new ActivityRow("Total Pending", String.valueOf(pendingRequests.size()), AppTheme.STATUS_WARNING)
        ));
    }
}