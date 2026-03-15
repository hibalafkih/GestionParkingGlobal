package view.admin;

import database.Database;
import database.ParkingDAO;
import util.AdminUI;
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
    private static final Color BG     = AdminUI.BG;
    private static final Color BG2    = AdminUI.BG2;
    private static final Color SURF   = AdminUI.SURFACE;
    private static final Color SURF2  = AdminUI.SURFACE2;
    private static final Color BORDER = AdminUI.BORDER;
    private static final Color BORD_A = AdminUI.BORDER_A;
    private static final Color TEXT   = AdminUI.TEXT;
    private static final Color TEXT2  = AdminUI.TEXT_SEC;
    private static final Color CYAN   = AdminUI.CYAN;
    private static final Color VIOLET = AdminUI.VIOLET;
    private static final Color GREEN  = AdminUI.GREEN;
    private static final Color GOLD   = AdminUI.GOLD;
    private static final Color RED    = AdminUI.RED;
    private static final Color BLUE   = AdminUI.BLUE;
    private static final Color PURPLE = AdminUI.PURPLE;
    private static final Color TEAL   = AdminUI.TEAL;
    private static final Color ORANGE = AdminUI.ORANGE;

    private final String login, role;
    private final ParkingDAO dao = new ParkingDAO();
    private String activePage = "dashboard";
    private JPanel sidebar, contentPanel;
    private CardLayout cards;

    private JLabel kpiLibres, kpiOccupes, kpiRevenu, kpiVehicules, kpiAlertes, kpiAbonnes;
    private JLabel kpiLibresSub, kpiOccupesSub, kpiRevenuSub, kpiVehiculesSub, kpiAlertesSub, kpiAbonnesSub;

    private final List<Double> chartData = new ArrayList<>();
    private JPanel chartPanel, alertesPanel;
    private double occupationPct = 0;
    private float animTick = 0f;

    public AdminDashboard(String login, String role) {
        this.login = login; this.role = role;
        setTitle("Smart Parking — Administration");
        setSize(1280, 780);
        setMinimumSize(new Dimension(1024, 660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { askLogout(); } });
        setBackground(BG);
        buildUI(); loadData(); startTimers();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, BG, getWidth(), getHeight(), BG2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                paintBlob(g2, -80, -60, 400, CYAN,   0.07f);
                paintBlob(g2, getWidth() - 200, getHeight() - 160, 460, VIOLET, 0.055f);
                paintBlob(g2, getWidth() / 2 - 150, getHeight() / 3, 320, GREEN, 0.035f);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        getContentPane().setBackground(BG);
        setContentPane(root);
        root.add(buildTopBar(),  BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        cards = new CardLayout();
        contentPanel = new JPanel(cards); contentPanel.setOpaque(false);
        contentPanel.add(buildDashboardPage(), "dashboard");
        for (String[] p : new String[][]{{"abonnements"},{"historique"},{"revenus"},{"incidents"},{"tarifs"},{"notifications"}})
            contentPanel.add(buildSubPlaceholder(p[0]), p[0]);
        root.add(contentPanel, BorderLayout.CENTER);
    }

    // ── TOP BAR ──────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AdminUI.alpha(SURF, 180));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, getHeight()-1, AdminUI.alpha(CYAN, 70), getWidth(), getHeight()-1, AdminUI.alpha(VIOLET, 40)));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false); bar.setPreferredSize(new Dimension(0, 60));
        bar.setBorder(new EmptyBorder(0, 22, 0, 22));

        // Breadcrumb
        JLabel bread = new JLabel("TABLEAU DE BORD") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.alpha(CYAN, 18)); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.setColor(AdminUI.alpha(CYAN, 70)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);
                g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(CYAN);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        bread.setPreferredSize(new Dimension(130,22));

        JPanel center = new JPanel(new GridLayout(2,1,0,2)); center.setOpaque(false);
        center.add(lbl("Tableau de bord", 15, Font.BOLD, TEXT));
        center.add(lbl("Smart Parking Administration  ·  " + new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH).format(new Date()), 10, Font.PLAIN, TEXT2));
        JPanel cp = new JPanel(new FlowLayout(FlowLayout.LEFT,0,12)); cp.setOpaque(false); cp.add(center);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,12,0)); left.setOpaque(false);
        left.add(bread); left.add(cp);

        // Horloge
        JLabel clock = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.alpha(SURF2,200)); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(AdminUI.alpha(CYAN,60)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                g2.setFont(new Font("Courier New",Font.BOLD,13)); g2.setColor(CYAN);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        clock.setPreferredSize(new Dimension(108,28));
        new Timer(1000, e -> { clock.setText(new SimpleDateFormat("HH:mm:ss").format(new Date())); clock.repaint(); }).start();
        clock.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()));

        // User box
        JPanel userBox = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.alpha(SURF2,200)); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(AdminUI.alpha(CYAN,40)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.setColor(AdminUI.alpha(CYAN,40)); g2.fillOval(6,5,26,26);
                g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(CYAN);
                String ini=login.substring(0,1).toUpperCase(); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(ini,6+(26-fm.stringWidth(ini))/2,5+(26+fm.getAscent()-fm.getDescent())/2);
                g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(TEXT); g2.drawString(login,40,20);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(TEXT2); g2.drawString(role,40,32);
                g2.dispose();
            }
        };
        userBox.setOpaque(false); userBox.setPreferredSize(new Dimension(120,36));

        JButton btnOut = AdminUI.createButton("Déconnexion", RED, 110, 28);
        btnOut.addActionListener(e -> askLogout());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,14)); right.setOpaque(false);
        right.add(clock); right.add(userBox); right.add(btnOut);

        bar.add(left, BorderLayout.WEST); bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── SIDEBAR ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,AdminUI.alpha(SURF,220),0,getHeight(),AdminUI.alpha(BG2,230)));
                g2.fillRect(0,0,getWidth(),getHeight());
                // Reflet bleu
                RadialGradientPaint blob = new RadialGradientPaint(new Point2D.Float(getWidth()/2f,getHeight()*0.2f),130,
                        new float[]{0f,1f},new Color[]{AdminUI.alpha(CYAN,14),new Color(0,0,0,0)});
                g2.setPaint(blob); g2.fillRect(0,0,getWidth(),getHeight());
                // Séparateur droit
                g2.setPaint(new GradientPaint(0,0,AdminUI.alpha(CYAN,45),0,getHeight()/2,AdminUI.alpha(VIOLET,20)));
                g2.fillRect(getWidth()-1,0,1,getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(218,0));
        sidebar.setOpaque(false);
        sidebar.setBorder(new EmptyBorder(0,0,10,0));

        sidebar.add(buildSidebarLogo());
        sidebar.add(sidebarDivider());
        sidebar.add(sidebarSectionLabel("NAVIGATION"));
        sidebar.add(sideItem("Dashboard",       "dashboard",    CYAN,   "●"));
        sidebar.add(sideItem("Abonnements",     "abonnements",  VIOLET, "●"));
        sidebar.add(sideItem("Historique",      "historique",   BLUE,   "●"));
        sidebar.add(sideItem("Revenus",         "revenus",      GOLD,   "●"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sidebarDivider());
        sidebar.add(sidebarSectionLabel("GESTION"));
        sidebar.add(sideItem("Incidents",       "incidents",    RED,    "●"));
        sidebar.add(sideItem("Tarifs Spéciaux", "tarifs",       TEAL,   "●"));
        sidebar.add(sideItem("Notifications",   "notifications",PURPLE, "●"));
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(sidebarDivider());
        sidebar.add(buildSidebarOccupation());
        sidebar.add(buildSidebarUserCard());
        return sidebar;
    }

    private JPanel buildSidebarLogo() {
        JPanel p = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int lx=14,ly=16;
                g2.setColor(AdminUI.alpha(CYAN,18)); g2.fillRoundRect(lx-4,ly-4,44,44,14,14);
                g2.setColor(AdminUI.alpha(CYAN,22)); g2.fillRoundRect(lx,ly,36,36,11,11);
                g2.setColor(AdminUI.alpha(CYAN,90)); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(lx,ly,36,36,11,11);
                g2.setFont(new Font("Segoe UI",Font.BOLD,20)); g2.setColor(CYAN);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString("P",lx+(36-fm.stringWidth("P"))/2,ly+26);
                g2.setFont(new Font("Segoe UI",Font.BOLD,13)); g2.setColor(TEXT); g2.drawString("SmartParking",60,29);
                g2.setColor(AdminUI.alpha(CYAN,60)); g2.fillOval(60,36,5,5);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(AdminUI.alpha(CYAN,140)); g2.drawString("Administration",70,43);
                g2.dispose();
            }
        };
        p.setOpaque(false); p.setMaximumSize(new Dimension(218,68)); p.setPreferredSize(new Dimension(218,68));
        return p;
    }

    private JPanel sideItem(String label, String page, Color accent, String dot) {
        JPanel item = new JPanel(null) {
            boolean hover=false, pressed=false;
            { setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter(){
                    public void mouseEntered(MouseEvent e){hover=true;repaint();}
                    public void mouseExited(MouseEvent e){hover=false;pressed=false;repaint();}
                    public void mousePressed(MouseEvent e){pressed=true;repaint();}
                    public void mouseReleased(MouseEvent e){pressed=false;repaint();}
                    public void mouseClicked(MouseEvent e){navigate(page);}
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active=activePage.equals(page);
                if (active) {
                    g2.setPaint(new GradientPaint(8,0,AdminUI.alpha(accent,30),getWidth()-8,0,AdminUI.alpha(accent,8)));
                    g2.fillRoundRect(8,2,getWidth()-16,getHeight()-4,10,10);
                    g2.setColor(AdminUI.alpha(accent,55)); g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(8,2,getWidth()-16,getHeight()-4,10,10);
                    g2.setPaint(new GradientPaint(0,4,accent,0,getHeight()-4,AdminUI.alpha(accent,100)));
                    g2.fillRoundRect(0,8,3,getHeight()-16,3,3);
                } else if (pressed) {
                    g2.setColor(AdminUI.alpha(accent,18)); g2.fillRoundRect(8,2,getWidth()-16,getHeight()-4,10,10);
                } else if (hover) {
                    g2.setColor(AdminUI.alpha(AdminUI.BORDER,60)); g2.fillRoundRect(8,2,getWidth()-16,getHeight()-4,10,10);
                }
                int ix=20, iy=getHeight()/2-5;
                g2.setColor(active?accent:AdminUI.alpha(accent,hover?140:80));
                g2.fillOval(ix,iy,9,9);
                if (active) { g2.setColor(AdminUI.alpha(accent,25)); g2.fillOval(ix-3,iy-3,15,15); }
                g2.setFont(new Font("Segoe UI",active?Font.BOLD:Font.PLAIN,12));
                g2.setColor(active?accent:hover?TEXT:TEXT2);
                g2.drawString(label,40,(getHeight()+g2.getFontMetrics().getAscent()-g2.getFontMetrics().getDescent())/2);
                if (active) {
                    g2.setColor(AdminUI.alpha(accent,150));
                    int ax=getWidth()-18,ay=getHeight()/2;
                    g2.fillPolygon(new int[]{ax,ax+4,ax},new int[]{ay-3,ay,ay+3},3);
                }
                g2.dispose();
            }
        };
        item.setMaximumSize(new Dimension(218,40)); item.setPreferredSize(new Dimension(218,40));
        return item;
    }

    private JPanel buildSidebarOccupation() {
        JPanel p = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int mx=10,my=4,mw=getWidth()-20,mh=getHeight()-8;
                double safe=Math.max(0,Math.min(100,occupationPct));
                Color pc=safe>80?RED:safe>50?ORANGE:CYAN;
                g2.setColor(AdminUI.alpha(SURF2,160)); g2.fillRoundRect(mx,my,mw,mh,10,10);
                g2.setColor(AdminUI.alpha(pc,40)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(mx,my,mw,mh,10,10);
                g2.setFont(new Font("Segoe UI",Font.BOLD,8)); g2.setColor(AdminUI.alpha(TEXT2,180)); g2.drawString("OCCUPATION",mx+10,my+14);
                g2.setFont(new Font("Segoe UI",Font.BOLD,14)); g2.setColor(pc);
                String ps=String.format("%.0f%%",safe); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(ps,mx+mw-fm.stringWidth(ps)-10,my+14);
                int bx=mx+10,by=my+18,bw=mw-20,bh=4;
                g2.setColor(AdminUI.alpha(TEXT2,20)); g2.fillRoundRect(bx,by,bw,bh,bh,bh);
                int filled=safe<=0?0:Math.max(8,(int)(safe/100.0*bw));
                if(filled>0){g2.setPaint(new GradientPaint(bx,by,pc,bx+filled,by,VIOLET));g2.fillRoundRect(bx,by,filled,bh,bh,bh);}
                g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(AdminUI.alpha(CYAN,130)); g2.drawString("● Libres",bx,by+bh+10);
                g2.dispose();
            }
        };
        p.setOpaque(false); p.setMaximumSize(new Dimension(218,52)); p.setPreferredSize(new Dimension(218,52));
        return p;
    }

    private JPanel buildSidebarUserCard() {
        JPanel p = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int mx=10,my=4,mw=getWidth()-20,mh=getHeight()-8;
                g2.setColor(AdminUI.alpha(SURF2,160)); g2.fillRoundRect(mx,my,mw,mh,12,12);
                g2.setColor(AdminUI.alpha(CYAN,35)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(mx,my,mw,mh,12,12);
                int av=30,ax=mx+10,ay=my+(mh-av)/2;
                g2.setColor(AdminUI.alpha(CYAN,30)); g2.fillOval(ax,ay,av,av);
                g2.setColor(AdminUI.alpha(CYAN,80)); g2.setStroke(new BasicStroke(1.2f)); g2.drawOval(ax,ay,av,av);
                g2.setFont(new Font("Segoe UI",Font.BOLD,14)); g2.setColor(CYAN);
                String ini=login.substring(0,1).toUpperCase(); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(ini,ax+(av-fm.stringWidth(ini))/2,ay+(av+fm.getAscent()-fm.getDescent())/2);
                g2.setColor(GREEN); g2.fillOval(ax+av-8,ay+av-8,9,9);
                g2.setColor(BG); g2.setStroke(new BasicStroke(1.2f)); g2.drawOval(ax+av-8,ay+av-8,9,9);
                g2.setFont(new Font("Segoe UI",Font.BOLD,11)); g2.setColor(TEXT);
                g2.drawString(login,ax+av+10,my+(mh+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        p.setOpaque(false); p.setMaximumSize(new Dimension(218,56)); p.setPreferredSize(new Dimension(218,56));
        return p;
    }

    private JPanel sidebarDivider() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setPaint(new GradientPaint(16,0,new Color(0,0,0,0),getWidth()/2f,0,AdminUI.alpha(CYAN,28)));
                g2.fillRect(0,1,getWidth()/2,1);
                g2.setPaint(new GradientPaint(getWidth()/2f,0,AdminUI.alpha(CYAN,28),getWidth()-16,0,new Color(0,0,0,0)));
                g2.fillRect(getWidth()/2,1,getWidth()/2,1);
                g2.dispose();
            }
        };
        p.setOpaque(false); p.setMaximumSize(new Dimension(218,10)); p.setPreferredSize(new Dimension(218,10));
        return p;
    }

    private JLabel sidebarSectionLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI",Font.BOLD,8));
        l.setForeground(AdminUI.alpha(CYAN,90));
        l.setBorder(new EmptyBorder(8,18,3,0));
        l.setMaximumSize(new Dimension(218,24));
        return l;
    }

    // ── DASHBOARD PAGE ────────────────────────────────────────────────────────
    private JPanel buildDashboardPage() {
        JPanel page = new JPanel(new BorderLayout(0,12)); page.setOpaque(false); page.setBorder(new EmptyBorder(14,16,14,16));

        JPanel kpiRow = new JPanel(new GridLayout(1,6,10,0)); kpiRow.setOpaque(false); kpiRow.setPreferredSize(new Dimension(0,105));
        kpiLibres    = addKPI(kpiRow,"Places Libres","—",CYAN,  "+0 vs hier");    kpiLibresSub    = (JLabel)((JPanel)kpiRow.getComponent(0)).getClientProperty("sub");
        kpiOccupes   = addKPI(kpiRow,"Occupées",    "—",BLUE,  "0%");             kpiOccupesSub   = (JLabel)((JPanel)kpiRow.getComponent(1)).getClientProperty("sub");
        kpiRevenu    = addKPI(kpiRow,"Revenus auj.", "—",GOLD,  "0 DH");          kpiRevenuSub    = (JLabel)((JPanel)kpiRow.getComponent(2)).getClientProperty("sub");
        kpiVehicules = addKPI(kpiRow,"Passages",    "—",TEAL,  "0 auj.");         kpiVehiculesSub = (JLabel)((JPanel)kpiRow.getComponent(3)).getClientProperty("sub");
        kpiAlertes   = addKPI(kpiRow,"Alertes",     "—",RED,   "à résoudre");     kpiAlertesSub   = (JLabel)((JPanel)kpiRow.getComponent(4)).getClientProperty("sub");
        kpiAbonnes   = addKPI(kpiRow,"Abonnés",     "—",VIOLET,"actifs");         kpiAbonnesSub   = (JLabel)((JPanel)kpiRow.getComponent(5)).getClientProperty("sub");
        page.add(kpiRow, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(10,0)); middle.setOpaque(false);
        chartPanel = buildChart(); middle.add(chartPanel, BorderLayout.CENTER);
        alertesPanel = buildAlertesPanel(); alertesPanel.setPreferredSize(new Dimension(250,0));
        middle.add(alertesPanel, BorderLayout.EAST);
        page.add(middle, BorderLayout.CENTER);

        JPanel actRow = new JPanel(new GridLayout(1,7,10,0)); actRow.setOpaque(false); actRow.setPreferredSize(new Dimension(0,62));
        actRow.add(actionBtn("Abonnements", VIOLET, () -> new AbonnementsFrame(this).setVisible(true)));
        actRow.add(actionBtn("Historique",  BLUE,   () -> new HistoriqueFrame(this).setVisible(true)));
        actRow.add(actionBtn("Revenus",     GOLD,   () -> new RevenusFrame(this).setVisible(true)));
        actRow.add(actionBtn("Incidents",   RED,    () -> new IncidentsFrame(this).setVisible(true)));
        actRow.add(actionBtn("Tarifs",      TEAL,   () -> new TarifsFrame(this).setVisible(true)));
        actRow.add(actionBtn("Notifs",      PURPLE, () -> new NotificationsFrame(this).setVisible(true)));
        actRow.add(actionBtn("Carte",       CYAN,   () -> { ParkingMapPanel mp=new ParkingMapPanel(); JDialog d=new JDialog(this,"Carte",false); d.setContentPane(mp); d.setSize(700,500); d.setLocationRelativeTo(this); d.setVisible(true); }));
        page.add(actRow, BorderLayout.SOUTH);
        return page;
    }

    private JLabel addKPI(JPanel row, String titre, String val, Color accent, String sub) {
        JLabel valLbl = new JLabel(val);
        JLabel subLbl = new JLabel(sub);
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.alpha(SURF,200)); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                // Reflet en haut
                g2.setPaint(new GradientPaint(0,0,AdminUI.alpha(accent,18),0,getHeight()/3f,AdminUI.alpha(accent,0)));
                g2.fillRoundRect(0,0,getWidth(),getHeight()/3+6,14,14);
                g2.setColor(AdminUI.alpha(accent,45)); g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                // Barre bottom
                g2.setPaint(new GradientPaint(0,0,accent,getWidth()*0.6f,0,AdminUI.alpha(accent,0)));
                g2.fillRoundRect(0,getHeight()-2,getWidth(),2,2,2);
                g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(AdminUI.alpha(accent,160));
                g2.drawString(titre.toUpperCase(),12,16);
                g2.setFont(new Font("Segoe UI",Font.BOLD,26)); g2.setColor(accent);
                g2.drawString(valLbl.getText(),12,52);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(TEXT2);
                g2.drawString(subLbl.getText(),12,70);
                g2.dispose();
            }
        };
        card.putClientProperty("sub",subLbl);
        valLbl.setVisible(false); subLbl.setVisible(false);
        valLbl.addPropertyChangeListener("text", e -> card.repaint());
        subLbl.addPropertyChangeListener("text", e -> card.repaint());
        card.setOpaque(false); row.add(card);
        return valLbl;
    }

    private JPanel buildChart() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.alpha(SURF,200)); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setPaint(new GradientPaint(0,0,AdminUI.alpha(CYAN,12),0,getHeight()/3f,AdminUI.alpha(CYAN,0)));
                g2.fillRoundRect(0,0,getWidth(),getHeight()/3+6,14,14);
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(TEXT); g2.drawString("Revenus — 7 derniers jours",14,22);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(TEXT2); g2.drawString("En dirhams (DH)",14,38);
                if(chartData.isEmpty()){g2.dispose();return;}
                int padL=42,padR=18,padT=50,padB=36;
                int cw=getWidth()-padL-padR,ch=getHeight()-padT-padB;
                int n=Math.min(chartData.size(),7);
                List<Double> slice=chartData.subList(chartData.size()-n,chartData.size());
                double maxV=slice.stream().mapToDouble(Double::doubleValue).max().orElse(1); if(maxV==0)maxV=1;
                g2.setStroke(new BasicStroke(0.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{4,4},0));
                g2.setColor(AdminUI.alpha(TEXT2,18));
                for(int i=0;i<=4;i++){int y=padT+(int)(ch*(1-(double)i/4));g2.drawLine(padL,y,padL+cw,y);}
                double barW=(double)cw/n;
                GeneralPath area=new GeneralPath(), line=new GeneralPath();
                for(int i=0;i<n;i++){
                    double v=slice.get(i); int x=(int)(padL+i*barW+barW/2); int y=(int)(padT+ch*(1-v/maxV));
                    if(i==0){area.moveTo(x,padT+ch);area.lineTo(x,y);line.moveTo(x,y);}
                    else{area.lineTo(x,y);line.lineTo(x,y);}
                }
                area.lineTo((int)(padL+(n-1)*barW+barW/2),padT+ch); area.closePath();
                g2.setPaint(new GradientPaint(0,padT,AdminUI.alpha(CYAN,55),0,padT+ch,AdminUI.alpha(CYAN,5)));
                g2.fill(area);
                g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.setPaint(new GradientPaint(padL,0,CYAN,padL+cw,0,VIOLET)); g2.draw(line);
                String[] days={"J-6","J-5","J-4","J-3","J-2","J-1","Auj."};
                for(int i=0;i<n;i++){
                    double v=slice.get(i); int x=(int)(padL+i*barW+barW/2); int y=(int)(padT+ch*(1-v/maxV));
                    boolean last=(i==n-1);
                    g2.setColor(AdminUI.alpha(CYAN,last?45:20)); g2.fillOval(x-7,y-7,14,14);
                    g2.setColor(last?CYAN:AdminUI.alpha(CYAN,170)); g2.fillOval(x-3,y-3,6,6);
                    if(last||v==maxV){
                        g2.setColor(AdminUI.alpha(BG,180)); g2.fillRoundRect(x-20,y-26,40,16,6,6);
                        g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(last?CYAN:TEXT);
                        FontMetrics fm=g2.getFontMetrics(); String sv=String.format("%.0f",v);
                        g2.drawString(sv,x-fm.stringWidth(sv)/2,y-13);
                    }
                    g2.setFont(new Font("Segoe UI",last?Font.BOLD:Font.PLAIN,8)); g2.setColor(last?CYAN:TEXT2);
                    FontMetrics fm=g2.getFontMetrics(); String dl=days[7-n+i]; g2.drawString(dl,x-fm.stringWidth(dl)/2,padT+ch+14);
                }
                g2.dispose();
            }
        };
        p.setOpaque(false); return p;
    }

    private JPanel buildAlertesPanel() {
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.alpha(SURF,200)); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                g2.dispose();
            }
        };
        outer.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.setBorder(new EmptyBorder(12,14,8,14));
        header.add(lbl("Alertes actives",12,Font.BOLD,TEXT),BorderLayout.WEST);
        JButton r = AdminUI.outlineButton("↻",CYAN,e->loadAlertes());
        r.setPreferredSize(new Dimension(26,26)); header.add(r,BorderLayout.EAST);
        outer.add(header,BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(new JPanel());
        scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        outer.add(scroll,BorderLayout.CENTER);
        outer.putClientProperty("scroll",scroll);
        return outer;
    }

    private void loadAlertes() {
        JScrollPane scroll = (JScrollPane) alertesPanel.getClientProperty("scroll");
        if(scroll==null)return;
        JPanel inner = new JPanel(); inner.setLayout(new BoxLayout(inner,BoxLayout.Y_AXIS)); inner.setOpaque(false); inner.setBorder(new EmptyBorder(0,10,10,10));
        try(Connection c=Database.getConnection();
            ResultSet rs=c.createStatement().executeQuery("SELECT type_alerte,message,date_alerte FROM alertes WHERE resolue=0 ORDER BY date_alerte DESC LIMIT 10")){
            int count=0;
            while(rs.next()){
                count++;
                String type=rs.getString("type_alerte"); String msg=rs.getString("message"); String date=new SimpleDateFormat("dd/MM HH:mm").format(rs.getTimestamp("date_alerte"));
                Color ac=type!=null&&type.contains("ASSIST")?RED:ORANGE;
                inner.add(makeAlerteCard(type==null?"ALERTE":type,msg==null?"":msg,date,ac));
                inner.add(Box.createVerticalStrut(6));
            }
            if(count==0){JLabel none=lbl("Aucune alerte active",11,Font.PLAIN,TEXT2);none.setBorder(new EmptyBorder(12,4,0,0));inner.add(none);}
        }catch(Exception e){e.printStackTrace();}
        scroll.setViewportView(inner); scroll.revalidate(); scroll.repaint();
    }

    private JPanel makeAlerteCard(String type, String msg, String date, Color accent) {
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.alpha(accent,15)); g2.fillRoundRect(0,0,getWidth(),getHeight(),9,9);
                g2.setColor(AdminUI.alpha(accent,60)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,9,9);
                g2.setColor(accent); g2.fillRoundRect(0,6,3,getHeight()-12,3,3);
                g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(accent); g2.drawString(type,10,15);
                String sm=msg.length()>35?msg.substring(0,35)+"…":msg;
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(TEXT2); g2.drawString(sm,10,29);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(AdminUI.alpha(TEXT2,120)); g2.drawString(date,10,43);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setMaximumSize(new Dimension(10000,50)); card.setPreferredSize(new Dimension(0,50));
        return card;
    }

    private JPanel actionBtn(String label, Color accent, Runnable action) {
        JPanel p = new JPanel(null) {
            boolean hover=false;
            {setOpaque(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter(){
                    public void mouseEntered(MouseEvent e){hover=true;repaint();}
                    public void mouseExited(MouseEvent e){hover=false;repaint();}
                    public void mouseClicked(MouseEvent e){action.run();}
                });}
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover?AdminUI.alpha(accent,30):AdminUI.alpha(SURF,200)); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(hover?AdminUI.alpha(accent,160):AdminUI.alpha(accent,55)); g2.setStroke(new BasicStroke(hover?1.5f:1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.setColor(hover?accent:AdminUI.alpha(accent,180)); g2.fillOval(10,getHeight()/2-5,10,10);
                g2.setFont(new Font("Segoe UI",Font.BOLD,11)); g2.setColor(hover?accent:TEXT);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(label,26,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        return p;
    }

    private JPanel buildSubPlaceholder(String page) {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); return p;
    }

    private void loadData() {
        SwingWorker<Void,Void> w = new SwingWorker<>(){
            int libres,occupes,passages,alertesN,abonnes; double revenu,pct; List<Double> rev7=new ArrayList<>();
            @Override protected Void doInBackground(){
                try(Connection c=Database.getConnection()){
                    int[] e=dao.getEtatParking();libres=e[0];occupes=e[1];int total=libres+occupes;pct=total>0?(occupes*100.0/total):0;
                    ResultSet r1=c.createStatement().executeQuery("SELECT COALESCE(SUM(montant_paye),0) FROM historique_paiements WHERE DATE(date_sortie)=CURDATE()");
                    if(r1.next())revenu=r1.getDouble(1);
                    ResultSet r2=c.createStatement().executeQuery("SELECT COUNT(*) FROM historique_paiements WHERE DATE(date_sortie)=CURDATE()");
                    if(r2.next())passages=r2.getInt(1);
                    ResultSet r3=c.createStatement().executeQuery("SELECT COUNT(*) FROM alertes WHERE resolue=0");
                    if(r3.next())alertesN=r3.getInt(1);
                    ResultSet r4=c.createStatement().executeQuery("SELECT COUNT(*) FROM abonnements WHERE date_fin>=NOW()");
                    if(r4.next())abonnes=r4.getInt(1);
                    ResultSet r5=c.createStatement().executeQuery("SELECT COALESCE(SUM(montant_paye),0) as rev FROM historique_paiements WHERE date_sortie>=DATE_SUB(CURDATE(),INTERVAL 6 DAY) GROUP BY DATE(date_sortie) ORDER BY DATE(date_sortie)");
                    while(r5.next())rev7.add(r5.getDouble("rev"));
                    if(rev7.isEmpty()){for(int i=0;i<7;i++)rev7.add(0.0);}
                    while(rev7.size()<7)rev7.add(0,0.0);
                }catch(Exception ex){ex.printStackTrace();}
                return null;
            }
            @Override protected void done(){
                kpiLibres.setText(String.valueOf(libres)); kpiOccupes.setText(String.valueOf(occupes));
                kpiRevenu.setText(String.format("%.0f DH",revenu)); kpiVehicules.setText(String.valueOf(passages));
                kpiAlertes.setText(String.valueOf(alertesN)); kpiAbonnes.setText(String.valueOf(abonnes));
                if(kpiRevenuSub!=null)kpiRevenuSub.setText(revenu>0?"Aujourd'hui":"Pas de revenus");
                if(kpiAlertesSub!=null)kpiAlertesSub.setText(alertesN>0?"⚠ À résoudre":"Aucune alerte");
                occupationPct=pct; chartData.clear(); chartData.addAll(rev7);
                if(chartPanel!=null)chartPanel.repaint(); if(sidebar!=null)sidebar.repaint(); loadAlertes();
            }
        };
        w.execute();
    }

    private void startTimers() { new Timer(30000,e->loadData()).start(); }

    private void navigate(String page) {
        activePage=page; if(sidebar!=null)sidebar.repaint();
        cards.show(contentPanel,page);
        if(!"dashboard".equals(page))openFrame(page);
    }

    private void openFrame(String page) {
        switch(page){
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
        if(JOptionPane.showConfirmDialog(this,"Se déconnecter ?","Déconnexion",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
            dispose(); new LoginFrame().setVisible(true);
        }
    }

    private void paintBlob(Graphics2D g, int x, int y, int r, Color c, float a) {
        RadialGradientPaint rg=new RadialGradientPaint(new Point2D.Float(x+r/2f,y+r/2f),r/2f,
                new float[]{0f,1f},new Color[]{new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(a*255)),new Color(0,0,0,0)});
        g.setPaint(rg); g.fillOval(x,y,r,r);
    }

    private JLabel lbl(String t,int s,int st,Color c){JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",st,s));l.setForeground(c);return l;}
}