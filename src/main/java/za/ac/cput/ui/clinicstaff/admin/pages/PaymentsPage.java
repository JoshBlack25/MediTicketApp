package za.ac.cput.ui.clinicstaff.admin.pages;
//ABDULLAHI RAAGE FARAH - 230971091
import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.ui.clinicstaff.components.PaymentDetailsDialog;
import za.ac.cput.ui.clinicstaff.components.SummaryCard;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;


public class PaymentsPage extends JPanel {

    private SummaryCard pendingCard, paidCard, refundedCard, failedCard;
    private JPanel needsAttentionSection;
    private DefaultTableModel tableModel;
    private JTable paymentsTable;

    private List<Payment> allPayments = List.of();
    private String activeFilter = "ALL";
    private JPanel filterBarContainer;

    public PaymentsPage() {
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
        needsAttentionSection = emptySection();
        content.add(needsAttentionSection);
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));

        filterBarContainer = new JPanel(new BorderLayout());
        filterBarContainer.setOpaque(false);
        filterBarContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBarContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
        content.add(filterBarContainer);

        content.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        content.add(buildTable());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Payments");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Monitor patient payments. Click a row to view details.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, AppTheme.SPACE_MD, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        pendingCard = new SummaryCard("Pending", "—", AppTheme.STATUS_WARNING);
        paidCard = new SummaryCard("Paid", "—", AppTheme.STATUS_SUCCESS);
        refundedCard = new SummaryCard("Refunded", "—", AppTheme.STATUS_INFO);
        failedCard = new SummaryCard("Failed", "—", AppTheme.STATUS_DANGER);

        grid.add(pendingCard);
        grid.add(paidCard);
        grid.add(refundedCard);
        grid.add(failedCard);
        return grid;
    }

    private JPanel emptySection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"PENDING", "Pending"}, {"PAID", "Paid"},
                {"REFUNDED", "Refunded"}, {"FAILED", "Failed"}
        };

        for (String[] f : filters) {
            JButton btn = new JButton(f[1]);
            btn.setFont(FontManager.bodyFont(Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBackground(f[0].equals(activeFilter) ? AppTheme.PRIMARY : AppTheme.SURFACE);
            btn.setForeground(f[0].equals(activeFilter) ? AppTheme.TEXT_ON_PRIMARY : AppTheme.TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
            btn.addActionListener(e -> {
                activeFilter = f[0];
                renderTable();
                filterBarContainer.removeAll();
                filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
                filterBarContainer.revalidate();
                filterBarContainer.repaint();
            });
            bar.add(btn);
        }
        return bar;
    }

    private JComponent buildTable() {
        String[] columns = {"Patient", "Doctor", "Appointment Date", "Amount", "Method", "Status", "ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        paymentsTable = new JTable(tableModel);
        paymentsTable.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        paymentsTable.setRowHeight(40);
        paymentsTable.getTableHeader().setFont(FontManager.bodyFont(Font.BOLD, 12));
        paymentsTable.setShowGrid(false);
        paymentsTable.setIntercellSpacing(new Dimension(0, 0));
        paymentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        paymentsTable.getColumnModel().getColumn(6).setMinWidth(0);
        paymentsTable.getColumnModel().getColumn(6).setMaxWidth(0);
        paymentsTable.getColumnModel().getColumn(6).setWidth(0);

        RowClickHelper.makeRowsClickable(paymentsTable, 6, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(paymentsTable);
        scroll.setPreferredSize(new Dimension(0, 380));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int paymentId) {
        Payment payment = findById(paymentId);
        if (payment != null) {
            PaymentDetailsDialog.show(this, payment, this::loadData);
        }
    }


    private void loadData() {
        BaseApiClient.ApiResult<List<Payment>> result = ApiClientProvider.getInstance().payments().getAll();
        allPayments = result.isSuccess() ? result.getData() : List.of();

        updateSummaryCards();
        updateNeedsAttention();
        renderTable();
    }

    private void updateSummaryCards() {
        pendingCard.setValue(String.valueOf(countByStatus("PENDING")));
        paidCard.setValue(String.valueOf(countByStatus("PAID")));
        refundedCard.setValue(String.valueOf(countByStatus("REFUNDED")));
        failedCard.setValue(String.valueOf(countByStatus("FAILED")));
    }

    private long countByStatus(String status) {
        return allPayments.stream().filter(p -> status.equals(p.getPaymentStatus())).count();
    }

    private void updateNeedsAttention() {
        needsAttentionSection.removeAll();

        long pending = countByStatus("PENDING");
        if (pending == 0) {
            needsAttentionSection.revalidate();
            needsAttentionSection.repaint();
            return;
        }

        BigDecimal pendingTotal = allPayments.stream()
                .filter(p -> "PENDING".equals(p.getPaymentStatus()) && p.getPaymentAmount() != null)
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        JLabel title = new JLabel("Needs Attention");
        title.setFont(FontManager.bodyFont(Font.BOLD, 15));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));
        needsAttentionSection.add(title);

        needsAttentionSection.add(attentionBanner(
                "\uD83D\uDCB3 " + pending + " payment" + (pending == 1 ? "" : "s") + " awaiting patient payment",
                "R" + pendingTotal + " total outstanding.", "PENDING"));

        needsAttentionSection.revalidate();
        needsAttentionSection.repaint();
    }

    private JComponent attentionBanner(String title, String subtitle, String filterOnClick) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(AppTheme.SURFACE_ALT);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_MD, AppTheme.SPACE_SM, AppTheme.SPACE_MD)
        ));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontManager.bodyFont(Font.BOLD, 13));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        subtitleLabel.setForeground(AppTheme.TEXT_SECONDARY);

        textStack.add(titleLabel);
        textStack.add(subtitleLabel);

        JButton viewButton = new JButton("View Payments");
        viewButton.setFont(FontManager.bodyFont(Font.BOLD, 12));
        viewButton.setFocusPainted(false);
        viewButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewButton.addActionListener(e -> {
            activeFilter = filterOnClick;
            renderTable();
            filterBarContainer.removeAll();
            filterBarContainer.add(buildFilterBar(), BorderLayout.WEST);
            filterBarContainer.revalidate();
            filterBarContainer.repaint();
        });

        banner.add(textStack, BorderLayout.WEST);
        banner.add(viewButton, BorderLayout.EAST);
        return banner;
    }

    private void renderTable() {
        tableModel.setRowCount(0);

        List<Payment> filtered = "ALL".equals(activeFilter)
                ? allPayments
                : allPayments.stream().filter(p -> activeFilter.equals(p.getPaymentStatus())).collect(Collectors.toList());

        for (Payment payment : filtered) {
            tableModel.addRow(new Object[]{
                    patientName(payment),
                    doctorName(payment),
                    appointmentDate(payment),
                    payment.getPaymentAmount() != null ? "R" + payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP) : "—",
                    payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "—",
                    payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "—",
                    payment.getPaymentId()
            });
        }
    }

    private String patientName(Payment payment) {
        Appointment appt = payment.getAppointment();
        if (appt == null || appt.getPatient() == null || appt.getPatient().getName() == null) return "—";
        String first = appt.getPatient().getName().getFirstName();
        String last = appt.getPatient().getName().getLastName();
        return (first != null ? first : "") + " " + (last != null ? last.charAt(0) + "." : "");
    }

    private String doctorName(Payment payment) {
        Appointment appt = payment.getAppointment();
        if (appt == null || appt.getDoctor() == null || appt.getDoctor().getName() == null) return "—";
        String last = appt.getDoctor().getName().getLastName();
        return "Dr. " + (last != null ? last : "—");
    }

    private String appointmentDate(Payment payment) {
        Appointment appt = payment.getAppointment();
        if (appt == null || appt.getAppointmentDate() == null) return "—";
        return appt.getAppointmentDate().toString();
    }

    private Payment findById(int paymentId) {
        return allPayments.stream().filter(p -> p.getPaymentId() == paymentId).findFirst().orElse(null);
    }
}