package za.ac.cput.ui.doctor;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.AppFrame;
import za.ac.cput.ui.doctor.pages.*;
import za.ac.cput.ui.layout.NavItem;
import za.ac.cput.ui.layout.Sidebar;
import za.ac.cput.ui.layout.TopHeader;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DoctorDashboard extends JPanel {

    private final AppFrame appFrame;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);

    private Sidebar sidebar;
    private TopHeader topHeader;

    private static final String PAGE_HOME = "HOME";
    private static final String PAGE_APPOINTMENTS = "APPOINTMENTS";
    private static final String PAGE_TICKETS = "TICKETS";
    private static final String PAGE_PATIENTS = "PATIENTS";
    private static final String PAGE_NOTIFICATIONS = "NOTIFICATIONS";
    private static final String PAGE_PROFILE = "PROFILE";

    public DoctorDashboard(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        List<NavItem> navItems = List.of(
                new NavItem(PAGE_HOME, "\uD83C\uDFE0", "Dashboard"),
                new NavItem(PAGE_APPOINTMENTS, "\uD83D\uDCC5", "Appointments"),
                new NavItem(PAGE_TICKETS, "\uD83C\uDFAB", "Tickets"),
                new NavItem(PAGE_PATIENTS, "\uD83D\uDC65", "Patients"),
                new NavItem(PAGE_NOTIFICATIONS, "\uD83D\uDD14", "Notifications"),
                new NavItem(PAGE_PROFILE, "\uD83D\uDC64", "Profile")
        );

        sidebar = new Sidebar(navItems, PAGE_HOME, this::showPage, this::onLogout);

        // Header must exist before registerPages(), since ProfilePage needs
        // topHeader::refreshProfile to notify it of name/avatar changes.
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
        pageContainer.add(placeholder("Dashboard — coming soon"), PAGE_HOME);
        pageContainer.add(placeholder("Appointments — coming soon"), PAGE_APPOINTMENTS);
        pageContainer.add(new TicketsPage(), PAGE_TICKETS);
        pageContainer.add(placeholder("Patients — coming soon"), PAGE_PATIENTS);
        pageContainer.add(new NotificationsPage(), PAGE_NOTIFICATIONS);
        pageContainer.add(placeholder("Profile — coming soon"), PAGE_PROFILE);
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