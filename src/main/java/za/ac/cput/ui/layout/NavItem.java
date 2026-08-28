package za.ac.cput.ui.layout;

public class NavItem {
    private final String key;
    private final String icon;
    private final String label;

    public NavItem(String key, String icon, String label) {
        this.key = key;
        this.icon = icon;
        this.label = label;
    }

    public String getKey() { return key; }
    public String getIcon() { return icon; }
    public String getLabel() { return label; }
}