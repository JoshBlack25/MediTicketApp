package za.ac.cput.ui.layout;

import za.ac.cput.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

/**
 * Makes a JTable's rows click-to-open instead of relying on a dedicated
 * Action column. Replaces the ActionCellRenderer/ActionCellEditor pattern
 * used throughout the app — CTAs move into the dialog the row opens,
 * conditionally rendered there based on status, rather than living in a
 * table cell. Attach once per table; the id column still exists in the
 * model (usually hidden or reused for sorting) so the click handler can
 * look up which row's object to load.
 */
public class RowClickHelper {

    public static void makeRowsClickable(JTable table, int idColumn, IntConsumer onRowClicked) {
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                int modelRow = table.convertRowIndexToModel(row);
                Object idValue = table.getModel().getValueAt(modelRow, idColumn);
                if (idValue instanceof Integer id) {
                    onRowClicked.accept(id);
                }
            }
        });

        // Hover highlight — reuses the same tint Sidebar rows already use,
        // so the interaction language matches across the app.
        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            private int lastHoverRow = -1;

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != lastHoverRow) {
                    lastHoverRow = row;
                    table.repaint();
                }
            }
        });

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                Point mousePos = table.getMousePosition();
                boolean hovered = mousePos != null && table.rowAtPoint(mousePos) == row;
                c.setBackground(hovered ? AppTheme.SURFACE_ALT : AppTheme.SURFACE);
                return c;
            }
        });
    }
}