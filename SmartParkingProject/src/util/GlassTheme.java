package util;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Thème Luxe "GlassTheme" — Palette cohérente pour tout le projet Admin.
 */
public final class GlassTheme {

    // ── Couleurs de base (Luxe / Prestige) ───────────────────────────────────
    public static final Color BG1      = new Color(16, 16, 18);    // Noir profond
    public static final Color BG2      = new Color(26, 26, 30);    // Anthracite
    public static final Color SURFACE  = new Color(26, 26, 30);
    public static final Color SURFACE2 = new Color(36, 36, 42);

    public static final Color BORDER_C = new Color(212, 175, 55, 80); // Or translucide
    public static final Color GLASS_C  = new Color(212, 175, 55, 15); // Reflet Or
    public static final Color GLASSBR  = new Color(212, 175, 55, 40);

    public static final Color TEXT     = new Color(245, 245, 245); // Argent
    public static final Color TEXT_SEC = new Color(140, 140, 145); // Gris élégant

    public static final Color GREEN    = new Color(46, 139, 87);   // Emeraude
    public static final Color GREEN2   = new Color(34, 110, 65);
    public static final Color BLUE     = new Color(184, 134, 11);  // Bronze
    public static final Color GOLD     = new Color(212, 175, 55);  // OR
    public static final Color GOLD2    = new Color(192, 155, 35);
    public static final Color RED      = new Color(190, 40, 50);   // Carmin
    public static final Color PURPLE   = new Color(128, 0, 32);    // Bordeaux
    public static final Color TEAL     = new Color(212, 175, 55);
    public static final Color ORANGE   = new Color(205, 127, 50);

    public static Color alpha(Color c, int a){ return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    public static void applyBackground(JDialog d, Color c1, Color c2) {
        d.getContentPane().setBackground(BG1);
    }

    public static JPanel buildHeader(String title, String subtitle, Color accent, JLabel extra) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(true);
        h.setBackground(SURFACE);
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C),
                new EmptyBorder(20, 30, 20, 30)
        ));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 5));
        titles.setOpaque(false);
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24)); lblTitle.setForeground(TEXT);
        JLabel lblSub = new JLabel(subtitle); lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblSub.setForeground(TEXT_SEC);
        titles.add(lblTitle); titles.add(lblSub);

        h.add(titles, BorderLayout.WEST);
        if(extra != null){ extra.setForeground(GOLD); h.add(extra, BorderLayout.EAST); }
        return h;
    }

    public static JLabel kpiCard(String title, String value, Color accent) {
        JLabel card = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_C); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.setColor(accent); g2.fillOval(15, 18, 8, 8);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12)); g2.setColor(TEXT_SEC); g2.drawString(title, 32, 26);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22)); g2.setColor(TEXT); g2.drawString(value, 15, 60);
                g2.dispose();
            }
        };
        card.setPreferredSize(new Dimension(150, 80)); return card;
    }

    public static JButton createButton(String text, Color bg, int w, int h) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean p = getModel().isPressed(), r = getModel().isRollover();
                if (bg.equals(TEXT_SEC) || bg.equals(BORDER_C)) {
                    g2.setColor(p ? SURFACE2 : (r ? SURFACE : BG1));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(BORDER_C); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                    g2.setColor(TEXT);
                } else {
                    g2.setColor(p ? bg.darker() : (r ? bg.brighter() : bg));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(BG1); // Texte noir sur or
                    if(bg.equals(RED) || bg.equals(GREEN) || bg.equals(PURPLE)) g2.setColor(TEXT);
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(w, h)); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JComboBox<String> createCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(SURFACE); combo.setForeground(TEXT);
        return combo;
    }

    public static void styleTable(JTable t, Color accent) {
        t.setBackground(SURFACE); t.setForeground(TEXT); t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(35); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(alpha(accent, 40)); t.setSelectionForeground(TEXT);
        JTableHeader h = t.getTableHeader();
        h.setBackground(BG1); h.setForeground(GOLD); h.setFont(new Font("Segoe UI", Font.BOLD, 12));
        h.setPreferredSize(new Dimension(0, 40)); h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C));
        ((DefaultTableCellRenderer)h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
    }

    public static JScrollPane scrollTable(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.getViewport().setBackground(SURFACE); sp.setBorder(BorderFactory.createLineBorder(BORDER_C));
        return sp;
    }

    public static JLabel sectionLabel(String text, Color accent) {
        JLabel l = new JLabel("  " + text); l.setFont(new Font("Segoe UI", Font.BOLD, 14)); l.setForeground(GOLD);
        return l;
    }

    public static JPanel buildFooter() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        f.setOpaque(true); f.setBackground(BG1); f.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_C));
        return f;
    }
}