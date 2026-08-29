package za.ac.cput.ui.doctor.pages;

import za.ac.cput.api.ApiClientProvider;
import za.ac.cput.api.BaseApiClient;
import za.ac.cput.model.domain.Doctor;
import za.ac.cput.model.domain.Name;
import za.ac.cput.session.SessionManager;
import za.ac.cput.ui.auth.components.LabeledTextField;
import za.ac.cput.ui.clinicstaff.components.ChangePasswordDialog;
import za.ac.cput.ui.theme.AppDialog;
import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.AvatarManager;
import za.ac.cput.ui.theme.FontManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;

/**
 * Doctor profile. Email and license number are intentionally locked:
 * email is the unique login identifier tied to verification/invite
 * tokens, and license number is a credential established at signup —
 * neither is self-editable. Specialty is editable, treated the same way
 * ClinicStaff's department is. Avatar is stored locally on disk
 * (AvatarManager), entirely outside the database.
 */
public class ProfilePage extends JPanel {

    private Doctor currentDoctor;
    private JLabel avatarLabel;

    private LabeledTextField firstNameField, middleNameField, lastNameField, phoneField, dobField, specialtyField;

    private JLabel emailValueLabel;
    private JLabel licenseValueLabel;
    private JLabel statusValueLabel;

    private JButton saveButton;

    private final Runnable onProfileUpdated;

    public ProfilePage() {
        this(() -> {});
    }

    public ProfilePage(Runnable onProfileUpdated) {
        this.onProfileUpdated = onProfileUpdated;

        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildAvatarSection());
        content.add(Box.createVerticalStrut(AppTheme.SPACE_LG));
        content.add(buildFormCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadProfile();
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getMaximumSize().height));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("Profile");
        title.setFont(FontManager.headlineFont(Font.BOLD, 26));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Manage your personal information and account security.");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, 0, 0, 0));

        titleBlock.add(title);
        titleBlock.add(subtitle);

        JButton changePasswordButton = new JButton("Change Password");
        changePasswordButton.setFont(FontManager.bodyFont(Font.BOLD, 13));
        changePasswordButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
        changePasswordButton.setBackground(AppTheme.PRIMARY);
        changePasswordButton.setFocusPainted(false);
        changePasswordButton.setBorderPainted(false);
        changePasswordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changePasswordButton.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        changePasswordButton.addActionListener(e -> ChangePasswordDialog.show(this));

        JPanel buttonWrapper = new JPanel(new GridBagLayout());
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(changePasswordButton);

        panel.add(titleBlock, BorderLayout.WEST);
        panel.add(buttonWrapper, BorderLayout.EAST);
        return panel;
    }

    private JComponent buildAvatarSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        avatarLabel = new JLabel();
        avatarLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        avatarLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_SM, 0));

        JButton changePhoto = new JButton("Change Photo");
        changePhoto.setFont(FontManager.bodyFont(Font.BOLD, 12));
        changePhoto.setForeground(AppTheme.PRIMARY);
        changePhoto.setFocusPainted(false);
        changePhoto.setBorderPainted(false);
        changePhoto.setContentAreaFilled(false);
        changePhoto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changePhoto.setAlignmentX(Component.LEFT_ALIGNMENT);
        changePhoto.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        changePhoto.addActionListener(e -> pickAvatar());

        panel.add(avatarLabel);
        panel.add(changePhoto);
        return panel;
    }

    private void pickAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a profile photo");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files", "jpg", "jpeg", "png"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        int userId = SessionManager.getInstance().getUserId();

        boolean saved = AvatarManager.saveAvatar(userId, selected);
        if (saved) {
            avatarLabel.setIcon(AvatarManager.getCircularAvatar(userId, 96));
        } else {
            AppDialog.show(this, "Unable to Save Photo",
                    "That file couldn't be read as an image. Try a different JPG or PNG file.", AppDialog.Type.ERROR);
        }
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG, AppTheme.SPACE_LG)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel sectionTitle = new JLabel("Personal Information");
        sectionTitle.setFont(FontManager.bodyFont(Font.BOLD, 16));
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, AppTheme.SPACE_MD, 0));
        card.add(sectionTitle);

        firstNameField = new LabeledTextField("First Name");
        middleNameField = new LabeledTextField("Middle Name");
        lastNameField = new LabeledTextField("Last Name");
        phoneField = new LabeledTextField("Phone");
        dobField = new LabeledTextField("Date of Birth (yyyy-mm-dd)");
        specialtyField = new LabeledTextField("Specialty");

        card.add(row(firstNameField, middleNameField, lastNameField));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        card.add(row(phoneField, dobField, specialtyField));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        JLabel accountInfoTitle = new JLabel("Account Information");
        accountInfoTitle.setFont(FontManager.bodyFont(Font.BOLD, 13));
        accountInfoTitle.setForeground(AppTheme.TEXT_SECONDARY);
        accountInfoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountInfoTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.DIVIDER),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, 0, AppTheme.SPACE_SM, 0)
        ));
        card.add(accountInfoTitle);

        card.add(row(readOnlyField("Email", true), readOnlyField("License Number", false), readOnlyField("Status", false)));
        card.add(Box.createVerticalStrut(AppTheme.SPACE_MD));

        saveButton = new JButton("Save Changes");
        saveButton.setFont(FontManager.bodyFont(Font.BOLD, 14));
        saveButton.setForeground(AppTheme.TEXT_ON_PRIMARY);
        saveButton.setBackground(AppTheme.PRIMARY);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(220, 44));
        saveButton.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        saveButton.addActionListener(e -> saveChanges());
        card.add(saveButton);

        return card;
    }

    private JPanel row(JComponent... fields) {
        JPanel row = new JPanel(new GridLayout(1, fields.length, AppTheme.SPACE_MD, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        for (JComponent f : fields) row.add(f);
        return row;
    }

    private JComponent readOnlyField(String label, boolean isEmail) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComp = new JLabel(label + " (cannot be changed)");
        labelComp.setFont(FontManager.bodyFont(Font.BOLD, 11));
        labelComp.setForeground(AppTheme.TEXT_MUTED);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueComp = new JLabel("—");
        valueComp.setFont(FontManager.bodyFont(Font.PLAIN, 14));
        valueComp.setForeground(AppTheme.TEXT_SECONDARY);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueComp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        if (isEmail) {
            emailValueLabel = valueComp;
        } else if ("License Number".equals(label)) {
            licenseValueLabel = valueComp;
            valueComp.setFont(FontManager.bodyFont(Font.BOLD, 14));
            valueComp.setForeground(AppTheme.TEXT_PRIMARY);
        } else if ("Status".equals(label)) {
            statusValueLabel = valueComp;
            valueComp.setFont(FontManager.bodyFont(Font.BOLD, 14));
        }

        block.add(labelComp);
        block.add(valueComp);
        return block;
    }

    private void loadProfile() {
        int userId = SessionManager.getInstance().getUserId();
        avatarLabel.setIcon(AvatarManager.getCircularAvatar(userId, 96));

        setFormEnabled(false);

        SwingWorker<BaseApiClient.ApiResult<Doctor>, Void> worker = new SwingWorker<>() {
            @Override
            protected BaseApiClient.ApiResult<Doctor> doInBackground() {
                return ApiClientProvider.getInstance().doctors().read(userId);
            }

            @Override
            protected void done() {
                setFormEnabled(true);

                BaseApiClient.ApiResult<Doctor> result;
                try {
                    result = get();
                } catch (Exception e) {
                    AppDialog.show(ProfilePage.this, "Unable to Load Profile",
                            "Something went wrong while loading your profile.", AppDialog.Type.ERROR);
                    return;
                }

                if (!result.isSuccess() || result.getData() == null) {
                    AppDialog.show(ProfilePage.this, "Unable to Load Profile",
                            "Could not load your profile information.", AppDialog.Type.ERROR);
                    return;
                }

                applyProfile(result.getData());
            }
        };
        worker.execute();
    }

    private void applyProfile(Doctor doctor) {
        currentDoctor = doctor;

        if (currentDoctor.getName() != null) {
            firstNameField.getField().setText(currentDoctor.getName().getFirstName());
            middleNameField.getField().setText(currentDoctor.getName().getMiddleName());
            lastNameField.getField().setText(currentDoctor.getName().getLastName());
        }
        phoneField.getField().setText(currentDoctor.getCellPhone());
        dobField.getField().setText(currentDoctor.getDob() != null ? currentDoctor.getDob().toString() : "");
        specialtyField.getField().setText(currentDoctor.getSpecialty());

        emailValueLabel.setText(currentDoctor.getEmail() != null ? currentDoctor.getEmail() : "—");
        licenseValueLabel.setText(currentDoctor.getLicenseNumber() != null ? currentDoctor.getLicenseNumber() : "—");
        statusValueLabel.setText(currentDoctor.getAccountStatus() != null ? currentDoctor.getAccountStatus() : "—");
        statusValueLabel.setForeground(AppTheme.statusColor(currentDoctor.getAccountStatus()));
    }

    private void setFormEnabled(boolean enabled) {
        for (LabeledTextField field : new LabeledTextField[]{
                firstNameField, middleNameField, lastNameField, phoneField, dobField, specialtyField}) {
            field.getField().setEnabled(enabled);
        }
        saveButton.setEnabled(enabled);
    }

    private void saveChanges() {
        if (currentDoctor == null) return;

        LocalDate dob = null;
        String dobText = dobField.getField().getText().trim();
        if (!dobText.isEmpty()) {
            try {
                dob = LocalDate.parse(dobText);
            } catch (Exception ex) {
                AppDialog.show(this, "Invalid Date",
                        "Please enter the date of birth as yyyy-mm-dd.", AppDialog.Type.ERROR);
                return;
            }
        }

        Name updatedName = new Name();
        updatedName.setFirstName(firstNameField.getField().getText().trim());
        updatedName.setMiddleName(middleNameField.getField().getText().trim());
        updatedName.setLastName(lastNameField.getField().getText().trim());

        currentDoctor.setName(updatedName);
        currentDoctor.setCellPhone(phoneField.getField().getText().trim());
        currentDoctor.setDob(dob);
        currentDoctor.setSpecialty(specialtyField.getField().getText().trim());

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        SwingWorker<BaseApiClient.ApiResult<Doctor>, Void> worker = new SwingWorker<>() {
            @Override
            protected BaseApiClient.ApiResult<Doctor> doInBackground() {
                return ApiClientProvider.getInstance().doctors().update(currentDoctor);
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                saveButton.setText("Save Changes");

                BaseApiClient.ApiResult<Doctor> result;
                try {
                    result = get();
                } catch (Exception e) {
                    AppDialog.show(ProfilePage.this, "Unable to Save",
                            "Something went wrong. Please try again.", AppDialog.Type.ERROR);
                    return;
                }

                if (result.isSuccess()) {
                    AppDialog.show(ProfilePage.this, "Profile Updated", "Your changes have been saved.", AppDialog.Type.SUCCESS);
                    SessionManager.getInstance().setFullName(
                            firstNameField.getField().getText().trim() + " " + lastNameField.getField().getText().trim());
                    loadProfile();
                    onProfileUpdated.run();
                } else {
                    AppDialog.show(ProfilePage.this, "Unable to Save",
                            result.getMessage() != null ? result.getMessage() : "Something went wrong.", AppDialog.Type.ERROR);
                }
            }
        };
        worker.execute();
    }
}