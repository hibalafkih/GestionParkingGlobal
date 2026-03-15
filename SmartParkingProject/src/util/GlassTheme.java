package util;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * GlassTheme — alias vers AdminUI pour compatibilité des frames existantes.
 */
public final class GlassTheme {

    public static final Color BG1      = AdminUI.BG;
    public static final Color BG2      = AdminUI.BG2;
    public static final Color SURFACE  = AdminUI.SURFACE;
    public static final Color SURFACE2 = AdminUI.SURFACE2;

    public static final Color BORDER_C = AdminUI.BORDER;
    public static final Color GLASS_C  = AdminUI.alpha(AdminUI.CYAN, 12);
    public static final Color GLASSBR  = AdminUI.alpha(AdminUI.CYAN, 35);

    public static final Color TEXT     = AdminUI.TEXT;
    public static final Color TEXT_SEC = AdminUI.TEXT_SEC;

    public static final Color GREEN    = AdminUI.GREEN;
    public static final Color GREEN2   = new Color(0, 180, 100);
    public static final Color BLUE     = AdminUI.BLUE;
    public static final Color GOLD     = AdminUI.GOLD;
    public static final Color GOLD2    = new Color(220, 160, 30);
    public static final Color RED      = AdminUI.RED;
    public static final Color PURPLE   = AdminUI.PURPLE;
    public static final Color TEAL     = AdminUI.TEAL;
    public static final Color ORANGE   = AdminUI.ORANGE;
    public static final Color CYAN     = AdminUI.CYAN;

    public static Color alpha(Color c, int a) { return AdminUI.alpha(c, a); }

    public static void applyBackground(JDialog d, Color c1, Color c2) {
        d.getContentPane().setBackground(AdminUI.BG);
    }

    public static JPanel buildHeader(String title, String subtitle, Color accent, JLabel extra) {
        return AdminUI.buildHeader(title, subtitle, accent, extra);
    }

    public static JLabel kpiCard(String title, String value, Color accent) {
        return AdminUI.kpiCard(title, value, accent);
    }

    public static JButton createButton(String text, Color bg, int w, int h) {
        return AdminUI.createButton(text, bg, w, h);
    }

    public static JComboBox<String> createCombo(String[] items) {
        return AdminUI.createCombo(items);
    }

    public static void styleTable(JTable t, Color accent) {
        AdminUI.styleTable(t, accent);
    }

    public static JScrollPane scrollTable(JTable t) {
        return AdminUI.scrollTable(t);
    }

    public static JLabel sectionLabel(String text, Color accent) {
        return AdminUI.sectionLabel(text, accent);
    }

    public static JPanel buildFooter() {
        return AdminUI.buildFooter();
    }
}