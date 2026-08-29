package za.ac.cput.ui.patient.components;

import javax.swing.*;
import java.awt.*;


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