package za.ac.cput.ui.patient.pages;
//ABDULLAHI RAAGE FARAH - 230971091

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Appointment;
import za.ac.cput.model.domain.Payment;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.patient.components.FakeCheckoutDialog;
import za.ac.cput.ui.patient.components.MedicalAidDialog;
import za.ac.cput.ui.layout.RowClickHelper;
import za.ac.cput.ui.patient.components.PaymentReceiptDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;


public class PaymentsPage extends JPanel {

    private DefaultTableModel tableModel;
    private JTable paymentsTable;

    private List<Payment> myPayments = List.of();
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

        JLabel subtitle = new JLabel("View and settle your outstanding balances. Click a pending payment to pay now.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_SM, 0));
        bar.setOpaque(false);

        String[][] filters = {
                {"ALL", "All"}, {"PENDING", "Pending"}, {"PAID", "Paid"},
                {"FAILED", "Failed"}, {"REFUNDED", "Refunded"}
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
        String[] columns = {"Doctor", "Appointment Date", "Amount", "Method", "Status", "ID"};
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

        paymentsTable.getColumnModel().getColumn(5).setMinWidth(0);
        paymentsTable.getColumnModel().getColumn(5).setMaxWidth(0);
        paymentsTable.getColumnModel().getColumn(5).setWidth(0);

        RowClickHelper.makeRowsClickable(paymentsTable, 5, this::onRowClicked);

        JScrollPane scroll = new JScrollPane(paymentsTable);
        scroll.setPreferredSize(new Dimension(0, 400));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.DIVIDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scroll;
    }

    private void onRowClicked(int paymentId) {
        Payment payment = findById(paymentId);
        if (payment == null) return;

        if ("PENDING".equals(payment.getPaymentStatus())) {
            if ("EFT".equals(payment.getPaymentMethod())) {
                FakeCheckoutDialog.show(this, payment, this::loadData);
            } else if ("MEDICAL_AID".equals(payment.getPaymentMethod())) {
                MedicalAidDialog.show(this, payment);
            } else {
                JOptionPane.showMessageDialog(this,
                        "This payment is set to be settled at the clinic by " +
                                methodDisplayName(payment.getPaymentMethod()) + ".\n" +
                                "Please visit reception to complete payment.",
                        "Settle at Clinic", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if ("FAILED".equals(payment.getPaymentStatus()) && "EFT".equals(payment.getPaymentMethod())) {
            int retry = JOptionPane.showConfirmDialog(this,
                    "This payment attempt failed. Would you like to try again?",
                    "Payment Failed", JOptionPane.YES_NO_OPTION);
            if (retry == JOptionPane.YES_OPTION) {
                FakeCheckoutDialog.show(this, payment, this::loadData);
            }
        } else if ("PAID".equals(payment.getPaymentStatus()) || "REFUNDED".equals(payment.getPaymentStatus())) {
            PaymentReceiptDialog.show(this, payment);
        }
    }

    private String methodDisplayName(String method) {
        if (method == null) return "the clinic";
        return switch (method) {
            case "CASH" -> "cash";
            case "CARD" -> "card";
            case "MEDICAL_AID" -> "medical aid";
            default -> method;
        };
    }



    private void loadData() {
        int patientId = SessionManager.getInstance().getUserId();

        BaseApiClient.ApiResult<List<Payment>> result = ApiClientProvider.getInstance().payments().getAll();
        List<Payment> all = result.isSuccess() ? result.getData() : List.of();

        myPayments = all.stream()
                .filter(p -> p.getAppointment() != null
                        && p.getAppointment().getPatient() != null
                        && p.getAppointment().getPatient().getUserId() == patientId)
                .collect(Collectors.toList());

        renderTable();
    }

    private void renderTable() {
        tableModel.setRowCount(0);

        List<Payment> filtered = "ALL".equals(activeFilter)
                ? myPayments
                : myPayments.stream().filter(p -> activeFilter.equals(p.getPaymentStatus())).collect(Collectors.toList());

        for (Payment payment : filtered) {
            tableModel.addRow(new Object[]{
                    doctorName(payment),
                    appointmentDate(payment),
                    payment.getPaymentAmount() != null ? "R" + payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP) : "—",
                    payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "—",
                    payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "—",
                    payment.getPaymentId()
            });
        }
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
        return myPayments.stream().filter(p -> p.getPaymentId() == paymentId).findFirst().orElse(null);
    }
}

