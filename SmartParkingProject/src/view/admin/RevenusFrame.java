package view.admin;

import database.Database;
import util.AdminUI;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RevenusFrame extends JDialog {

    private final Frame parent;

    // KPIs
    private JLabel kpiJour, kpiSemaine, kpiMois, kpiAnnee, kpiVsHier, kpiVsMoisPrec;

    // Onglet détail
    private JTable            tableDetail;
    private DefaultTableModel modelDetail;
    private JComboBox<String> comboPeriode;

    // Onglet véhicules
    private BarChart          chartVehicules;
    private JTable            tableVehicules;
    private DefaultTableModel modelVehicules;

    // Graphique 30j
    private LineChart chart30j;

    public RevenusFrame(Frame parent) {
        super(parent, "Revenus — Smart Parking", true);
        this.parent = parent;
        setSize(1120, 720);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(AdminUI.BG);
        buildUI();
        chargerTout();
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JLabel clock = new JLabel();
        clock.setFont(new Font("Consolas", Font.BOLD, 13));
        clock.setForeground(AdminUI.GOLD);
        new Timer(1000, e -> clock.setText(
                new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss").format(new Date()))).start();
        clock.setText(new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss").format(new Date()));
        return AdminUI.buildHeader(
                "Tableau de Bord Revenus",
                "Jour  ·  Semaine  ·  Mois  ·  Année  ·  Par véhicule",
                AdminUI.GOLD, clock);
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = AdminUI.styledTabs();
        tabs.addTab("  Vue Générale  ",   buildTabVueGenerale());
        tabs.addTab("  Détail Période  ", buildTabDetail());
        tabs.addTab("  Par Véhicule  ",   buildTabVehicules());
        tabs.addTab("  Comparaison  ",    buildTabComparaison());
        return tabs;
    }

    private JPanel buildFooter() {
        JPanel f = AdminUI.footerPanel();
        JButton btnActu  = AdminUI.createButton("Actualiser", AdminUI.TEAL,    130, 36);
        JButton btnClose = AdminUI.createButton("Fermer",     AdminUI.TEXT_SEC, 100, 36);
        btnActu.addActionListener(e -> chargerTout());
        btnClose.addActionListener(e -> dispose());
        f.add(btnActu); f.add(btnClose);
        return f;
    }

    // ── Onglet 1 : Vue Générale ───────────────────────────────────────────────
    private JPanel buildTabVueGenerale() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 16, 10, 16));

        // KPI row
        JPanel kpiRow = new JPanel(new GridLayout(1, 6, 10, 0));
        kpiRow.setOpaque(false);
        kpiRow.setPreferredSize(new Dimension(0, 90));

        kpiJour       = kpiCard("AUJOURD'HUI",   "-- DH", AdminUI.GOLD);
        kpiSemaine    = kpiCard("CETTE SEMAINE",  "-- DH", AdminUI.CYAN);
        kpiMois       = kpiCard("CE MOIS",        "-- DH", AdminUI.GREEN);
        kpiAnnee      = kpiCard("CETTE ANNÉE",    "-- DH", AdminUI.VIOLET);
        kpiVsHier     = kpiCard("VS HIER",        "--",    AdminUI.TEAL);
        kpiVsMoisPrec = kpiCard("VS MOIS PRÉC.",  "--",    AdminUI.ORANGE);

        kpiRow.add(wrapKpi(kpiJour));
        kpiRow.add(wrapKpi(kpiSemaine));
        kpiRow.add(wrapKpi(kpiMois));
        kpiRow.add(wrapKpi(kpiAnnee));
        kpiRow.add(wrapKpi(kpiVsHier));
        kpiRow.add(wrapKpi(kpiVsMoisPrec));
        p.add(kpiRow, BorderLayout.NORTH);

        // Graphique 30 jours
        JPanel gWrap = new JPanel(new BorderLayout(0, 8));
        gWrap.setOpaque(false);
        JLabel gTitle = new JLabel("Revenus des 30 derniers jours");
        gTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gTitle.setForeground(AdminUI.alpha(AdminUI.GOLD, 200));
        gTitle.setBorder(new EmptyBorder(0, 0, 4, 0));
        chart30j = new LineChart();
        gWrap.add(gTitle,   BorderLayout.NORTH);
        gWrap.add(chart30j, BorderLayout.CENTER);
        p.add(gWrap, BorderLayout.CENTER);
        return p;
    }

    // ── Onglet 2 : Détail ─────────────────────────────────────────────────────
    private JPanel buildTabDetail() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(14, 14, 0, 14));

        // Barre filtre
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        bar.setOpaque(false);
        JLabel lbl = new JLabel("Période :");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AdminUI.TEXT_SEC);
        comboPeriode = AdminUI.createCombo(new String[]{
                "Aujourd'hui", "7 derniers jours", "30 derniers jours",
                "Ce mois", "3 derniers mois", "Cette année"});
        comboPeriode.setPreferredSize(new Dimension(210, 34));
        JButton btnFiltrer = AdminUI.outlineButton("Appliquer", AdminUI.GOLD,
                e -> chargerDetail(comboPeriode.getSelectedIndex()));
        bar.add(lbl); bar.add(comboPeriode); bar.add(btnFiltrer);
        p.add(bar, BorderLayout.NORTH);

        // Table
        modelDetail = new DefaultTableModel(
                new String[]{"Période", "Nb Véhicules", "Revenus (DH)", "Moy. / Véhicule", "Abonnés"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableDetail = new JTable(modelDetail);
        AdminUI.styleTable(tableDetail, AdminUI.GOLD);
        tableDetail.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(RIGHT);
                setFont(new Font("Consolas", Font.BOLD, 12));
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setBackground(s ? AdminUI.alpha(AdminUI.GOLD, 45)
                        : r % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setForeground(AdminUI.GOLD);
                return this;
            }
        });

        // Boutons export
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btns.setOpaque(false);
        btns.add(AdminUI.outlineButton("Exporter CSV", AdminUI.GREEN,  e -> exporterCSV()));
        btns.add(AdminUI.outlineButton("Imprimer PDF", AdminUI.VIOLET, e -> imprimerPDF()));

        p.add(AdminUI.scrollTable(tableDetail), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    // ── Onglet 3 : Par véhicule ───────────────────────────────────────────────
    private JPanel buildTabVehicules() {
        JPanel p = new JPanel(new GridLayout(1, 2, 14, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(14, 14, 10, 14));

        // Graphique barres
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        JLabel lt = new JLabel("Revenus par type de véhicule (30 jours)");
        lt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lt.setForeground(AdminUI.alpha(AdminUI.GOLD, 200));
        chartVehicules = new BarChart();
        left.add(lt,             BorderLayout.NORTH);
        left.add(chartVehicules, BorderLayout.CENTER);

        // Table
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        JLabel rt = new JLabel("Détail par type");
        rt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        rt.setForeground(AdminUI.alpha(AdminUI.GOLD, 200));
        modelVehicules = new DefaultTableModel(
                new String[]{"Type", "Passages", "Revenus (DH)", "%", "Durée Moy."}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableVehicules = new JTable(modelVehicules);
        AdminUI.styleTable(tableVehicules, AdminUI.GOLD);
        tableVehicules.setRowHeight(44);
        tableVehicules.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                String type = v != null ? v.toString() : "";
                Color col = switch (type) {
                    case "Voiture" -> AdminUI.BLUE;
                    case "Moto"    -> AdminUI.GREEN;
                    case "Camion"  -> AdminUI.ORANGE;
                    default        -> AdminUI.GOLD;
                };
                setForeground(col);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBackground(s ? AdminUI.alpha(AdminUI.GOLD, 45)
                        : r % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setHorizontalAlignment(CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
        right.add(rt,                              BorderLayout.NORTH);
        right.add(AdminUI.scrollTable(tableVehicules), BorderLayout.CENTER);

        p.add(left); p.add(right);
        return p;
    }

    // ── Onglet 4 : Comparaison ────────────────────────────────────────────────
    private JPanel buildTabComparaison() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 16, 10, 16));

        // Cards comparaison
        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(0, 200));
        String[][] comps = {
                {"Aujourd'hui vs Hier",      "jour"},
                {"Cette Semaine vs Préc.",   "semaine"},
                {"Ce Mois vs Mois Préc.",    "mois"},
                {"Nb Véhicules / Jour",      "vehicules"},
                {"Revenu Moyen / Véhicule",  "moyen"},
                {"Meilleur Jour du Mois",    "meilleur"}
        };
        for (String[] comp : comps) cards.add(buildCompCard(comp[0], comp[1]));
        p.add(cards, BorderLayout.NORTH);

        // Table 6 mois
        JPanel bot = new JPanel(new BorderLayout(0, 8));
        bot.setOpaque(false);
        JLabel bt = new JLabel("Comparaison des 6 derniers mois");
        bt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bt.setForeground(AdminUI.alpha(AdminUI.GOLD, 200));
        DefaultTableModel mComp = new DefaultTableModel(
                new String[]{"Mois", "Revenus", "Nb Véhicules", "Moy./Véhicule", "Croissance"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tComp = new JTable(mComp);
        AdminUI.styleTable(tComp, AdminUI.GOLD);
        tComp.setRowHeight(38);
        tComp.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                String val = v != null ? v.toString() : "";
                setForeground(val.startsWith("+") ? AdminUI.GREEN
                        : val.startsWith("-") ? AdminUI.RED : AdminUI.TEXT_SEC);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(CENTER);
                setBackground(s ? AdminUI.alpha(AdminUI.GOLD, 45)
                        : r % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
        chargerComparaisonMensuelle(mComp);
        bot.add(bt,                           BorderLayout.NORTH);
        bot.add(AdminUI.scrollTable(tComp),   BorderLayout.CENTER);
        p.add(bot, BorderLayout.CENTER);
        return p;
    }

    // ── Données ───────────────────────────────────────────────────────────────
    private void chargerTout() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                chargerKPI();
                chargerGraphique30Jours();
                chargerDetail(0);
                chargerVehicules();
                return null;
            }
        };
        w.execute();
    }

    private void chargerKPI() {
        try (Connection c = Database.getConnection()) {
            double jour   = rev(c, "DATE(date_sortie)=CURDATE()");
            double hier   = rev(c, "DATE(date_sortie)=DATE_SUB(CURDATE(),INTERVAL 1 DAY)");
            double sem    = rev(c, "YEARWEEK(date_sortie,1)=YEARWEEK(NOW(),1)");
            double mois   = rev(c, "YEAR(date_sortie)=YEAR(NOW()) AND MONTH(date_sortie)=MONTH(NOW())");
            double annee  = rev(c, "YEAR(date_sortie)=YEAR(NOW())");
            double moisP  = rev(c, "YEAR(date_sortie)=YEAR(DATE_SUB(NOW(),INTERVAL 1 MONTH)) AND MONTH(date_sortie)=MONTH(DATE_SUB(NOW(),INTERVAL 1 MONTH))");
            SwingUtilities.invokeLater(() -> {
                kpiJour.setText(String.format("%.2f DH", jour));
                kpiSemaine.setText(String.format("%.2f DH", sem));
                kpiMois.setText(String.format("%.2f DH", mois));
                kpiAnnee.setText(String.format("%.2f DH", annee));
                kpiVsHier.setText(hier > 0
                        ? String.format("%+.1f%%", ((jour - hier) / hier) * 100) : jour > 0 ? "+100%" : "--");
                kpiVsMoisPrec.setText(moisP > 0
                        ? String.format("%+.1f%%", ((mois - moisP) / moisP) * 100) : mois > 0 ? "+100%" : "--");
                // Couleur vs
                Color cvh = hier > 0 && jour >= hier ? AdminUI.GREEN : AdminUI.RED;
                Color cvm = moisP > 0 && mois >= moisP ? AdminUI.GREEN : AdminUI.RED;
                kpiVsHier.putClientProperty("accent", cvh);
                kpiVsMoisPrec.putClientProperty("accent", cvm);
                kpiVsHier.repaint(); kpiVsMoisPrec.repaint();
            });
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private double rev(Connection c, String where) throws SQLException {
        ResultSet rs = c.createStatement().executeQuery(
                "SELECT COALESCE(SUM(montant_paye),0) FROM historique_paiements WHERE " + where);
        return rs.next() ? rs.getDouble(1) : 0;
    }

    private void chargerGraphique30Jours() {
        List<double[]> data = new ArrayList<>();
        try (Connection c = Database.getConnection();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT DATE(date_sortie) as j, SUM(montant_paye) as t " +
                             "FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY) " +
                             "GROUP BY DATE(date_sortie) ORDER BY j ASC")) {
            while (rs.next()) data.add(new double[]{rs.getDate("j").getTime(), rs.getDouble("t")});
        } catch (SQLException e) { e.printStackTrace(); }
        SwingUtilities.invokeLater(() -> chart30j.setData(data));
    }

    private void chargerDetail(int idx) {
        modelDetail.setRowCount(0);
        String groupBy, where;
        switch (idx) {
            case 0:  where = "DATE(date_sortie)=CURDATE()";                              groupBy = "HOUR(date_sortie)";  break;
            case 1:  where = "date_sortie>=DATE_SUB(NOW(),INTERVAL 7 DAY)";              groupBy = "DATE(date_sortie)";  break;
            case 2:  where = "date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY)";             groupBy = "DATE(date_sortie)";  break;
            case 3:  where = "YEAR(date_sortie)=YEAR(NOW()) AND MONTH(date_sortie)=MONTH(NOW())"; groupBy = "DATE(date_sortie)"; break;
            case 4:  where = "date_sortie>=DATE_SUB(NOW(),INTERVAL 90 DAY)";             groupBy = "DATE(date_sortie)";  break;
            default: where = "YEAR(date_sortie)=YEAR(NOW())";                            groupBy = "MONTH(date_sortie)"; break;
        }
        try (Connection c = Database.getConnection();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT " + groupBy + " as p, COUNT(*) as nb, SUM(montant_paye) as rev, AVG(montant_paye) as moy " +
                             "FROM historique_paiements WHERE " + where +
                             " GROUP BY " + groupBy + " ORDER BY " + groupBy + " DESC")) {
            while (rs.next()) modelDetail.addRow(new Object[]{
                    rs.getString("p"), rs.getInt("nb"),
                    String.format("%.2f DH", rs.getDouble("rev")),
                    String.format("%.2f DH", rs.getDouble("moy")), "--"});
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void chargerVehicules() {
        modelVehicules.setRowCount(0);
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        List<Color>  colors = new ArrayList<>();
        double totalRev = 0;
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Database.getConnection();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT type_vehicule, COUNT(*) as nb, SUM(montant_paye) as rev, AVG(duree_minutes) as duree " +
                             "FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY) " +
                             "GROUP BY type_vehicule ORDER BY rev DESC")) {
            while (rs.next()) {
                double r = rs.getDouble("rev");
                totalRev += r;
                rows.add(new Object[]{rs.getString("type_vehicule"), rs.getInt("nb"), r, rs.getDouble("duree")});
                labels.add(rs.getString("type_vehicule"));
                values.add(r);
                colors.add(switch (rs.getString("type_vehicule")) {
                    case "Voiture" -> AdminUI.BLUE;
                    case "Moto"    -> AdminUI.GREEN;
                    case "Camion"  -> AdminUI.ORANGE;
                    default        -> AdminUI.GOLD;
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        final double total = totalRev;
        SwingUtilities.invokeLater(() -> {
            for (Object[] row : rows) {
                double pct = total > 0 ? ((double) row[2] / total) * 100 : 0;
                modelVehicules.addRow(new Object[]{
                        row[0], row[1],
                        String.format("%.2f DH", row[2]),
                        String.format("%.1f%%", pct),
                        String.format("%.0f min", row[3])});
            }
            chartVehicules.setData(labels, values, colors);
        });
    }

    private void chargerComparaisonMensuelle(DefaultTableModel m) {
        try (Connection c = Database.getConnection();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT DATE_FORMAT(date_sortie,'%Y-%m') as mois, SUM(montant_paye) as rev, " +
                             "COUNT(*) as nb, AVG(montant_paye) as moy " +
                             "FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 6 MONTH) " +
                             "GROUP BY DATE_FORMAT(date_sortie,'%Y-%m') ORDER BY mois DESC")) {
            double prevRev = -1;
            while (rs.next()) {
                double rev = rs.getDouble("rev");
                String cr = "--";
                if (prevRev > 0) cr = String.format("%+.1f%%", ((prevRev - rev) / rev) * 100);
                m.addRow(new Object[]{rs.getString("mois"),
                        String.format("%.2f DH", rev), rs.getInt("nb"),
                        String.format("%.2f DH", rs.getDouble("moy")), cr});
                prevRev = rev;
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Carte comparaison ─────────────────────────────────────────────────────
    private JPanel buildCompCard(String titre, String type) {
        JPanel card = new JPanel(null) {
            String val1 = "--", val2 = "--";
            double pct = 0;
            Color accentColor = AdminUI.GOLD;
            {
                SwingUtilities.invokeLater(() -> {
                    try (Connection c = Database.getConnection()) {
                        switch (type) {
                            case "jour":
                                val1 = fmt(rev(c, "DATE(date_sortie)=CURDATE()"));
                                double h = rev(c, "DATE(date_sortie)=DATE_SUB(CURDATE(),INTERVAL 1 DAY)");
                                val2 = fmt(h);
                                if (h > 0) { pct = ((rev(c, "DATE(date_sortie)=CURDATE()") - h) / h) * 100; accentColor = pct >= 0 ? AdminUI.GREEN : AdminUI.RED; }
                                break;
                            case "semaine":
                                double sw = rev(c, "YEARWEEK(date_sortie,1)=YEARWEEK(NOW(),1)-1");
                                val1 = fmt(rev(c, "YEARWEEK(date_sortie,1)=YEARWEEK(NOW(),1)"));
                                val2 = fmt(sw);
                                if (sw > 0) { pct = ((rev(c, "YEARWEEK(date_sortie,1)=YEARWEEK(NOW(),1)") - sw) / sw) * 100; accentColor = pct >= 0 ? AdminUI.GREEN : AdminUI.RED; }
                                break;
                            case "mois":
                                double mo = rev(c, "YEAR(date_sortie)=YEAR(NOW()) AND MONTH(date_sortie)=MONTH(NOW())");
                                double mp = rev(c, "YEAR(date_sortie)=YEAR(DATE_SUB(NOW(),INTERVAL 1 MONTH)) AND MONTH(date_sortie)=MONTH(DATE_SUB(NOW(),INTERVAL 1 MONTH))");
                                val1 = fmt(mo); val2 = fmt(mp);
                                if (mp > 0) { pct = ((mo - mp) / mp) * 100; accentColor = pct >= 0 ? AdminUI.GREEN : AdminUI.RED; }
                                break;
                            case "vehicules":
                                ResultSet r = c.createStatement().executeQuery("SELECT COALESCE(AVG(nb),0) FROM (SELECT DATE(date_sortie),COUNT(*) as nb FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY) GROUP BY DATE(date_sortie)) t");
                                val1 = r.next() ? String.format("%.1f veh/j", r.getDouble(1)) : "--"; val2 = "30 jours"; accentColor = AdminUI.CYAN;
                                break;
                            case "moyen":
                                ResultSet r2 = c.createStatement().executeQuery("SELECT COALESCE(AVG(montant_paye),0) FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY)");
                                val1 = r2.next() ? String.format("%.2f DH", r2.getDouble(1)) : "--"; val2 = "30 jours"; accentColor = AdminUI.TEAL;
                                break;
                            case "meilleur":
                                ResultSet r3 = c.createStatement().executeQuery("SELECT DATE(date_sortie) as j, SUM(montant_paye) as t FROM historique_paiements WHERE MONTH(date_sortie)=MONTH(NOW()) GROUP BY j ORDER BY t DESC LIMIT 1");
                                if (r3.next()) { val1 = new SimpleDateFormat("dd/MM").format(r3.getDate("j")); val2 = fmt(r3.getDouble("t")); accentColor = AdminUI.VIOLET; }
                                break;
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                    repaint();
                });
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fond glass
                g2.setColor(AdminUI.SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Reflet top
                g2.setPaint(new GradientPaint(0, 0, AdminUI.alpha(accentColor, 18), 0, getHeight() / 2f, AdminUI.alpha(accentColor, 0)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2 + 8, 14, 14);
                // Barre colorée en haut
                g2.setPaint(new GradientPaint(0, 0, accentColor, getWidth() * 0.6f, 0, AdminUI.alpha(accentColor, 0)));
                g2.fillRoundRect(0, 0, getWidth(), 2, 2, 2);
                // Bordure
                g2.setColor(AdminUI.alpha(accentColor, 45));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                // Textes
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(AdminUI.alpha(accentColor, 160));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(titre.toUpperCase(), (getWidth() - fm.stringWidth(titre.toUpperCase())) / 2, 18);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.setColor(accentColor);
                fm = g2.getFontMetrics();
                g2.drawString(val1, (getWidth() - fm.stringWidth(val1)) / 2, 48);
                if (!val2.equals("--")) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    g2.setColor(AdminUI.TEXT_SEC);
                    fm = g2.getFontMetrics();
                    g2.drawString("Préc: " + val2, (getWidth() - fm.stringWidth("Préc: " + val2)) / 2, 64);
                }
                if (pct != 0) {
                    String ps = String.format("%+.1f%%", pct);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    g2.setColor(pct >= 0 ? AdminUI.GREEN : AdminUI.RED);
                    fm = g2.getFontMetrics();
                    g2.drawString(ps, (getWidth() - fm.stringWidth(ps)) / 2, 82);
                }
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    // ── Export CSV ────────────────────────────────────────────────────────────
    private void exporterCSV() {
        if (modelDetail.getRowCount() == 0) {
            NotificationManager.show(parent, "Aucune donnée à exporter.", NotificationManager.Type.WARNING);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("revenus_parking.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
            pw.println("Période,Nb Véhicules,Revenus (DH),Moy. / Véhicule,Abonnés");
            for (int i = 0; i < modelDetail.getRowCount(); i++) {
                StringBuilder row = new StringBuilder();
                for (int j = 0; j < modelDetail.getColumnCount(); j++) {
                    if (j > 0) row.append(",");
                    row.append(modelDetail.getValueAt(i, j));
                }
                pw.println(row);
            }
            NotificationManager.show(parent, "Export CSV réussi !", NotificationManager.Type.SUCCESS);
        } catch (IOException e) {
            NotificationManager.show(parent, "Erreur export CSV.", NotificationManager.Type.ERROR);
        }
    }

    // ── PDF tableau simple ────────────────────────────────────────────────────
    private void imprimerPDF() {
        if (modelDetail.getRowCount() == 0) {
            NotificationManager.show(parent, "Aucune donnée à imprimer.", NotificationManager.Type.WARNING);
            return;
        }

        final Color HDR_BG   = new Color(20, 30, 55);
        final Color HDR_FG   = new Color(255, 190, 50);
        final Color ROW_ODD  = new Color(18, 22, 38);
        final Color ROW_EVEN = new Color(24, 30, 50);
        final Color ROW_FG   = new Color(220, 228, 255);
        final Color MONTANT  = new Color(255, 190, 50);
        final Color BORDER   = new Color(40, 55, 90);
        final Color TITLE_C  = new Color(255, 190, 50);
        final Color TOT_BG   = new Color(12, 18, 35);

        final String[] COLS   = {"Période", "Nb Véhicules", "Revenus (DH)", "Moy./Véhicule", "Abonnés"};
        final int[]    WIDTHS = {110, 90, 100, 110, 80};
        final int ROW_H = 22, HDR_H = 26, ML = 36, MT = 70, MB = 46;

        // Collecter données
        int rowCount = modelDetail.getRowCount();
        Object[][] data = new Object[rowCount][5];
        double totalRev = 0;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < 5; c++) data[r][c] = modelDetail.getValueAt(r, c);
            try {
                String ms = data[r][2].toString().replace(" DH","").trim();
                totalRev += Double.parseDouble(ms);
            } catch (Exception ignored) {}
        }
        final double total = totalRev;
        final String exportDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .format(LocalDateTime.now());
        final String periode = comboPeriode.getSelectedItem() != null
                ? comboPeriode.getSelectedItem().toString() : "";

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Revenus Smart Parking");

        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        paper.setSize(841.89, 595.28);
        paper.setImageableArea(0, 0, 841.89, 595.28);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.LANDSCAPE);

        int tableWTmp = 0; for (int w : WIDTHS) tableWTmp += w;
        final int tableW = tableWTmp;

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            double pageH = pageFormat.getImageableHeight();
            int usableH   = (int)(pageH - MT - MB);
            int rowsPerPg = Math.max(1, (usableH - HDR_H - 28) / ROW_H);
            int totalPages = (int) Math.ceil((double) rowCount / rowsPerPg);
            if (totalPages == 0) totalPages = 1;
            if (pageIndex >= totalPages) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            // ── Titre page 1 ──────────────────────────────────────────────
            if (pageIndex == 0) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                g2.setColor(TITLE_C);
                g2.drawString("SMART PARKING — Rapport Revenus", ML, 32);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(110, 125, 165));
                g2.drawString("Période : " + periode + "   |   Exporté le : " + exportDate
                                + "   |   " + rowCount + " ligne(s)   |   Total : " + String.format("%.2f DH", total),
                        ML, 48);
                g2.setColor(TITLE_C);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawLine(ML, 54, ML + tableW, 54);
            }

            // ── Header tableau ─────────────────────────────────────────────
            int y = MT;
            g2.setColor(HDR_BG);
            g2.fillRect(ML, y, tableW, HDR_H);
            g2.setColor(HDR_FG);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(ML, y, tableW, HDR_H);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            int x = ML;
            for (int c = 0; c < COLS.length; c++) {
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (WIDTHS[c] - fm.stringWidth(COLS[c])) / 2;
                g2.setColor(HDR_FG);
                g2.drawString(COLS[c], tx, y + HDR_H - 8);
                if (c < COLS.length - 1) {
                    g2.setColor(BORDER);
                    g2.drawLine(x + WIDTHS[c], y, x + WIDTHS[c], y + HDR_H);
                }
                x += WIDTHS[c];
            }
            y += HDR_H;

            // ── Lignes ─────────────────────────────────────────────────────
            int startRow = pageIndex * rowsPerPg;
            int endRow   = Math.min(startRow + rowsPerPg, rowCount);
            for (int r = startRow; r < endRow; r++) {
                g2.setColor(r % 2 == 0 ? ROW_ODD : ROW_EVEN);
                g2.fillRect(ML, y, tableW, ROW_H);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(0.4f));
                g2.drawLine(ML, y + ROW_H, ML + tableW, y + ROW_H);
                x = ML;
                for (int c = 0; c < 5; c++) {
                    String val = data[r][c] != null ? data[r][c].toString() : "";
                    g2.setFont(new Font(c == 2 || c == 3 ? "Courier New" : "Segoe UI",
                            c == 2 ? Font.BOLD : Font.PLAIN, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    while (fm.stringWidth(val) > WIDTHS[c] - 8 && val.length() > 3)
                        val = val.substring(0, val.length() - 4) + "…";
                    g2.setColor(c == 2 ? MONTANT : ROW_FG);
                    int tx = (c == 0) ? x + 6
                            : x + (WIDTHS[c] - fm.stringWidth(val)) / 2;
                    g2.drawString(val, tx, y + ROW_H - 7);
                    if (c < 4) { g2.setColor(BORDER); g2.drawLine(x + WIDTHS[c], y, x + WIDTHS[c], y + ROW_H); }
                    x += WIDTHS[c];
                }
                y += ROW_H;
            }

            // ── Bordure extérieure ─────────────────────────────────────────
            g2.setColor(TITLE_C);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(ML, MT, tableW, HDR_H + (endRow - startRow) * ROW_H);

            // ── Total (dernière page) ──────────────────────────────────────
            if (pageIndex == totalPages - 1) {
                y += 4;
                g2.setColor(TOT_BG);
                g2.fillRect(ML, y, tableW, 24);
                g2.setColor(AdminUI.alpha(TITLE_C, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(ML, y, tableW, 24);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(MONTANT);
                g2.drawString("TOTAL GÉNÉRAL", ML + 8, y + 16);
                String ts = String.format("%.2f DH", total);
                FontMetrics fm = g2.getFontMetrics();
                int totalX = ML + WIDTHS[0] + WIDTHS[1] + (WIDTHS[2] - fm.stringWidth(ts)) / 2;
                g2.drawString(ts, totalX, y + 16);
            }

            // ── Pied de page ───────────────────────────────────────────────
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(110, 125, 165));
            g2.drawString("Smart Parking System  —  Page " + (pageIndex + 1) + "/" + totalPages,
                    ML, (int)(pageH - 18));
            g2.drawString(exportDate, ML + tableW - 80, (int)(pageH - 18));
            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog()) {
            try { job.print(); NotificationManager.show(parent, "PDF envoyé à l'imprimante !", NotificationManager.Type.SUCCESS); }
            catch (PrinterException e) { NotificationManager.show(parent, "Erreur : " + e.getMessage(), NotificationManager.Type.ERROR); }
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────
    private String fmt(double v) { return String.format("%.2f DH", v); }

    private JLabel kpiCard(String titre, String valInit, Color accent) {
        JLabel l = new JLabel(valInit) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color ac = getClientProperty("accent") instanceof Color
                        ? (Color) getClientProperty("accent") : accent;
                g2.setColor(AdminUI.SURFACE); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setPaint(new GradientPaint(0,0,AdminUI.alpha(ac,18),0,getHeight()/2f,AdminUI.alpha(ac,0)));
                g2.fillRoundRect(0,0,getWidth(),getHeight()/2+6,14,14);
                g2.setColor(AdminUI.alpha(ac,45)); g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                g2.setPaint(new GradientPaint(0,0,ac,getWidth()*0.6f,0,AdminUI.alpha(ac,0)));
                g2.fillRoundRect(0,getHeight()-2,getWidth(),2,2,2);
                g2.setFont(new Font("Segoe UI",Font.BOLD,8)); g2.setColor(AdminUI.alpha(ac,160));
                g2.drawString(titre,12,16);
                g2.setFont(new Font("Segoe UI",Font.BOLD,20)); g2.setColor(ac);
                g2.drawString(getText(),12,50);
                g2.dispose();
            }
        };
        l.putClientProperty("accent", accent);
        l.addPropertyChangeListener("text", e -> l.repaint());
        return l;
    }

    private JPanel wrapKpi(JLabel kpi) {
        JPanel w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        w.add(kpi);
        return w;
    }

    // ── Graphique ligne 30j ───────────────────────────────────────────────────
    class LineChart extends JPanel {
        private List<double[]> data = new ArrayList<>();
        void setData(List<double[]> d) { this.data = d; repaint(); }
        { setOpaque(false); setBorder(BorderFactory.createLineBorder(AdminUI.BORDER)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AdminUI.alpha(AdminUI.SURFACE, 200));
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (data == null || data.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                g2.setColor(AdminUI.TEXT_SEC);
                g2.drawString("Aucune donnée disponible", getWidth()/2 - 90, getHeight()/2);
                g2.dispose(); return;
            }
            int pad=48, w=getWidth()-pad*2, h=getHeight()-pad*2;
            double maxV = data.stream().mapToDouble(d->d[1]).max().orElse(1);
            if (maxV == 0) maxV = 1;
            // Grille
            g2.setStroke(new BasicStroke(0.5f));
            for (int i=0; i<=4; i++) {
                int y = pad + h - i*(h/4);
                g2.setColor(AdminUI.alpha(AdminUI.BORDER_A, 40));
                g2.drawLine(pad, y, pad+w, y);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g2.setColor(AdminUI.TEXT_SEC);
                g2.drawString(String.format("%.0f", maxV*i/4), 4, y+4);
            }
            int n = data.size();
            int[] xs = new int[n], ys = new int[n];
            for (int i=0; i<n; i++) {
                xs[i] = pad + i*w/(n>1?n-1:1);
                ys[i] = pad + h - (int)(data.get(i)[1]/maxV*h);
            }
            // Aire dégradée
            GeneralPath area = new GeneralPath();
            area.moveTo(xs[0], pad+h);
            for (int i=0; i<n; i++) area.lineTo(xs[i], ys[i]);
            area.lineTo(xs[n-1], pad+h); area.closePath();
            g2.setPaint(new GradientPaint(0,pad,AdminUI.alpha(AdminUI.GOLD,55),0,pad+h,AdminUI.alpha(AdminUI.GOLD,5)));
            g2.fill(area);
            // Courbe
            g2.setPaint(new GradientPaint(pad,0,AdminUI.GOLD,pad+w,0,AdminUI.ORANGE));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i=0; i<n-1; i++) g2.drawLine(xs[i],ys[i],xs[i+1],ys[i+1]);
            // Points
            for (int i=0; i<n; i++) {
                boolean last = i==n-1;
                g2.setColor(AdminUI.alpha(AdminUI.GOLD, last?50:20)); g2.fillOval(xs[i]-7,ys[i]-7,14,14);
                g2.setColor(last?AdminUI.GOLD:AdminUI.alpha(AdminUI.GOLD,180)); g2.fillOval(xs[i]-3,ys[i]-3,6,6);
            }
            // Labels X
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            g2.setColor(AdminUI.TEXT_SEC);
            for (int i=0; i<n; i+=Math.max(1,n/8)) {
                String lbl = new SimpleDateFormat("dd/MM").format(new Date((long)data.get(i)[0]));
                g2.drawString(lbl, xs[i]-12, pad+h+14);
            }
            g2.dispose();
        }
    }

    // ── Graphique barres véhicules ────────────────────────────────────────────
    class BarChart extends JPanel {
        private List<String> labels = new ArrayList<>();
        private List<Double> values = new ArrayList<>();
        private List<Color>  colors = new ArrayList<>();
        void setData(List<String> l, List<Double> v, List<Color> c) {
            labels=l; values=v; colors=c; repaint();
        }
        { setOpaque(false); setBorder(BorderFactory.createLineBorder(AdminUI.BORDER)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AdminUI.alpha(AdminUI.SURFACE, 200));
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (values == null || values.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                g2.setColor(AdminUI.TEXT_SEC);
                g2.drawString("Aucune donnée", getWidth()/2-50, getHeight()/2);
                g2.dispose(); return;
            }
            int pad=48, w=getWidth()-pad*2, h=getHeight()-pad*2;
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
            if (max == 0) max = 1;
            int n = values.size(), bw = w/Math.max(n,1) - 14;
            for (int i=0; i<n; i++) {
                int bh = (int)(values.get(i)/max*h);
                int bx = pad + i*(w/n)+7, by = pad+h-bh;
                Color c = i < colors.size() ? colors.get(i) : AdminUI.GOLD;
                // Barre
                g2.setPaint(new GradientPaint(bx, by, c, bx, by+bh, AdminUI.alpha(c, 60)));
                g2.fillRoundRect(bx, by, bw, bh, 8, 8);
                g2.setColor(AdminUI.alpha(c, 140));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(bx, by, bw, bh, 8, 8);
                // Halo en haut
                g2.setColor(AdminUI.alpha(c, 30));
                g2.fillOval(bx + bw/2 - 8, by - 6, 16, 12);
                // Label bas
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                g2.setColor(AdminUI.TEXT_SEC);
                String lbl = i < labels.size() ? labels.get(i) : "";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lbl, bx+(bw-fm.stringWidth(lbl))/2, pad+h+14);
                // Valeur au-dessus
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(c);
                String val = String.format("%.0f DH", values.get(i));
                fm = g2.getFontMetrics();
                g2.drawString(val, bx+(bw-fm.stringWidth(val))/2, by-6);
            }
            // Grille horizontale
            g2.setColor(AdminUI.alpha(AdminUI.BORDER_A, 30));
            g2.setStroke(new BasicStroke(0.5f));
            for (int i=0; i<=4; i++) {
                int y = pad+h-i*(h/4);
                g2.drawLine(pad, y, pad+w, y);
            }
            g2.dispose();
        }
    }
}