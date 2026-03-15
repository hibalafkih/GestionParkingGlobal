package view.admin;

import database.Database;
import database.ParkingDAO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminDashboard extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG1    = new Color( 8, 15, 26);
    private static final Color BG2    = new Color( 5, 10, 20);
    private static final Color GREEN  = new Color(16,185,129);
    private static final Color GREEN2 = new Color( 5,150,105);
    private static final Color GREEN3 = new Color( 2,100, 72);
    private static final Color GLASS  = new Color(255,255,255, 12);
    private static final Color GLASSB = new Color(255,255,255, 22);
    private static final Color BORDER = new Color(255,255,255, 25);
    private static final Color BORD2  = new Color(255,255,255, 50);
    private static final Color TEXT   = new Color(210,230,250);
    private static final Color TEXT2  = new Color(140,165,185);
    private static final Color BLUE   = new Color(59,130,246);
    private static final Color GOLD   = new Color(245,158, 11);
    private static final Color RED    = new Color(239, 68, 68);
    private static final Color PURPLE = new Color(139, 92,246);
    private static final Color TEAL   = new Color(20,184,166);
    private static final Color ORANGE = new Color(249,115, 22);

    private final String     login;
    private final String     role;
    private final ParkingDAO dao = new ParkingDAO();
    private       String     activePage = "dashboard";
    private       JPanel     sidebar, contentPanel;
    private       CardLayout cards;

    // KPI
    private JLabel kpiLibres, kpiOccupes, kpiRevenu, kpiVehicules, kpiAlertes, kpiAbonnes;
    private JLabel kpiLibresSub, kpiOccupesSub, kpiRevenuSub, kpiVehiculesSub, kpiAlertesSub, kpiAbonnesSub;

    private final List<Double> chartData = new ArrayList<>();
    private       JPanel       chartPanel;
    private JPanel alertesPanel;
    private double occupationPct = 0;

    public AdminDashboard(String login, String role) {
        this.login = login; this.role = role;
        setTitle("Smart Parking — Administration");
        setSize(1280, 780);
        setMinimumSize(new Dimension(1024, 660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { askLogout(); } });
        setBackground(BG1);
        buildUI();
        loadData();
        startTimers();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, BG1, getWidth(), getHeight(), BG2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                paintBlob(g2, -100, -80, 420, GREEN, 0.09f);
                paintBlob(g2, getWidth()-200, getHeight()-180, 500, BLUE, 0.06f);
                paintBlob(g2, getWidth()/2-150, getHeight()/3, 300, PURPLE, 0.04f);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        getContentPane().setBackground(BG1);
        setContentPane(root);
        root.add(buildTopBar(),  BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        cards = new CardLayout();
        contentPanel = new JPanel(cards); contentPanel.setOpaque(false);
        contentPanel.add(buildDashboardPage(), "dashboard");
        for (String[] p : new String[][]{
                {"abonnements"}, {"historique"}, {"revenus"},
                {"incidents"}, {"tarifs"}, {"notifications"}
        }) contentPanel.add(buildSubPlaceholder(p[0]), p[0]);
        root.add(contentPanel, BorderLayout.CENTER);
    }

    // ── TOP BAR ──────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255,255,255,8)); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setPaint(new GradientPaint(0,getHeight()-1,new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),80),getWidth(),getHeight()-1,new Color(BLUE.getRed(),BLUE.getGreen(),BLUE.getBlue(),40)));
                g2.fillRect(0,getHeight()-1,getWidth(),1);
                g2.dispose();
            }
        };
        bar.setOpaque(false); bar.setPreferredSize(new Dimension(0,62));
        bar.setBorder(new EmptyBorder(0,24,0,24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,0,14)); left.setOpaque(false);
        JLabel breadcrumb = new JLabel("") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),40)); g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                g2.setColor(new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),100)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,20,20);
                g2.setFont(new Font("Segoe UI",Font.BOLD,10)); g2.setColor(GREEN);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };


        JPanel center = new JPanel(new GridLayout(2,1,0,0)); center.setOpaque(false);
        JLabel t1 = lbl("Tableau de bord", 16, Font.BOLD, TEXT);
        JLabel t2 = lbl("Smart Parking Administration  ·  " + new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH).format(new Date()), 10, Font.PLAIN, TEXT2);
        center.add(t1); center.add(t2);
        JPanel cp = new JPanel(new FlowLayout(FlowLayout.LEFT,0,11)); cp.setOpaque(false); cp.add(center);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,14)); right.setOpaque(false);

        JLabel clock = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255,255,255,10)); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                g2.setFont(new Font("Courier New",Font.BOLD,13)); g2.setColor(new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),220));
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        clock.setPreferredSize(new Dimension(108,30));
        new Timer(1000, e -> { clock.setText(new SimpleDateFormat("HH:mm:ss").format(new Date())); clock.repaint(); }).start();
        clock.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()));


        JPanel userBox = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.setColor(new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),50)); g2.fillOval(6,5,26,26);
                g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(GREEN);
                String ini=login.substring(0,1).toUpperCase(); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(ini,6+(26-fm.stringWidth(ini))/2,5+(26+fm.getAscent()-fm.getDescent())/2);
                g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(TEXT);
                g2.drawString(login,40,20);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(TEXT2);
                g2.drawString(role,40,33);
                g2.dispose();
            }
        };
        userBox.setOpaque(false); userBox.setPreferredSize(new Dimension(120,38));

        JButton btnOut = new JButton("Déconnexion") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color c=getModel().isRollover()?new Color(239,68,68,60):new Color(239,68,68,20);
                g2.setColor(c); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(new Color(239,68,68,getModel().isRollover()?160:80)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                g2.setFont(new Font("Segoe UI",Font.BOLD,11)); g2.setColor(RED);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btnOut.setContentAreaFilled(false); btnOut.setBorderPainted(false); btnOut.setFocusPainted(false);
        btnOut.setPreferredSize(new Dimension(110,30)); btnOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOut.addActionListener(e -> askLogout());

        right.add(clock); right.add(userBox); right.add(btnOut);

        JPanel lp = new JPanel(new FlowLayout(FlowLayout.LEFT,14,0)); lp.setOpaque(false); lp.add(left); lp.add(cp);
        bar.add(lp, BorderLayout.WEST); bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // =========================================================================
    // SIDEBAR — VERSION AMÉLIORÉE
    // =========================================================================
    private JPanel buildSidebar() {
        sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fond : dégradé vertical BG1 légèrement plus clair
                g2.setPaint(new GradientPaint(0,0,new Color(12,22,38),0,getHeight(),new Color(6,12,22)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Reflet vert en fond (tache douce)
                RadialGradientPaint blob = new RadialGradientPaint(
                        new Point2D.Float(getWidth()/2f, getHeight()*0.25f), 140,
                        new float[]{0f, 1f},
                        new Color[]{new Color(16,185,129,18), new Color(0,0,0,0)}
                );
                g2.setPaint(blob); g2.fillRect(0, 0, getWidth(), getHeight());

                // Trait droit élégant
                g2.setPaint(new GradientPaint(0,0,new Color(16,185,129,50),0,getHeight()/2,new Color(59,130,246,20)));
                g2.fillRect(getWidth()-1, 0, 1, getHeight());

                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setOpaque(false);
        sidebar.setBorder(new EmptyBorder(0, 0, 12, 0));

        // ── Logo ──────────────────────────────────────────────────────────────
        sidebar.add(buildSidebarLogo());
        sidebar.add(sidebarDivider());

        // ── Navigation ────────────────────────────────────────────────────────
        sidebar.add(sidebarSectionLabel("NAVIGATION"));
        sidebar.add(sideItem("Dashboard",      "dashboard",    GREEN,  "🏠"));
        sidebar.add(sideItem("Abonnements",    "abonnements",  PURPLE, "📋"));
        sidebar.add(sideItem("Historique",     "historique",   BLUE,   "📜"));
        sidebar.add(sideItem("Revenus",        "revenus",      GOLD,   "💰"));

        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sidebarDivider());

        // ── Gestion ───────────────────────────────────────────────────────────
        sidebar.add(sidebarSectionLabel("GESTION"));
        sidebar.add(sideItem("Incidents",      "incidents",    RED,    "⚠"));
        sidebar.add(sideItem("Tarifs Spéciaux","tarifs",       TEAL,   "🏷"));
        sidebar.add(sideItem("Notifications",  "notifications",PURPLE, "🔔"));

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(sidebarDivider());

        // ── Barre occupation ──────────────────────────────────────────────────

        // ── Carte utilisateur ─────────────────────────────────────────────────


        return sidebar;
    }

    // ── Logo amélioré ─────────────────────────────────────────────────────────
    private JPanel buildSidebarLogo() {
        JPanel p = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int lx = 14, ly = 16;

                // Icône : carré arrondi avec dégradé vert
                g2.setPaint(new GradientPaint(lx, ly, GREEN, lx+36, ly+36, GREEN2));
                g2.fillRoundRect(lx, ly, 36, 36, 11, 11);

                // Halo vert derrière l'icône
                g2.setColor(new Color(16,185,129,22));
                g2.fillRoundRect(lx-4, ly-4, 44, 44, 15, 15);
                g2.setPaint(new GradientPaint(lx, ly, GREEN, lx+36, ly+36, GREEN2));
                g2.fillRoundRect(lx, ly, 36, 36, 11, 11);

                // Lettre P
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.setColor(new Color(255,255,255,230));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("P", lx+(36-fm.stringWidth("P"))/2, ly+26);

                // Texte "SmartParking"
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.setColor(TEXT);
                g2.drawString("SmartParking", 60, 30);

                // Texte "Administration" avec pastille verte
                g2.setColor(new Color(16,185,129,180));
                g2.fillOval(60, 36, 6, 6);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(16,185,129,160));
                g2.drawString("Administration", 72, 44);

                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(220, 72));
        p.setPreferredSize(new Dimension(220, 72));
        return p;
    }

    // ── Item de navigation amélioré ──────────────────────────────────────────
    private JPanel sideItem(String label, String page, Color accent, String icon) {
        JPanel item = new JPanel(null) {
            boolean hover = false;
            boolean pressed = false;
            {
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e)  { hover=true;  repaint(); }
                    public void mouseExited(MouseEvent e)   { hover=false; pressed=false; repaint(); }
                    public void mousePressed(MouseEvent e)  { pressed=true; repaint(); }
                    public void mouseReleased(MouseEvent e) { pressed=false; repaint(); }
                    public void mouseClicked(MouseEvent e)  { navigate(page); }
                });
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = activePage.equals(page);

                // Fond de l'item
                if (active) {
                    // Fond coloré semi-transparent
                    g2.setPaint(new GradientPaint(8,0, new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),35),
                            getWidth()-8,0, new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),10)));
                    g2.fillRoundRect(8, 2, getWidth()-16, getHeight()-4, 12, 12);
                    // Bord subtil
                    g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),60));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(8, 2, getWidth()-16, getHeight()-4, 12, 12);
                    // Barre gauche épaisse et lumineuse
                    g2.setPaint(new GradientPaint(0,4, accent, 0, getHeight()-4, new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),120)));
                    g2.fillRoundRect(0, 8, 4, getHeight()-16, 4, 4);
                } else if (pressed) {
                    g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),20));
                    g2.fillRoundRect(8, 2, getWidth()-16, getHeight()-4, 12, 12);
                } else if (hover) {
                    g2.setColor(new Color(255,255,255,8));
                    g2.fillRoundRect(8, 2, getWidth()-16, getHeight()-4, 12, 12);
                    g2.setColor(new Color(255,255,255,15));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(8, 2, getWidth()-16, getHeight()-4, 12, 12);
                }

                // Icône colorée à gauche (zone arrondie)
                int ix = 20, iy = getHeight()/2 - 11;
                if (active) {
                    g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),30));
                    g2.fillRoundRect(ix-2, iy, 22, 22, 8, 8);
                }
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, active ? 13 : 12));
                g2.setColor(active ? accent : new Color(accent.getRed(),accent.getGreen(),accent.getBlue(), hover ? 150 : 90));
                g2.drawString(icon, ix+2, iy+15);

                // Label
                int labelX = 50;
                g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
                g2.setColor(active ? accent : hover ? TEXT : TEXT2);
                g2.drawString(label, labelX, (getHeight()+g2.getFontMetrics().getAscent()-g2.getFontMetrics().getDescent())/2);

                // Flèche droite si actif
                if (active) {
                    g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),160));
                    int ax = getWidth()-18, ay = getHeight()/2;
                    int[] px = {ax, ax+5, ax};
                    int[] py = {ay-4, ay, ay+4};
                    g2.fillPolygon(px, py, 3);
                }

                g2.dispose();
            }
        };
        item.setMaximumSize(new Dimension(220, 42));
        item.setPreferredSize(new Dimension(220, 42));
        return item;
    }

    // ── Barre d'occupation améliorée ─────────────────────────────────────────
    private JPanel buildSidebarOccupation() {
        JPanel p = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int mx=12, my=3, mw=getWidth()-24, mh=getHeight()-6;
                double safePct = Math.max(0, Math.min(100, occupationPct));
                Color pctColor = safePct > 80 ? RED : safePct > 50 ? ORANGE : GREEN;

                // Fond card glass avec bord coloré selon occupation
                g2.setColor(new Color(255,255,255,7));
                g2.fillRoundRect(mx, my, mw, mh, 12, 12);
                g2.setColor(new Color(pctColor.getRed(),pctColor.getGreen(),pctColor.getBlue(),40));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(mx, my, mw, mh, 12, 12);

                // Label "OCCUPATION" + pourcentage sur même ligne
                int row1y = my+15;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.setColor(new Color(255,255,255,55));
                g2.drawString("OCCUPATION", mx+10, row1y);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.setColor(pctColor);
                String pctStr = String.format("%.0f%%", safePct);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(pctStr, mx+mw-fm.stringWidth(pctStr)-10, row1y);

                // Barre de progression — piste
                int bx=mx+10, by=my+19, bw=mw-20, bh=5;
                g2.setColor(new Color(255,255,255,10));
                g2.fillRoundRect(bx, by, bw, bh, bh, bh);

                // Portion remplie — minimum 8px pour rester visible même à 1%
                int filled = (safePct <= 0) ? 0 : Math.max(8, (int)(safePct/100.0*bw));
                if (filled > 0) {
                    Color c2 = safePct > 80 ? ORANGE : safePct > 50 ? GOLD : TEAL;
                    g2.setPaint(new GradientPaint(bx, by, pctColor, bx+filled, by, c2));
                    g2.fillRoundRect(bx, by, filled, bh, bh, bh);
                    // Reflet brillant au bout de la barre
                    g2.setColor(new Color(255,255,255,100));
                    g2.fillOval(bx+filled-4, by, 4, bh);
                }

                // Texte bas : "● Libres" uniquement
                int row3y = by+bh+10;
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g2.setColor(new Color(16,185,129,140));
                g2.drawString("● Libres", bx, row3y);

                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(220, 56));
        p.setPreferredSize(new Dimension(220, 56));
        return p;
    }

    // ── Carte utilisateur corrigée ───────────────────────────────────────────
    private JPanel buildSidebarUserCard() {
        JPanel p = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int mx=10, my=3, mw=getWidth()-20, mh=getHeight()-6;

                // Fond glass teinté vert
                g2.setPaint(new GradientPaint(mx,my,new Color(16,185,129,20),mx+mw,my+mh,new Color(5,150,105,8)));
                g2.fillRoundRect(mx, my, mw, mh, 14, 14);
                g2.setColor(new Color(16,185,129,45));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(mx, my, mw, mh, 14, 14);

                // Avatar — taille fixe 34px, centré verticalement
                int av = 34; // diamètre
                int ax = mx+11;
                int ay = my + (mh-av)/2;

                // Halo flou autour de l'avatar
                g2.setColor(new Color(16,185,129,18));
                g2.fillOval(ax-4, ay-4, av+8, av+8);

                // Cercle avatar
                g2.setPaint(new GradientPaint(ax, ay, new Color(16,185,129,90), ax+av, ay+av, new Color(5,150,105,60)));
                g2.fillOval(ax, ay, av, av);
                g2.setColor(new Color(16,185,129,110));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(ax, ay, av, av);

                // Initiale centrée
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                g2.setColor(GREEN);
                String ini = login.substring(0,1).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ini, ax+(av-fm.stringWidth(ini))/2, ay+(av+fm.getAscent()-fm.getDescent())/2);

                // Pastille online en bas-droite de l'avatar
                g2.setColor(GREEN);
                g2.fillOval(ax+av-9, ay+av-9, 10, 10);
                g2.setColor(new Color(8,15,26));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(ax+av-9, ay+av-9, 10, 10);

                // Nom uniquement — centré verticalement
                int tx = ax+av+10;
                int textAreaW = mx+mw-tx-8;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(TEXT);
                fm = g2.getFontMetrics();
                String displayLogin = login;
                while (fm.stringWidth(displayLogin) > textAreaW && displayLogin.length() > 2)
                    displayLogin = displayLogin.substring(0, displayLogin.length()-1);
                if (!displayLogin.equals(login)) displayLogin += "…";
                int nameY = my + (mh + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(displayLogin, tx, nameY);

                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(220, 62));
        p.setPreferredSize(new Dimension(220, 62));
        return p;
    }

    // ── Séparateur amélioré ───────────────────────────────────────────────────
    private JPanel sidebarDivider() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                GradientPaint gp = new GradientPaint(
                        16,0, new Color(255,255,255,0),
                        getWidth()/2f,0, new Color(16,185,129,30));
                GradientPaint gp2 = new GradientPaint(
                        getWidth()/2f,0, new Color(16,185,129,30),
                        getWidth()-16,0, new Color(255,255,255,0));
                g2.setPaint(gp);  g2.fillRect(0,1,getWidth()/2,1);
                g2.setPaint(gp2); g2.fillRect(getWidth()/2,1,getWidth()/2,1);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(220, 10));
        p.setPreferredSize(new Dimension(220, 10));
        return p;
    }

    // ── Section label amélioré ────────────────────────────────────────────────
    private JLabel sidebarSectionLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(new Color(16,185,129,100));
        l.setBorder(new EmptyBorder(8, 18, 3, 0));
        l.setMaximumSize(new Dimension(220, 26));
        return l;
    }

    // ── PAGE DASHBOARD ────────────────────────────────────────────────────────
    private JPanel buildDashboardPage() {
        JPanel page = new JPanel(new BorderLayout(0,14)); page.setOpaque(false); page.setBorder(new EmptyBorder(16,18,16,18));

        JPanel kpiRow = new JPanel(new GridLayout(1,6,10,0)); kpiRow.setOpaque(false); kpiRow.setPreferredSize(new Dimension(0,110));
        kpiLibres    = addKPI(kpiRow,"Places Libres",   "—", GREEN,  "+0 vs hier");  kpiLibresSub    = (JLabel)((JPanel)kpiRow.getComponent(0)).getClientProperty("sub");
        kpiOccupes   = addKPI(kpiRow,"Occupées",        "—", BLUE,   "0%");          kpiOccupesSub   = (JLabel)((JPanel)kpiRow.getComponent(1)).getClientProperty("sub");
        kpiRevenu    = addKPI(kpiRow,"Revenus auj.",    "—", GOLD,   "0 DH");        kpiRevenuSub    = (JLabel)((JPanel)kpiRow.getComponent(2)).getClientProperty("sub");
        kpiVehicules = addKPI(kpiRow,"Passages",        "—", TEAL,   "0 auj.");      kpiVehiculesSub = (JLabel)((JPanel)kpiRow.getComponent(3)).getClientProperty("sub");
        kpiAlertes   = addKPI(kpiRow,"Alertes",         "—", RED,    "à résoudre");  kpiAlertesSub   = (JLabel)((JPanel)kpiRow.getComponent(4)).getClientProperty("sub");
        kpiAbonnes   = addKPI(kpiRow,"Abonnés actifs",  "—", PURPLE, "0 expirent");  kpiAbonnesSub   = (JLabel)((JPanel)kpiRow.getComponent(5)).getClientProperty("sub");
        page.add(kpiRow, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(12,0)); middle.setOpaque(false);
        chartPanel = buildChart(); middle.add(chartPanel, BorderLayout.CENTER);
        alertesPanel = buildAlertesPanel(); alertesPanel.setPreferredSize(new Dimension(260,0));
        middle.add(alertesPanel, BorderLayout.EAST);
        page.add(middle, BorderLayout.CENTER);

        JPanel actRow = new JPanel(new GridLayout(1,7,10,0)); actRow.setOpaque(false); actRow.setPreferredSize(new Dimension(0,68));
        actRow.add(actionBtn("Abonnements", PURPLE, () -> new AbonnementsFrame(this).setVisible(true)));
        actRow.add(actionBtn("Historique",  BLUE,   () -> new HistoriqueFrame(this).setVisible(true)));
        actRow.add(actionBtn("Revenus",     GOLD,   () -> new RevenusFrame(this).setVisible(true)));
        actRow.add(actionBtn("Incidents",   RED,    () -> new IncidentsFrame(this).setVisible(true)));
        actRow.add(actionBtn("Tarifs",      TEAL,   () -> new TarifsFrame(this).setVisible(true)));
        actRow.add(actionBtn("Notifs",      PURPLE, () -> new NotificationsFrame(this).setVisible(true)));
        actRow.add(actionBtn("Carte",       GREEN,  () -> { ParkingMapPanel mp=new ParkingMapPanel(); JDialog d=new JDialog(this,"Carte",false); d.setContentPane(mp); d.setSize(700,500); d.setLocationRelativeTo(this); d.setVisible(true); }));
        page.add(actRow, BorderLayout.SOUTH);
        return page;
    }

    private JLabel addKPI(JPanel row, String titre, String val, Color accent, String sub) {
        JLabel valLbl = new JLabel(val);
        JLabel subLbl = new JLabel(sub);
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                GradientPaint ref=new GradientPaint(0,0,GLASSB,0,getHeight()/2f,new Color(255,255,255,0));
                g2.setPaint(ref); g2.fillRoundRect(0,0,getWidth(),getHeight()/2+8,16,16);
                RadialGradientPaint rg=new RadialGradientPaint(new Point2D.Float(getWidth()-14,14),50,
                        new float[]{0f,1f},new Color[]{new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),55),new Color(0,0,0,0)});
                g2.setPaint(rg); g2.fillOval(getWidth()-64,-36,100,100);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),50));
                g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);
                g2.setPaint(new GradientPaint(0,getHeight()-3,accent,getWidth(),getHeight()-3,new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),60)));
                g2.fillRoundRect(0,getHeight()-3,getWidth(),3,3,3);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),180));
                g2.drawString(titre.toUpperCase(),10,16);
                g2.setFont(new Font("Segoe UI",Font.BOLD,28)); g2.setColor(accent);
                g2.drawString(valLbl.getText(),10,56);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(TEXT2);
                g2.drawString(subLbl.getText(),10,74);
                g2.dispose();
            }
        };
        card.putClientProperty("sub", subLbl);
        valLbl.setVisible(false); subLbl.setVisible(false);
        valLbl.addPropertyChangeListener("text", e -> card.repaint());
        subLbl.addPropertyChangeListener("text", e -> card.repaint());
        card.setOpaque(false);
        row.add(card);
        return valLbl;
    }

    private JPanel buildChart() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                GradientPaint ref=new GradientPaint(0,0,GLASSB,0,getHeight()/3f,new Color(255,255,255,0));
                g2.setPaint(ref); g2.fillRoundRect(0,0,getWidth(),getHeight()/3+8,16,16);
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);
                g2.setFont(new Font("Segoe UI",Font.BOLD,13)); g2.setColor(TEXT); g2.drawString("Revenus — 7 derniers jours",14,24);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(TEXT2); g2.drawString("En dirhams (DH)",14,40);
                if (chartData.isEmpty()) { g2.dispose(); return; }
                int padL=44,padR=20,padT=52,padB=38;
                int cw=getWidth()-padL-padR, ch=getHeight()-padT-padB;
                int n=Math.min(chartData.size(),7);
                List<Double> slice=chartData.subList(chartData.size()-n, chartData.size());
                double maxV=slice.stream().mapToDouble(Double::doubleValue).max().orElse(1);
                if(maxV==0)maxV=1;
                g2.setStroke(new BasicStroke(0.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{4,4},0));
                g2.setColor(new Color(255,255,255,12));
                for(int i=0;i<=4;i++){
                    int y=padT+(int)(ch*(1-(double)i/4));
                    g2.drawLine(padL,y,padL+cw,y);
                    g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(new Color(255,255,255,40));
                    g2.drawString(String.format("%.0f",(maxV/4)*i),2,y+4);
                    g2.setColor(new Color(255,255,255,12));
                }
                double barW=(double)cw/n;
                GeneralPath area=new GeneralPath();
                for(int i=0;i<n;i++){
                    double v=slice.get(i); int x=(int)(padL+i*barW+barW/2); int y=(int)(padT+ch*(1-v/maxV));
                    if(i==0)area.moveTo(x,padT+ch); else area.lineTo(x,padT+ch);
                }
                GeneralPath line=new GeneralPath();
                for(int i=0;i<n;i++){
                    double v=slice.get(i); int x=(int)(padL+i*barW+barW/2); int y=(int)(padT+ch*(1-v/maxV));
                    if(i==0){area.lineTo(x,y);line.moveTo(x,y);}
                    else{area.lineTo(x,y);line.lineTo(x,y);}
                }
                area.closePath();
                GradientPaint fill=new GradientPaint(0,padT,new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),70),0,padT+ch,new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),5));
                g2.setPaint(fill); g2.fill(area);
                g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.setPaint(new GradientPaint(padL,0,GREEN,padL+cw,0,TEAL));
                g2.draw(line);
                String[] days={"J-6","J-5","J-4","J-3","J-2","J-1","Auj."};
                for(int i=0;i<n;i++){
                    double v=slice.get(i); int x=(int)(padL+i*barW+barW/2); int y=(int)(padT+ch*(1-v/maxV));
                    boolean last=(i==n-1);
                    g2.setColor(new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),last?50:25)); g2.fillOval(x-8,y-8,16,16);
                    g2.setColor(last?GREEN:new Color(GREEN.getRed(),GREEN.getGreen(),GREEN.getBlue(),180)); g2.fillOval(x-4,y-4,8,8);
                    if(last||v==maxV){
                        g2.setColor(new Color(20,20,30,160)); g2.fillRoundRect(x-22,y-28,44,18,6,6);
                        g2.setFont(new Font("Segoe UI",Font.BOLD,10)); g2.setColor(last?GREEN:TEXT);
                        FontMetrics fm=g2.getFontMetrics(); String sv=String.format("%.0f",v);
                        g2.drawString(sv,x-fm.stringWidth(sv)/2,y-14);
                    }
                    g2.setFont(new Font("Segoe UI",last?Font.BOLD:Font.PLAIN,9)); g2.setColor(last?GREEN:TEXT2);
                    FontMetrics fm=g2.getFontMetrics(); String dl=days[7-n+i];
                    g2.drawString(dl,x-fm.stringWidth(dl)/2,padT+ch+16);
                }
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private JPanel buildAlertesPanel() {
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                GradientPaint ref=new GradientPaint(0,0,GLASSB,0,getHeight()/4f,new Color(255,255,255,0));
                g2.setPaint(ref); g2.fillRoundRect(0,0,getWidth(),getHeight()/4+8,16,16);
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);
                g2.dispose();
            }
        };
        outer.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.setBorder(new EmptyBorder(12,14,8,14));
        JLabel title = lbl("Alertes actives",13,Font.BOLD,TEXT); header.add(title,BorderLayout.WEST);
        JButton refresh = new JButton("↻") { @Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setColor(GLASS);g2.fillOval(0,0,22,22);g2.setColor(BORDER);g2.drawOval(0,0,21,21);g2.setFont(new Font("Segoe UI",Font.PLAIN,14));g2.setColor(TEXT2);g2.drawString("↻",3,16);g2.dispose();}};
        refresh.setContentAreaFilled(false);refresh.setBorderPainted(false);refresh.setFocusPainted(false);refresh.setPreferredSize(new Dimension(22,22));refresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refresh.addActionListener(e->loadAlertes());
        header.add(refresh,BorderLayout.EAST);
        outer.add(header,BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(getAlertesInner()); scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.getVerticalScrollBar().setUnitIncrement(12);
        outer.add(scroll,BorderLayout.CENTER);
        outer.putClientProperty("scroll",scroll);
        return outer;
    }

    private JPanel getAlertesInner() {
        JPanel inner = new JPanel(); inner.setLayout(new BoxLayout(inner,BoxLayout.Y_AXIS)); inner.setOpaque(false); inner.setBorder(new EmptyBorder(0,10,10,10));
        return inner;
    }

    private void loadAlertes() {
        JScrollPane scroll = (JScrollPane) alertesPanel.getClientProperty("scroll");
        if (scroll==null) return;
        JPanel inner = new JPanel(); inner.setLayout(new BoxLayout(inner,BoxLayout.Y_AXIS)); inner.setOpaque(false); inner.setBorder(new EmptyBorder(0,10,10,10));
        try(Connection c=Database.getConnection(); ResultSet rs=c.createStatement().executeQuery("SELECT type_alerte,message,date_alerte FROM alertes WHERE resolue=0 ORDER BY date_alerte DESC LIMIT 10")) {
            int count=0;
            while(rs.next()) {
                count++;
                String type=rs.getString("type_alerte"); String msg=rs.getString("message"); String date=new SimpleDateFormat("dd/MM HH:mm").format(rs.getTimestamp("date_alerte"));
                Color ac=type!=null&&type.contains("ASSIST")?RED:ORANGE;
                inner.add(makeAlerteCard(type==null?"ALERTE":type, msg==null?"":msg, date, ac));
                inner.add(Box.createVerticalStrut(6));
            }
            if(count==0){ JLabel none=lbl("Aucune alerte active",11,Font.PLAIN,TEXT2); none.setBorder(new EmptyBorder(12,4,0,0)); inner.add(none); }
        } catch(Exception e){ e.printStackTrace(); }
        scroll.setViewportView(inner); scroll.revalidate(); scroll.repaint();
    }

    private JPanel makeAlerteCard(String type, String msg, String date, Color accent) {
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),18)); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),70)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.setColor(accent); g2.fillRoundRect(0,6,3,getHeight()-12,3,3);
                g2.setFont(new Font("Segoe UI",Font.BOLD,10)); g2.setColor(accent); g2.drawString(type,10,16);
                String shortMsg=msg.length()>36?msg.substring(0,36)+"…":msg;
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(TEXT2); g2.drawString(shortMsg,10,30);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(new Color(255,255,255,40)); g2.drawString(date,10,44);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setMaximumSize(new Dimension(10000,52)); card.setPreferredSize(new Dimension(0,52));
        return card;
    }

    private JPanel actionBtn(String label, Color accent, Runnable action) {
        JPanel p = new JPanel(null) {
            boolean hover=false;
            { setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter(){
                    public void mouseEntered(MouseEvent e){hover=true;repaint();}
                    public void mouseExited(MouseEvent e){hover=false;repaint();}
                    public void mouseClicked(MouseEvent e){action.run();}
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover?new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),35):GLASS); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                GradientPaint ref=new GradientPaint(0,0,hover?new Color(255,255,255,18):GLASSB,0,getHeight()/2f,new Color(255,255,255,0));
                g2.setPaint(ref); g2.fillRoundRect(0,0,getWidth(),getHeight()/2+4,12,12);
                g2.setColor(hover?new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),170):new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),60));
                g2.setStroke(new BasicStroke(hover?1.5f:1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),80)); g2.fillOval(10,getHeight()/2-6,12,12);
                g2.setColor(accent); g2.fillOval(13,getHeight()/2-3,6,6);
                g2.setFont(new Font("Segoe UI",Font.BOLD,11)); g2.setColor(hover?accent:TEXT);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(label,28,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        return p;
    }

    private JPanel buildSubPlaceholder(String page) {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); return p;
    }

    private void loadData() {
        SwingWorker<Void,Void> w = new SwingWorker<>() {
            int libres,occupes,passages,alertesN,abonnes; double revenu,pct; List<Double> rev7=new ArrayList<>();
            @Override protected Void doInBackground() {
                try(Connection c=Database.getConnection()) {
                    int[] e=dao.getEtatParking(); libres=e[0]; occupes=e[1]; int total=libres+occupes; pct=total>0?(occupes*100.0/total):0;
                    ResultSet r1=c.createStatement().executeQuery("SELECT COALESCE(SUM(montant_paye),0) FROM historique_paiements WHERE DATE(date_sortie)=CURDATE()");
                    if(r1.next()) revenu=r1.getDouble(1);
                    ResultSet r2=c.createStatement().executeQuery("SELECT COUNT(*) FROM historique_paiements WHERE DATE(date_sortie)=CURDATE()");
                    if(r2.next()) passages=r2.getInt(1);
                    ResultSet r3=c.createStatement().executeQuery("SELECT COUNT(*) FROM alertes WHERE resolue=0");
                    if(r3.next()) alertesN=r3.getInt(1);
                    ResultSet r4=c.createStatement().executeQuery("SELECT COUNT(*) FROM abonnements WHERE date_fin>=NOW()");
                    if(r4.next()) abonnes=r4.getInt(1);
                    ResultSet r5=c.createStatement().executeQuery("SELECT COALESCE(SUM(montant_paye),0) as rev FROM historique_paiements WHERE date_sortie>=DATE_SUB(CURDATE(),INTERVAL 6 DAY) GROUP BY DATE(date_sortie) ORDER BY DATE(date_sortie)");
                    while(r5.next()) rev7.add(r5.getDouble("rev"));
                    if(rev7.isEmpty()){ for(int i=0;i<7;i++) rev7.add(0.0); }
                    while(rev7.size()<7) rev7.add(0,0.0);
                } catch(Exception ex) { ex.printStackTrace(); }
                return null;
            }
            @Override protected void done() {
                kpiLibres.setText(String.valueOf(libres));
                kpiOccupes.setText(String.valueOf(occupes));
                kpiRevenu.setText(String.format("%.0f DH",revenu));
                kpiVehicules.setText(String.valueOf(passages));
                kpiAlertes.setText(String.valueOf(alertesN));
                kpiAbonnes.setText(String.valueOf(abonnes));
                if(kpiOccupesSub!=null) kpiOccupesSub.setText("");
                if(kpiRevenuSub!=null)  kpiRevenuSub.setText(revenu>0?"Aujourd'hui":"Pas de revenus");
                if(kpiAlertesSub!=null) kpiAlertesSub.setText(alertesN>0?"⚠ À résoudre":"Aucune alerte");
                occupationPct=pct;
                chartData.clear(); chartData.addAll(rev7);
                if(chartPanel!=null) chartPanel.repaint();
                if(sidebar!=null) sidebar.repaint();
                loadAlertes();
            }
        };
        w.execute();
    }

    private void startTimers() {
        new Timer(30000, e -> loadData()).start();
    }

    private void navigate(String page) {
        activePage = page;
        if (sidebar!=null) sidebar.repaint();
        cards.show(contentPanel, page);
        if (!"dashboard".equals(page)) openFrame(page);
    }

    private void openFrame(String page) {
        switch(page) {
            case "abonnements":   new AbonnementsFrame(this).setVisible(true);   break;
            case "historique":    new HistoriqueFrame(this).setVisible(true);    break;
            case "revenus":       new RevenusFrame(this).setVisible(true);       break;
            case "incidents":     new IncidentsFrame(this).setVisible(true);     break;
            case "tarifs":        new TarifsFrame(this).setVisible(true);        break;
            case "notifications": new NotificationsFrame(this).setVisible(true); break;
        }
        activePage="dashboard"; cards.show(contentPanel,"dashboard"); if(sidebar!=null)sidebar.repaint();
    }

    private void askLogout() {
        if(JOptionPane.showConfirmDialog(this,"Se déconnecter ?","Déconnexion",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
            dispose(); new LoginFrame().setVisible(true);
        }
    }

    private void paintBlob(Graphics2D g, int x, int y, int r, Color c, float a) {
        RadialGradientPaint rg=new RadialGradientPaint(new Point2D.Float(x+r/2f,y+r/2f),r/2f,
                new float[]{0f,1f},new Color[]{new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(a*255)),new Color(0,0,0,0)});
        g.setPaint(rg); g.fillOval(x,y,r,r);
    }

    private JLabel lbl(String t,int s,int st,Color c){ JLabel l=new JLabel(t); l.setFont(new Font("Segoe UI",st,s)); l.setForeground(c); return l; }

    private JLabel pill(String txt, Color accent, int w, int h) {
        JLabel lbl = new JLabel(txt) {
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),30)); g2.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),100)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,getHeight(),getHeight());
                g2.setFont(new Font("Segoe UI",Font.BOLD,10)); g2.setColor(accent);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        lbl.setPreferredSize(new Dimension(w,h));
        return lbl;
    }
}