package za.ac.cput.ui.patient.components;

import javax.swing.*;
import java.awt.*;

/**
 * A non-editable JTextArea styled to look like a JLabel, used wherever body
 * text needs to reflow across multiple lines. JLabel's "<html><div
 * style='width:320px'>" trick hardcodes a pixel width that goes stale the
 * moment a card resizes or a description is unusually long/short — this
 * wraps at whatever width it's actually given.
 */
public class WrappingLabel extends JTextArea {

    public WrappingLabel(String text, Font font, Color color) {
        super(text);
        setFont(font);
        setForeground(color);
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setBorder(null);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}