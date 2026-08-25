package za.ac.cput.ui.layout;

import za.ac.cput.ui.theme.AppTheme;
import za.ac.cput.ui.theme.FontManager;
import za.ac.cput.ui.theme.ImageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Generic role-agnostic sidebar. Callers pass in their own NavItem list
 * (admin, nurse, doctor, patient all differ), so this component contains
 * no role-specific logic — just rendering, selection state, and click
 * dispatch back to whoever constructed it.
 */
public class Sidebar extends JPanel {

    private static final int WIDTH = 240;

    private final Map<String, JPanel> rowsByKey = new LinkedHashMap<>();
    private final Map<String, JLabel> labelsByKey = new LinkedHashMap<>();
    private String selectedKey;
    private final Consumer<String> onNavigate;

    public Sidebar(List<NavItem> items, String initialSelectedKey, Consumer<String> onNavigate, Runnable onLogout) {
        this.onNavigate = onNavigate;
        this.selectedKey = initialSelectedKey;

        setLayout(new BorderLayout());
        setBackground(AppTheme.SURFACE);
        setPreferredSize(new Dimension(WIDTH, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.BORDER));

        add(buildBrandHeader(), BorderLayout.NORTH);
        add(buildNavList(items), BorderLayout.CENTER);
        add(buildLogoutRow(onLogout), BorderLayout.SOUTH);
    }

    private JComponent buildBrandHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(AppTheme.SURFACE);
        header.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_LG, AppTheme.SPACE_MD, AppTheme.SPACE_LG, AppTheme.SPACE_MD));

        JLabel logo = new JLabel(ImageManager.getIcon(ImageManager.LOGO_ICON, -1, 40));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Clinic Management");
        subtitle.setFont(FontManager.bodyFont(Font.PLAIN, 11));
        subtitle.setForeground(AppTheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        header.add(logo);
        header.add(subtitle);
        return header;
    }

    private JComponent buildNavList(List<NavItem> items) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(AppTheme.SURFACE);
        list.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_SM, AppTheme.SPACE_SM, AppTheme.SPACE_SM));

        for (NavItem item : items) {
            JPanel row = buildRow(item.getIcon(), item.getLabel());
            rowsByKey.put(item.getKey(), row);
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectKey(item.getKey());
                    onNavigate.accept(item.getKey());
                }
            });
            list.add(row);
            list.add(Box.createVerticalStrut(2));
        }

        applySelectionStyles();

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildRow(String icon, String label) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.SPACE_SM, 0));
        row.setOpaque(true);
        row.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel iconLabel = new JLabel(icon);
// Icons are emoji glyphs — Inter (our custom embedded font) has no emoji
// coverage and doesn't get OS font-fallback the way Java's built-in
// logical fonts do. Use a plain system font here so emoji render
// correctly instead of as empty boxes.
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        JLabel textLabel = new JLabel(label);
        labelsByKey.put(label, textLabel); // keyed loosely; style pass below uses rowsByKey instead

        row.add(iconLabel, BorderLayout.WEST);
        row.add(textLabel, BorderLayout.CENTER);
        return row;
    }

    private void selectKey(String key) {
        selectedKey = key;
        applySelectionStyles();
    }

    private void applySelectionStyles() {
        for (Map.Entry<String, JPanel> entry : rowsByKey.entrySet()) {
            boolean selected = entry.getKey().equals(selectedKey);
            JPanel row = entry.getValue();
            row.setBackground(selected ? AppTheme.PRIMARY_LIGHT : AppTheme.SURFACE);
            JLabel textLabel = (JLabel) row.getComponent(1);
            textLabel.setFont(FontManager.bodyFont(selected ? Font.BOLD : Font.PLAIN, 13));
            textLabel.setForeground(selected ? AppTheme.PRIMARY : AppTheme.TEXT_PRIMARY);
        }
    }

    private JComponent buildLogoutRow(Runnable onLogout) {
        JPanel row = buildRow("\uD83D\uDEAA", "Logout"); // 🚪
        row.setBackground(AppTheme.SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.DIVIDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { onLogout.run(); }
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.SURFACE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_SM, AppTheme.SPACE_MD, AppTheme.SPACE_SM));
        wrapper.add(row, BorderLayout.CENTER);
        return wrapper;
    }
}