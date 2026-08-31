package za.ac.cput.ui.clinicstaff.admin;
//JOSHUA REID ADAMS - 230317693
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.clinicstaff.admin.pages.*;
import za.ac.cput.ui.layout.NavItem;
import za.ac.cput.ui.layout.Sidebar;
import za.ac.cput.ui.layout.TopHeader;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdminDashboard extends JPanel {

    private final AppFrame appFrame;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);

    private Sidebar sidebar;
    private TopHeader topHeader;

    private static final String PAGE_HOME = "HOME";
    private static final String PAGE_APPOINTMENTS = "APPOINTMENTS";
    private static final String PAGE_STAFF = "STAFF";
    private static final String PAGE_PATIENTS = "PATIENTS";
    private static final String PAGE_ONBOARDING = "ONBOARDING";
    private static final String PAGE_TICKETS = "TICKETS";
    private static final String PAGE_REPORTS = "REPORTS";
    private static final String PAGE_NOTIFICATIONS = "NOTIFICATIONS";
    private static final String PAGE_PAYMENTS = "PAYMENTS";
    private static final String PAGE_PROFILE = "PROFILE";

    public AdminDashboard(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        List<NavItem> navItems = List.of(
                new NavItem(PAGE_HOME, "\uD83C\uDFE0", "Home"),
                new NavItem(PAGE_APPOINTMENTS, "\uD83D\uDCC5", "Appointments"),
                new NavItem(PAGE_STAFF, "\uD83D\uDC65", "Staff"),
                new NavItem(PAGE_PATIENTS, "\uD83D\uDECC", "Patients"),
                new NavItem(PAGE_ONBOARDING, "\uD83D\uDCCB", "Onboarding"),
                new NavItem(PAGE_TICKETS, "\uD83C\uDFAB", "Tickets"),
                new NavItem(PAGE_REPORTS, "\uD83D\uDCCA", "Reports"),
                new NavItem(PAGE_NOTIFICATIONS, "\uD83D\uDD14", "Notifications"),
                new NavItem(PAGE_PAYMENTS, "\uD83D\uDCB3", "Payments"),
                new NavItem(PAGE_PROFILE, "\uD83D\uDC64", "Profile")
        );

        sidebar = new Sidebar(navItems, PAGE_HOME, this::showPage, this::onLogout);


        topHeader = new TopHeader(() -> {
            sidebar.select(PAGE_PROFILE);
            showPage(PAGE_PROFILE);
        });

        registerPages();

        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(AppTheme.BACKGROUND);
        rightSide.add(topHeader, BorderLayout.NORTH);
        rightSide.add(pageContainer, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(rightSide, BorderLayout.CENTER);

        showPage(PAGE_HOME);
    }

    private void registerPages() {
        pageContainer.add(new DashboardPage(), PAGE_HOME);
        pageContainer.add(new AppointmentsPage(), PAGE_APPOINTMENTS);
        pageContainer.add(new StaffPage(), PAGE_STAFF);
        pageContainer.add(new PatientsPage(), PAGE_PATIENTS);
        pageContainer.add(new EmployeeOnboardingPage(), PAGE_ONBOARDING);
        pageContainer.add(new TicketsPage(), PAGE_TICKETS);
        pageContainer.add(new ReportsPage(), PAGE_REPORTS);
        pageContainer.add(new NotificationsPage(), PAGE_NOTIFICATIONS);
        pageContainer.add(new PaymentsPage(), PAGE_PAYMENTS);
        pageContainer.add(new ProfilePage(topHeader::refreshProfile), PAGE_PROFILE);
    }

    private JComponent placeholder(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(AppTheme.BACKGROUND);
        JLabel label = new JLabel(message);
        label.setFont(FontManager.bodyFont(Font.PLAIN, 15));
        label.setForeground(AppTheme.TEXT_SECONDARY);
        panel.add(label);
        return panel;
    }

    private void showPage(String key) {
        pageLayout.show(pageContainer, key);
    }

    private void onLogout() {
        ApiClientProvider.getInstance().getBaseApiClient().clearAuthToken();
        SessionManager.getInstance().clear();
        appFrame.showScreen(AppFrame.SCREEN_LOGIN);
    }
}