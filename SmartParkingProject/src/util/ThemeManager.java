package util;

import javax.swing.*;
import java.awt.*;

/**
 * ThemeManager — redirige vers la palette AdminUI glassmorphism dark.
 */
public class ThemeManager {

    public static final Color DARK_BG          = AdminUI.BG;
    public static final Color DARK_CARD        = AdminUI.SURFACE;
    public static final Color DARK_CARD2       = AdminUI.SURFACE2;
    public static final Color DARK_BORDER      = AdminUI.BORDER_A;
    public static final Color DARK_TEXT        = AdminUI.TEXT;
    public static final Color DARK_TEXT_MUTED  = AdminUI.TEXT_SEC;

    public static final Color ACCENT_BLUE      = AdminUI.BLUE;
    public static final Color ACCENT_GREEN     = AdminUI.GREEN;
    public static final Color ACCENT_RED       = AdminUI.RED;
    public static final Color ACCENT_ORANGE    = AdminUI.ORANGE;
    public static final Color ACCENT_PURPLE    = AdminUI.PURPLE;
    public static final Color ACCENT_CYAN      = AdminUI.CYAN;

    public static final Color LIGHT_BG         = AdminUI.BG;
    public static final Color LIGHT_CARD       = AdminUI.SURFACE;
    public static final Color LIGHT_BORDER     = AdminUI.BORDER_A;
    public static final Color LIGHT_TEXT       = AdminUI.TEXT;
    public static final Color LIGHT_TEXT_MUTED = AdminUI.TEXT_SEC;

    private static final boolean darkMode = true;

    public static boolean isDarkMode() { return darkMode; }
    public static void setDarkMode(boolean dark) {}

    public static Color bg()        { return AdminUI.BG; }
    public static Color card()      { return AdminUI.SURFACE; }
    public static Color card2()     { return AdminUI.SURFACE2; }
    public static Color border()    { return AdminUI.BORDER_A; }
    public static Color text()      { return AdminUI.TEXT; }
    public static Color textMuted() { return AdminUI.TEXT_SEC; }

    public static void applyTheme(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel || c instanceof JScrollPane) {
                c.setBackground(card()); c.setForeground(text());
            }
            if (c instanceof JLabel) c.setForeground(text());
            if (c instanceof Container) applyTheme((Container) c);
        }
    }

    public static JButton createButton(String text, Color bg, Color fg) {
        return AdminUI.createButton(text, bg, 170, 42);
    }

    public static JPanel createCard() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(card()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(border()); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }
}