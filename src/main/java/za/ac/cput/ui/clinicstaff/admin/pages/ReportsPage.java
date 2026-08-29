package za.ac.cput.ui.clinicstaff.admin.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.*;
import za.ac.cput.ui.clinicstaff.components.ActivityRow;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


public class ReportsPage extends JPanel {

    private SummaryCard revenueCard, appointmentsCard, closedTicketsCard, avgFeeCard;
    private JPanel appointmentBreakdown, ticketBreakdown, paymentBreakdown, staffSnapshot;

    public ReportsPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildSummaryCards());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildTwoColumnSection(true));
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildTwoColumnSection(false));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel title = new JLabel("Reports");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Clinic performance at a glance.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        textStack.add(title);
        textStack.add(subtitle);

        JButton refresh = new JButton("\u21BB Refresh");
        refresh.setFont(FontManager.bodyFont(Font.BOLD, 13));
        refresh.setFocusPainted(false);
        refresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refresh.addActionListener(e -> loadData());

        panel.add(textStack, BorderLayout.WEST);
        panel.add(refresh, BorderLayout.EAST);
        return panel;
    }

    private JComponent buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        revenueCard = new SummaryCard("Total Revenue", "—", AppTheme.STATUS_SUCCESS);
        appointmentsCard = new SummaryCard("Total Appointments", "—", AppTheme.PRIMARY);
        closedTicketsCard = new SummaryCard("Tickets Closed", "—", AppTheme.STATUS_INFO);
        avgFeeCard = new SummaryCard("Avg. Consultation Fee", "—", AppTheme.STATUS_WARNING);

        grid.add(revenueCard);
        grid.add(appointmentsCard);
        grid.add(closedTicketsCard);
        grid.add(avgFeeCard);
        return grid;
    }

    private JComponent buildTwoColumnSection(boolean firstPair) {
        JPanel columns = new JPanel(new GridLayout(1, 2, AppTheme.SPACE_LG, 0));
        columns.setOpaque(false);
        columns.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (firstPair) {
            appointmentBreakdown = buildCardSection("Appointment Breakdown");
            ticketBreakdown = buildCardSection("Ticket Breakdown");
            columns.add(appointmentBreakdown);
            columns.add(ticketBreakdown);
        } else {
            paymentBreakdown = buildCardSection("Payment Breakdown");
            staffSnapshot = buildCardSection("Staff Snapshot");
            columns.add(paymentBreakdown);
            columns.add(staffSnapshot);
        }
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

    private void replaceBody(JPanel section, List<JComponent> rows) {
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
        BaseApiClient.ApiResult<List<Appointment>> apptResult = ApiClientProvider.getInstance().appointments().getAll();
        List<Appointment> appointments = apptResult.isSuccess() ? apptResult.getData() : List.of();

        BaseApiClient.ApiResult<List<PatientTicket>> ticketResult = ApiClientProvider.getInstance().patientTickets().getAll();
        List<PatientTicket> tickets = ticketResult.isSuccess() ? ticketResult.getData() : List.of();

        BaseApiClient.ApiResult<List<Payment>> paymentResult = ApiClientProvider.getInstance().payments().getAll();
        List<Payment> payments = paymentResult.isSuccess() ? paymentResult.getData() : List.of();

        BaseApiClient.ApiResult<List<Doctor>> doctorResult = ApiClientProvider.getInstance().doctors().getAll();
        List<Doctor> doctors = doctorResult.isSuccess() ? doctorResult.getData() : List.of();

        BaseApiClient.ApiResult<List<ClinicStaff>> staffResult = ApiClientProvider.getInstance().clinicStaff().getAll();
        List<ClinicStaff> staff = staffResult.isSuccess() ? staffResult.getData() : List.of();

        renderSummaryCards(appointments, tickets, payments);
        renderAppointmentBreakdown(appointments);
        renderTicketBreakdown(tickets);
        renderPaymentBreakdown(payments);
        renderStaffSnapshot(doctors, staff);
    }

    private void renderSummaryCards(List<Appointment> appointments, List<PatientTicket> tickets, List<Payment> payments) {
        BigDecimal totalRevenue = payments.stream()
                .filter(p -> "PAID".equals(p.getPaymentStatus()) && p.getPaymentAmount() != null)
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long closedTickets = tickets.stream().filter(t -> "CLOSED".equals(t.getCurrentStatus())).count();

        List<Payment> paidPayments = payments.stream()
                .filter(p -> "PAID".equals(p.getPaymentStatus()) && p.getPaymentAmount() != null)
                .toList();
        BigDecimal avgFee = paidPayments.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(paidPayments.size()), 2, RoundingMode.HALF_UP);

        revenueCard.setValue("R" + totalRevenue.setScale(2, RoundingMode.HALF_UP));
        appointmentsCard.setValue(String.valueOf(appointments.size()));
        closedTicketsCard.setValue(String.valueOf(closedTickets));
        avgFeeCard.setValue("R" + avgFee);
    }

    private void renderAppointmentBreakdown(List<Appointment> appointments) {
        replaceBody(appointmentBreakdown, List.of(
                new ActivityRow("Pending", String.valueOf(countAppt(appointments, "PENDING")), AppTheme.STATUS_WARNING),
                new ActivityRow("Confirmed", String.valueOf(countAppt(appointments, "CONFIRMED")), AppTheme.PRIMARY),
                new ActivityRow("Completed", String.valueOf(countAppt(appointments, "COMPLETED")), AppTheme.STATUS_SUCCESS),
                new ActivityRow("Cancelled", String.valueOf(countAppt(appointments, "CANCELLED")), AppTheme.STATUS_DANGER),
                new ActivityRow("Rejected", String.valueOf(countAppt(appointments, "REJECTED")), AppTheme.STATUS_DANGER),
                new ActivityRow("Rescheduled", String.valueOf(countAppt(appointments, "RESCHEDULED")), AppTheme.STATUS_INFO)
        ));
    }

    private void renderTicketBreakdown(List<PatientTicket> tickets) {
        replaceBody(ticketBreakdown, List.of(
                new ActivityRow("Open", String.valueOf(countTicket(tickets, "OPEN")), AppTheme.PRIMARY),
                new ActivityRow("In Progress", String.valueOf(countTicket(tickets, "IN_PROGRESS")), AppTheme.STATUS_INFO),
                new ActivityRow("Resolved", String.valueOf(countTicket(tickets, "RESOLVED")), AppTheme.STATUS_WARNING),
                new ActivityRow("Closed", String.valueOf(countTicket(tickets, "CLOSED")), AppTheme.STATUS_SUCCESS),
                new ActivityRow("Escalated", String.valueOf(countTicket(tickets, "ESCALATED")), AppTheme.STATUS_DANGER)
        ));
    }

    private void renderPaymentBreakdown(List<Payment> payments) {
        replaceBody(paymentBreakdown, List.of(
                new ActivityRow("Paid", "R" + sumByStatus(payments, "PAID"), AppTheme.STATUS_SUCCESS),
                new ActivityRow("Pending", "R" + sumByStatus(payments, "PENDING"), AppTheme.STATUS_WARNING),
                new ActivityRow("Refunded", "R" + sumByStatus(payments, "REFUNDED"), AppTheme.STATUS_INFO),
                new ActivityRow("Failed", "R" + sumByStatus(payments, "FAILED"), AppTheme.STATUS_DANGER)
        ));
    }

    private void renderStaffSnapshot(List<Doctor> doctors, List<ClinicStaff> staff) {
        long nurses = staff.stream().filter(s -> "NURSE".equals(s.getStaffRole())).count();
        long admins = staff.stream().filter(s -> "ADMIN".equals(s.getStaffRole())).count();

        replaceBody(staffSnapshot, List.of(
                new ActivityRow("Doctors", String.valueOf(doctors.size()), AppTheme.PRIMARY),
                new ActivityRow("Nurses", String.valueOf(nurses), null),
                new ActivityRow("Admins", String.valueOf(admins), null),
                new ActivityRow("Total Staff", String.valueOf(doctors.size() + staff.size()), AppTheme.STATUS_SUCCESS)
        ));
    }

    private long countAppt(List<Appointment> list, String status) {
        return list.stream().filter(a -> status.equals(a.getConfirmationStatus())).count();
    }

    private long countTicket(List<PatientTicket> list, String status) {
        return list.stream().filter(t -> status.equals(t.getCurrentStatus())).count();
    }

    private BigDecimal sumByStatus(List<Payment> payments, String status) {
        return payments.stream()
                .filter(p -> status.equals(p.getPaymentStatus()) && p.getPaymentAmount() != null)
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}