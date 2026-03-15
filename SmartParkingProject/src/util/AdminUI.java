package util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Thème "Luxe / Prestige" (Noir Anthracite & Or)
 * Style haut de gamme, fond profond et accents dorés.
 */
public class AdminUI {

    // --- PALETTE "LUXE / PRESTIGE" ---
    public static final Color BG       = new Color(16, 16, 18);    // Noir profond
    public static final Color SURFACE  = new Color(26, 26, 30);    // Anthracite élégant
    public static final Color SURFACE2 = new Color(36, 36, 42);    // Gris de Payne
    public static final Color SURFACE3 = new Color(46, 46, 52);

    public static final Color TEXT      = new Color(245, 245, 245); // Argent / Blanc
    public static final Color TEXT_SEC  = new Color(140, 140, 145); // Gris doux

    public static final Color BORDER   = new Color(212, 175, 55, 80); // Liseré Or

    // --- COULEURS D'ACCENTUATION (Bijouterie) ---
    public static final Color GOLD      = new Color(212, 175, 55);  // Or Métallique
    public static final Color BLUE      = new Color(184, 134, 11);  // Bronze / Cuivre
    public static final Color GREEN     = new Color(46, 139, 87);   // Vert Émeraude
    public static final Color RED       = new Color(190, 40, 50);   // Rouge Carmin
    public static final Color PURPLE    = new Color(128, 0, 32);    // Bordeaux / Rubis
    public static final Color TEAL      = new Color(212, 175, 55);  // Or (remplace le cyan)
    public static final Color ORANGE    = new Color(205, 127, 50);  // Bronze clair

    // Utilitaire de transparence
    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static void applyBackground(JDialog dialog, Color c1, Color c2) {
        dialog.getContentPane().setBackground(BG);
    }

    // --- COMPOSANTS UI ---

    public static JPanel buildHeader(String title, String subtitle, Color accent, JLabel extra) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(true);
        h.setBackground(SURFACE); // En-tête anthracite
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(20, 30, 20, 30)
        ));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 5));
        titles.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(TEXT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(TEXT_SEC);

        titles.add(lblTitle);
        titles.add(lblSub);

        h.add(titles, BorderLayout.WEST);
        if (extra != null) {
            extra.setForeground(GOLD);
            h.add(extra, BorderLayout.EAST);
        }
        return h;
    }

    public static JLabel kpiCard(String title, String value, Color accent) {
        JLabel card = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fond de la carte
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                // Bordure fine
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);

                // Petit point de couleur en haut à gauche
                g2.setColor(accent);
                g2.fillOval(15, 18, 8, 8);

                // Textes
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(TEXT_SEC);
                g2.drawString(title, 32, 26);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.setColor(TEXT);
                g2.drawString(value, 15, 60);

                g2.dispose();
            }
        };
        card.setPreferredSize(new Dimension(150, 80));
        return card;
    }

    public static JButton createButton(String text, Color bg, int w, int h) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isPressed = getModel().isPressed();
                boolean isRollover = getModel().isRollover();

                // Si c'est un bouton secondaire (Fermer, Annuler)
                if (bg.equals(TEXT_SEC) || bg.equals(BORDER) || bg.equals(BG)) {
                    g2.setColor(isPressed ? SURFACE3 : (isRollover ? SURFACE2 : SURFACE));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(TEXT); // Texte argenté
                } else {
                    // Bouton coloré "Luxe"
                    Color renderBg = isPressed ? bg.darker() : (isRollover ? bg.brighter() : bg);
                    g2.setColor(renderBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(new Color(16, 16, 18)); // Texte foncé pour contraster sur l'or/argent
                    if(bg.equals(RED) || bg.equals(GREEN) || bg.equals(PURPLE)) {
                        g2.setColor(TEXT); // Texte clair sur fond rouge/vert/bordeaux
                    }
                }

                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(w, h));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JComboBox<String> createCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(SURFACE);
        combo.setForeground(TEXT);
        return combo;
    }

    public static void styleTable(JTable t, Color accent) {
        t.setBackground(SURFACE);
        t.setForeground(TEXT);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(35);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(alpha(accent, 50));
        t.setSelectionForeground(TEXT);

        JTableHeader h = t.getTableHeader();
        h.setBackground(BG);
        h.setForeground(GOLD);
        h.setFont(new Font("Segoe UI", Font.BOLD, 12));
        h.setPreferredSize(new Dimension(0, 40));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        ((DefaultTableCellRenderer)h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
    }

    public static JScrollPane scrollTable(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.getViewport().setBackground(SURFACE);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        return sp;
    }

    public static JLabel sectionLabel(String text, Color accent) {
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(GOLD);
        return l;
    }
    public static DefaultTableCellRenderer centeredRenderer(Color color) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.CENTER);
        renderer.setForeground(color);
        return renderer;
    }

    public static JButton outlineButton(String text, Color color, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(color);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(color, 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (al != null) btn.addActionListener(al);
        return btn;
    }

    public static JPanel buildFooter() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        f.setOpaque(true);
        f.setBackground(BG);
        f.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        return f;
    }

    // Pour les AdminDialog
    public static abstract class AdminDialog extends JDialog {
        public AdminDialog(Frame parent, String title, int w, int h) {
            super(parent, title, true);
            setSize(w, h);
            setLocationRelativeTo(parent);
            getContentPane().setBackground(BG);
        }
        protected void init() {
            setLayout(new BorderLayout());
            add(buildHeader(), BorderLayout.NORTH);
            add(buildCenter(), BorderLayout.CENTER);
            add(buildFooter(), BorderLayout.SOUTH);
        }
        protected abstract JPanel buildHeader();
        protected abstract JComponent buildCenter();
        protected abstract JPanel buildFooter();
    }
    // À ajouter dans AdminUI.java
    public static JButton createButton(String text, Color color, java.awt.event.ActionListener al) {
        // On appelle la version existante avec des dimensions par défaut (ex: 160x36)
        JButton btn = createButton(text, color, 160, 36);
        if (al != null) btn.addActionListener(al);
        return btn;
    }
    public static JPanel footerPanel() {
        return buildFooter();
    }

    public static JTabbedPane styledTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.setBackground(SURFACE);
        tabs.setForeground(TEXT_SEC);
        tabs.setOpaque(false);
        UIManager.put("TabbedPane.selected",             SURFACE2);
        UIManager.put("TabbedPane.background",           SURFACE);
        UIManager.put("TabbedPane.foreground",           TEXT_SEC);
        UIManager.put("TabbedPane.selectedForeground",   GOLD);
        UIManager.put("TabbedPane.underlineColor",       GOLD);
        UIManager.put("TabbedPane.contentAreaColor",     SURFACE);
        return tabs;
    }
}