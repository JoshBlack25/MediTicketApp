package za.ac.cput.ui.clinicstaff.nurse;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.clinicstaff.nurse.pages.AppointmentsPage;
import za.ac.cput.ui.clinicstaff.nurse.pages.PaymentsPage;
import za.ac.cput.ui.clinicstaff.nurse.pages.TicketsPage;
import za.ac.cput.ui.layout.NavItem;
import za.ac.cput.ui.layout.Sidebar;
import za.ac.cput.ui.layout.TopHeader;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Shell for the NURSE role, mirroring AdminDashboard: a role-agnostic
 * Sidebar on the left, a TopHeader and a CardLayout page container on the
 * right, with logout clearing both the auth token and the session.
 *
 * Only Appointments, Tickets and Payments exist as real pages in
 * clinicstaff.nurse.pages — Home, Patients, Notifications and Profile are
 * still empty stubs that do not extend JPanel, so they cannot be added to
 * the CardLayout. Those four navigate to a placeholder instead, following
 * the approach AppFrame documents for screens that aren't built yet: the
 * app stays runnable end-to-end rather than only compiling once every page
 * exists. Swap each placeholder(...) for the real page as it lands.
 */
public class NurseDashboard extends JPanel {

    private final AppFrame appFrame;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);

    private static final String PAGE_HOME = "HOME";
    private static final String PAGE_APPOINTMENTS = "APPOINTMENTS";
    private static final String PAGE_PATIENTS = "PATIENTS";
    private static final String PAGE_TICKETS = "TICKETS";
    private static final String PAGE_NOTIFICATIONS = "NOTIFICATIONS";
    private static final String PAGE_PAYMENTS = "PAYMENTS";
    private static final String PAGE_PROFILE = "PROFILE";

    public NurseDashboard(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        List<NavItem> navItems = List.of(
                new NavItem(PAGE_HOME, "\uD83C\uDFE0", "Home"),
                new NavItem(PAGE_APPOINTMENTS, "\uD83D\uDCC5", "Appointments"),
                new NavItem(PAGE_PATIENTS, "\uD83D\uDC65", "Patients"),
                new NavItem(PAGE_TICKETS, "\uD83C\uDFAB", "Tickets"),
                new NavItem(PAGE_NOTIFICATIONS, "\uD83D\uDD14", "Notifications"),
                new NavItem(PAGE_PAYMENTS, "\uD83D\uDCB3", "Payments"),
                new NavItem(PAGE_PROFILE, "\uD83D\uDC64", "Profile")
        );

        Sidebar sidebar = new Sidebar(navItems, PAGE_HOME, this::showPage, this::onLogout);

        registerPages();

        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(AppTheme.BACKGROUND);
        rightSide.add(new TopHeader(), BorderLayout.NORTH);
        rightSide.add(pageContainer, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(rightSide, BorderLayout.CENTER);

        showPage(PAGE_HOME);
    }

    private void registerPages() {
        pageContainer.add(placeholder("Nurse home is not built yet."), PAGE_HOME);
        pageContainer.add(new AppointmentsPage(), PAGE_APPOINTMENTS);
        pageContainer.add(placeholder("Patients is not built yet."), PAGE_PATIENTS);
        pageContainer.add(new TicketsPage(), PAGE_TICKETS);
        pageContainer.add(placeholder("Notifications is not built yet."), PAGE_NOTIFICATIONS);
        pageContainer.add(new PaymentsPage(), PAGE_PAYMENTS);
        pageContainer.add(placeholder("Profile is not built yet."), PAGE_PROFILE);
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
