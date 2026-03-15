package util;

import javax.swing.*;
import java.awt.*;

/**
 * Palette de couleurs Dark Mode Premium pour Smart Parking.
 * Couleurs inspirées des dashboards industriels modernes.
 */
public class ThemeManager {

    // === DARK THEME ===
    public static final Color DARK_BG         = new Color(13, 17, 23);
    public static final Color DARK_CARD       = new Color(22, 27, 34);
    public static final Color DARK_CARD2      = new Color(30, 37, 46);
    public static final Color DARK_BORDER     = new Color(48, 54, 61);
    public static final Color DARK_TEXT       = new Color(230, 237, 243);
    public static final Color DARK_TEXT_MUTED = new Color(139, 148, 158);

    // === ACCENTS ===
    public static final Color ACCENT_BLUE     = new Color(88, 166, 255);
    public static final Color ACCENT_GREEN    = new Color(63, 185, 80);
    public static final Color ACCENT_RED      = new Color(248, 81, 73);
    public static final Color ACCENT_ORANGE   = new Color(210, 153, 34);
    public static final Color ACCENT_PURPLE   = new Color(188, 140, 255);
    public static final Color ACCENT_CYAN     = new Color(56, 189, 248);

    // === LIGHT THEME ===
    public static final Color LIGHT_BG        = new Color(246, 248, 250);
    public static final Color LIGHT_CARD      = Color.WHITE;
    public static final Color LIGHT_BORDER    = new Color(208, 215, 222);
    public static final Color LIGHT_TEXT      = new Color(31, 35, 40);
    public static final Color LIGHT_TEXT_MUTED= new Color(101, 109, 118);

    private static boolean darkMode = true;

    public static boolean isDarkMode() { return darkMode; }

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
    }

    public static Color bg()       { return darkMode ? DARK_BG        : LIGHT_BG; }
    public static Color card()     { return darkMode ? DARK_CARD       : LIGHT_CARD; }
    public static Color card2()    { return darkMode ? DARK_CARD2      : new Color(248, 249, 250); }
    public static Color border()   { return darkMode ? DARK_BORDER     : LIGHT_BORDER; }
    public static Color text()     { return darkMode ? DARK_TEXT       : LIGHT_TEXT; }
    public static Color textMuted(){ return darkMode ? DARK_TEXT_MUTED : LIGHT_TEXT_MUTED; }

    /** Applique le thème à toute la fenêtre récursivement */
    public static void applyTheme(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel || c instanceof JScrollPane) {
                c.setBackground(card());
                c.setForeground(text());
            }
            if (c instanceof JLabel) {
                c.setForeground(text());
            }
            if (c instanceof Container) {
                applyTheme((Container) c);
            }
        }
    }

    /**
     * Crée un bouton moderne avec effets hover
     */
    public static JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true; repaint();
                    }
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false; repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bgColor = hovered ? bg.brighter() : bg;
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(170, 42));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        return btn;
    }

    /** Crée un panneau carte avec coins arrondis et ombre */
    public static JPanel createCard() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(card());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }
}