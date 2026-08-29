package za.ac.cput.ui.clinicstaff.nurse.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Patient;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Patient directory / records lookup for the Nurse portal. Loads every
 * patient from the backend and lets staff filter the list locally by
 * name, email, or phone as they type.
 */
public class PatientsPage extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM uuuu");

    private final JTextField searchField;
    private final JPanel listPanel;
    private final JLabel emptyLabel;
    private final JLabel errorLabel;
    private final JLabel countLabel;

    private List<Patient> allPatients = new ArrayList<>();
    private boolean loading = false;

    public PatientsPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_XL, AppTheme.SPACE_LG));

        JLabel title = new JLabel("Patients");
        title.setFont(FontManager.headlineFont(Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField = new JTextField();
        searchField.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, email, or phone...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        countLabel = new JLabel(" ");
        countLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        countLabel.setForeground(AppTheme.TEXT_MUTED);
        countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        countLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        errorLabel.setForeground(AppTheme.STATUS_DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setVisible(false);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, 0, 0));

        emptyLabel = new JLabel("No patients found.");
        emptyLabel.setFont(FontManager.bodyFont(Font.PLAIN, 13));
        emptyLabel.setForeground(AppTheme.TEXT_MUTED);
        emptyLabel.setVisible(false);

        root.add(title);
        root.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        root.add(searchField);
        root.add(countLabel);
        root.add(errorLabel);
        root.add(listPanel);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        if (loading) return;
        loading = true;
        errorLabel.setVisible(false);

        SwingWorker<BaseApiClient.ApiResult<List<Patient>>, Void> worker = new SwingWorker<>() {
            @Override
            protected BaseApiClient.ApiResult<List<Patient>> doInBackground() {
                return ApiClientProvider.getInstance().patients().getAll();
            }

            @Override
            protected void done() {
                loading = false;
                try {
                    BaseApiClient.ApiResult<List<Patient>> result = get();
                    if (result.isSuccess()) {
                        allPatients = result.getData();
                        applyFilter();
                    } else {
                        errorLabel.setText("Couldn't reach the server. Make sure the backend is running.");
                        errorLabel.setVisible(true);
                    }
                } catch (Exception ex) {
                    errorLabel.setText("Couldn't reach the server. Make sure the backend is running.");
                    errorLabel.setVisible(true);
                }
            }
        };
        worker.execute();
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        List<Patient> filtered = new ArrayList<>();
        for (Patient p : allPatients) {
            if (query.isEmpty()) {
                filtered.add(p);
                continue;
            }
            String name = p.getName() != null ? p.getName().getFullName().toLowerCase() : "";
            String email = p.getEmail() != null ? p.getEmail().toLowerCase() : "";
            String phone = p.getCellPhone() != null ? p.getCellPhone().toLowerCase() : "";
            if (name.contains(query) || email.contains(query) || phone.contains(query)) {
                filtered.add(p);
            }
        }

        filtered.sort(Comparator.comparing(p -> p.getName() != null ? p.getName().getFullName() : "",
                String.CASE_INSENSITIVE_ORDER));

        rebuildList(filtered);
    }

    private void rebuildList(List<Patient> patients) {
        listPanel.removeAll();

        for (Patient p : patients) {
            listPanel.add(buildRow(p));
            listPanel.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        }

        countLabel.setText(patients.size() + " patient" + (patients.size() == 1 ? "" : "s"));
        emptyLabel.setVisible(patients.isEmpty());
        if (patients.isEmpty()) listPanel.add(emptyLabel);

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JComponent buildRow(Patient p) {
        JPanel card = new JPanel(new BorderLayout(AppTheme.SPACE_MD, 0));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD, AppTheme.SPACE_MD)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));

        String name = p.getName() != null ? p.getName().getFullName() : "Unnamed patient";

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(FontManager.bodyFont(Font.BOLD, 14));
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel contactLabel = new JLabel((p.getEmail() != null ? p.getEmail() : "No email") +
                "  \u00b7  " + (p.getCellPhone() != null ? p.getCellPhone() : "No phone"));
        contactLabel.setFont(FontManager.bodyFont(Font.PLAIN, 12));
        contactLabel.setForeground(AppTheme.TEXT_SECONDARY);

        String registered = p.getDateRegistered() != null ? p.getDateRegistered().format(DATE_FMT) : "Unknown";
        JLabel metaLabel = new JLabel("Patient ID: " + p.getUserId() + "  \u00b7  Registered " + registered +
                "  \u00b7  Emergency contact: " + (p.getEmergencyContact() != null ? p.getEmergencyContact() : "N/A"));
        metaLabel.setFont(FontManager.bodyFont(Font.PLAIN, 11));
        metaLabel.setForeground(AppTheme.TEXT_MUTED);

        textCol.add(nameLabel);
        textCol.add(Box.createVerticalStrut(4));
        textCol.add(contactLabel);
        textCol.add(Box.createVerticalStrut(4));
        textCol.add(metaLabel);

        JLabel statusLabel = new JLabel(p.getAccountStatus() != null ? p.getAccountStatus() : "ACTIVE");
        statusLabel.setOpaque(true);
        boolean active = p.getAccountStatus() == null || "ACTIVE".equals(p.getAccountStatus());
        statusLabel.setBackground(active ? AppTheme.STATUS_SUCCESS_BG : AppTheme.STATUS_NEUTRAL_BG);
        statusLabel.setForeground(active ? AppTheme.STATUS_SUCCESS : AppTheme.STATUS_NEUTRAL);
        statusLabel.setFont(FontManager.bodyFont(Font.BOLD, 10));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(statusLabel);

        card.add(textCol, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }
}
