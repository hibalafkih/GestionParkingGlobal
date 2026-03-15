package util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Design System — Glassmorphism Dark
 * Palette : Bleu nuit + accents néon cyan / violet / vert
 */
public class AdminUI {

    // ── Palette ──────────────────────────────────────────────────────────────
    public static final Color BG       = new Color(8,  10, 18);
    public static final Color BG2      = new Color(12, 15, 25);
    public static final Color SURFACE  = new Color(18, 22, 38);
    public static final Color SURFACE2 = new Color(24, 30, 50);
    public static final Color SURFACE3 = new Color(30, 38, 62);

    public static final Color TEXT     = new Color(220, 228, 255);
    public static final Color TEXT_SEC = new Color(110, 125, 165);

    public static final Color BORDER   = new Color(255, 255, 255, 18);
    public static final Color BORDER_A = new Color(100, 140, 255, 45);

    // ── Accents ──────────────────────────────────────────────────────────────
    public static final Color CYAN     = new Color(0,   210, 255);
    public static final Color VIOLET   = new Color(130, 80,  255);
    public static final Color GREEN    = new Color(0,   220, 130);
    public static final Color GOLD     = new Color(255, 190, 50);
    public static final Color RED      = new Color(255, 65,  90);
    public static final Color BLUE     = new Color(60,  140, 255);
    public static final Color PURPLE   = new Color(180, 80,  220);
    public static final Color TEAL     = new Color(0,   200, 180);
    public static final Color ORANGE   = new Color(255, 140, 40);
    public static final Color PINK     = new Color(255, 60,  160);

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static void applyBackground(JDialog d, Color c1, Color c2) {
        d.getContentPane().setBackground(BG);
    }

    // ── Header ───────────────────────────────────────────────────────────────
    public static JPanel buildHeader(String title, String subtitle, Color accent, JLabel extra) {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, SURFACE, getWidth(), 0, BG2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, accent, getWidth() / 2f, 0, alpha(accent, 0)));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        h.setOpaque(false);
        h.setBorder(new EmptyBorder(18, 28, 16, 28));

        JPanel left = new JPanel(null);
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(500, 52));

        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, accent, 0, getHeight(), alpha(accent, 50)));
                g2.fillRoundRect(0, 0, 3, getHeight(), 3, 3);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBounds(0, 4, 3, 42);

        JLabel t1 = new JLabel(title);
        t1.setFont(new Font("Segoe UI", Font.BOLD, 22));
        t1.setForeground(TEXT);
        t1.setBounds(14, 2, 460, 28);

        JLabel t2 = new JLabel(subtitle);
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t2.setForeground(TEXT_SEC);
        t2.setBounds(14, 30, 460, 18);

        left.add(bar); left.add(t1); left.add(t2);
        h.add(left, BorderLayout.WEST);

        if (extra != null) {
            extra.setFont(new Font("Consolas", Font.BOLD, 13));
            extra.setForeground(accent);
            JPanel ep = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
            ep.setOpaque(false);
            ep.add(extra);
            h.add(ep, BorderLayout.EAST);
        }
        return h;
    }

    // ── Bouton principal ─────────────────────────────────────────────────────
    public static JButton createButton(String text, Color color, int w, int h) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean pr = getModel().isPressed(), ro = getModel().isRollover();
                boolean sec = color.equals(TEXT_SEC) || color.equals(BORDER);
                if (sec) {
                    g2.setColor(ro ? SURFACE3 : SURFACE2);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(BORDER_A);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                    g2.setColor(pr ? alpha(TEXT, 160) : TEXT);
                } else {
                    g2.setColor(ro ? alpha(color, 35) : alpha(color, 18));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(ro ? alpha(color, 200) : alpha(color, 120));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                    g2.setColor(ro ? color : alpha(color, 210));
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(w, h));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createButton(String text, Color color, ActionListener al) {
        JButton btn = createButton(text, color, 160, 36);
        if (al != null) btn.addActionListener(al);
        return btn;
    }

    // ── Outline button ────────────────────────────────────────────────────────
    public static JButton outlineButton(String text, Color color, ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean ro = getModel().isRollover(), pr = getModel().isPressed();
                g2.setColor(ro ? alpha(color, 28) : alpha(color, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(ro ? alpha(color, 200) : alpha(color, 100));
                g2.setStroke(new BasicStroke(pr ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(ro ? color : alpha(color, 210));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 34));
        if (al != null) btn.addActionListener(al);
        return btn;
    }

    // ── Combo ─────────────────────────────────────────────────────────────────
    public static JComboBox<String> createCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(SURFACE2);
        combo.setForeground(TEXT);
        combo.setBorder(BorderFactory.createLineBorder(alpha(CYAN, 40)));
        return combo;
    }

    // ── Table ─────────────────────────────────────────────────────────────────
    public static void styleTable(JTable t, Color accent) {
        t.setBackground(SURFACE);
        t.setForeground(TEXT);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(38);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(alpha(accent, 40));
        t.setSelectionForeground(accent);

        DefaultTableCellRenderer rr = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(table, v, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (sel) { setBackground(alpha(accent, 40)); setForeground(accent); }
                else { setBackground(row % 2 == 0 ? SURFACE : SURFACE2); setForeground(TEXT); }
                return this;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(rr);

        JTableHeader hdr = t.getTableHeader();
        hdr.setBackground(BG2);
        hdr.setForeground(accent);
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 11));
        hdr.setPreferredSize(new Dimension(0, 38));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, alpha(accent, 60)));
        DefaultTableCellRenderer hr2 = new DefaultTableCellRenderer();
        hr2.setBackground(BG2); hr2.setForeground(accent);
        hr2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        hr2.setHorizontalAlignment(SwingConstants.LEFT);
        hr2.setBorder(new EmptyBorder(0, 12, 0, 12));
        hdr.setDefaultRenderer(hr2);
    }

    public static JScrollPane scrollTable(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.getViewport().setBackground(SURFACE);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        return sp;
    }

    // ── Section label ─────────────────────────────────────────────────────────
    public static JLabel sectionLabel(String text, Color accent) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(alpha(accent, 180));
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        return l;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    public static JPanel buildFooter() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER);
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
            }
        };
        f.setOpaque(false);
        return f;
    }

    public static JPanel footerPanel() { return buildFooter(); }

    // ── KPI card ──────────────────────────────────────────────────────────────
    public static JLabel kpiCard(String title, String value, Color accent) {
        JLabel card = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(alpha(accent, 40)); g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.setPaint(new GradientPaint(0, 0, accent, getWidth()/2f, 0, alpha(accent, 0)));
                g2.fillRoundRect(0, getHeight()-2, getWidth()/2, 2, 2, 2);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10)); g2.setColor(TEXT_SEC); g2.drawString(title, 14, 22);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 24)); g2.setColor(accent); g2.drawString(value, 14, 58);
                g2.dispose();
            }
        };
        card.setPreferredSize(new Dimension(150, 75));
        return card;
    }

    public static DefaultTableCellRenderer centeredRenderer(Color color) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(JLabel.CENTER); r.setForeground(color);
        return r;
    }

    // ── Tabs ─────────────────────────────────────────────────────────────────
    public static JTabbedPane styledTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.setBackground(SURFACE); tabs.setForeground(TEXT_SEC); tabs.setOpaque(false);
        UIManager.put("TabbedPane.selected",           SURFACE2);
        UIManager.put("TabbedPane.background",         SURFACE);
        UIManager.put("TabbedPane.foreground",         TEXT_SEC);
        UIManager.put("TabbedPane.selectedForeground", CYAN);
        UIManager.put("TabbedPane.underlineColor",     CYAN);
        UIManager.put("TabbedPane.contentAreaColor",   SURFACE);
        return tabs;
    }

    // ── AdminDialog ───────────────────────────────────────────────────────────
    public static abstract class AdminDialog extends JDialog {
        public AdminDialog(Frame parent, String title, int w, int h) {
            super(parent, title, true);
            setSize(w, h);
            setLocationRelativeTo(parent);
            getContentPane().setBackground(BG);
        }
        protected void init() {
            setLayout(new BorderLayout());
            add(buildHeader(),  BorderLayout.NORTH);
            add(buildCenter(),  BorderLayout.CENTER);
            add(buildFooter(),  BorderLayout.SOUTH);
        }
        protected abstract JPanel     buildHeader();
        protected abstract JComponent buildCenter();
        protected abstract JPanel     buildFooter();
    }
}