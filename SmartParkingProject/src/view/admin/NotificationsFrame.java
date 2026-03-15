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
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class NotificationsFrame extends AdminUI.AdminDialog {

    private JTable            tableNotifs;
    private DefaultTableModel modelNotifs;
    private JTextArea         rapportArea;
    private JLabel            lblStats;
    private Frame             parentFrame;

    public NotificationsFrame(Frame parent) {
        super(parent, "Notifications & Rapports", 1020, 660);
        this.parentFrame = parent;
        init();
        chargerAbonnesExpirants(7);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    @Override
    protected JPanel buildHeader() {
        lblStats = new JLabel("");
        lblStats.setFont(new Font("Consolas", Font.BOLD, 12));
        return AdminUI.buildHeader(
                "Notifications & Rapports",
                "Alertes abonnements  ·  Rapport journalier  ·  Paramètres",
                AdminUI.VIOLET, lblStats);
    }

    // ── Center ────────────────────────────────────────────────────────────────
    @Override
    protected JComponent buildCenter() {
        JTabbedPane tabs = AdminUI.styledTabs();
        tabs.addTab("  Abonnements expirants  ", buildTabNotifications());
        tabs.addTab("  Rapport journalier  ",     buildTabRapport());
        tabs.addTab("  Paramètres  ",             buildTabParametres());
        return tabs;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    @Override
    protected JPanel buildFooter() {
        JPanel f = AdminUI.footerPanel();
        JButton btnFermer = AdminUI.createButton("Fermer", AdminUI.TEXT_SEC, 100, 36);
        btnFermer.addActionListener(e -> dispose());
        f.add(btnFermer);
        return f;
    }

    // ── Onglet 1 : Abonnements expirants ─────────────────────────────────────
    private JPanel buildTabNotifications() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(14, 14, 0, 14));

        // Barre filtre glassmorphism
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AdminUI.SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AdminUI.BORDER);
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel lblExpire = new JLabel("Expire dans :");
        lblExpire.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblExpire.setForeground(AdminUI.TEXT_SEC);

        JComboBox<String> cJours = AdminUI.createCombo(
                new String[]{"3 jours", "7 jours", "14 jours", "30 jours"});
        cJours.setSelectedIndex(1);
        cJours.setPreferredSize(new Dimension(160, 34));

        JButton btnFiltrer = AdminUI.outlineButton("Filtrer", AdminUI.TEAL,
                e -> chargerAbonnesExpirants(new int[]{3,7,14,30}[cJours.getSelectedIndex()]));
        btnFiltrer.setPreferredSize(new Dimension(90, 34));

        left.add(lblExpire); left.add(cJours); left.add(btnFiltrer);

        // Compteur pill
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(lblStats);

        topBar.add(left,  BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);

        // Table
        modelNotifs = new DefaultTableModel(
                new String[]{"Matricule", "Nom", "Formule", "Expire le", "Jours restants", "Contact", "Action"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 6; }
        };
        tableNotifs = new JTable(modelNotifs);
        AdminUI.styleTable(tableNotifs, AdminUI.VIOLET);
        tableNotifs.setRowHeight(40);

        // Colonne jours colorée
        tableNotifs.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                setFont(new Font("Consolas", Font.BOLD, 13));
                setBorder(new EmptyBorder(0, 10, 0, 10));
                int j = 99;
                try { j = Integer.parseInt(v.toString()); } catch (Exception ignored) {}
                Color fg = j <= 3 ? AdminUI.RED : j <= 7 ? AdminUI.GOLD : AdminUI.GREEN;
                setForeground(fg);
                setBackground(sel ? AdminUI.alpha(AdminUI.VIOLET, 40)
                        : row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setText(j + " j");
                return this;
            }
        });

        // Colonne formule colorée
        tableNotifs.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                setBorder(new EmptyBorder(0, 10, 0, 10));
                String f = v != null ? v.toString() : "";
                setForeground(switch (f) {
                    case "1 AN"   -> AdminUI.VIOLET;
                    case "3 MOIS" -> AdminUI.CYAN;
                    case "2 MOIS" -> AdminUI.BLUE;
                    default       -> AdminUI.TEAL;
                });
                setBackground(sel ? AdminUI.alpha(AdminUI.VIOLET, 40)
                        : row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                return this;
            }
        });

        // Bouton notifier dans la table
        tableNotifs.getColumnModel().getColumn(6).setCellRenderer(
                (t, v, s, f, r, c) -> AdminUI.outlineButton("Notifier", AdminUI.VIOLET, null));
        tableNotifs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (tableNotifs.columnAtPoint(e.getPoint()) == 6) {
                    int row = tableNotifs.rowAtPoint(e.getPoint());
                    if (row >= 0) envoyerNotification(row);
                }
            }
        });

        // Boutons bas
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btns.setOpaque(false);
        btns.add(AdminUI.outlineButton("Modifier contact", AdminUI.TEAL,   e -> modifierContact()));
        btns.add(AdminUI.outlineButton("Notifier tous",    AdminUI.VIOLET, e -> notifierTous()));
        btns.add(AdminUI.outlineButton("Export PDF",       AdminUI.GOLD,   e -> exporterNotifsPDF()));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(AdminUI.scrollTable(tableNotifs), BorderLayout.CENTER);
        center.add(btns, BorderLayout.SOUTH);

        p.add(topBar,  BorderLayout.NORTH);
        p.add(center,  BorderLayout.CENTER);
        return p;
    }

    // ── Onglet 2 : Rapport journalier ─────────────────────────────────────────
    private JPanel buildTabRapport() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Barre outils
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        topBar.setOpaque(false);

        JLabel lblDate = new JLabel("Date :");
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDate.setForeground(AdminUI.TEXT_SEC);

        JTextField fDate = styledField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        fDate.setPreferredSize(new Dimension(150, 36));

        JButton btnGenerer  = AdminUI.outlineButton("Générer",      AdminUI.GOLD,   null);
        JButton btnImprimer = AdminUI.outlineButton("Imprimer PDF", AdminUI.VIOLET, null);
        btnGenerer.setPreferredSize(new Dimension(100, 36));
        btnImprimer.setPreferredSize(new Dimension(130, 36));

        btnGenerer.addActionListener(e -> {
            rapportArea.setText(genererTexteRapport(fDate.getText().trim()));
            sauvegarderRapportDB(fDate.getText().trim());
        });
        btnImprimer.addActionListener(e -> imprimerRapportPDF());

        topBar.add(lblDate); topBar.add(fDate);
        topBar.add(btnGenerer); topBar.add(btnImprimer);

        // Zone texte rapport
        rapportArea = new JTextArea();
        rapportArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        rapportArea.setBackground(AdminUI.SURFACE);
        rapportArea.setForeground(AdminUI.TEXT);
        rapportArea.setCaretColor(AdminUI.CYAN);
        rapportArea.setEditable(false);
        rapportArea.setLineWrap(false);
        rapportArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        rapportArea.setText(genererTexteRapport(
                new SimpleDateFormat("yyyy-MM-dd").format(new Date())));

        JScrollPane sp = new JScrollPane(rapportArea) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AdminUI.SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sp.setBorder(BorderFactory.createLineBorder(AdminUI.alpha(AdminUI.CYAN, 40)));
        sp.getViewport().setBackground(AdminUI.SURFACE);

        p.add(topBar, BorderLayout.NORTH);
        p.add(sp,     BorderLayout.CENTER);
        return p;
    }

    // ── Onglet 3 : Paramètres ─────────────────────────────────────────────────
    private JPanel buildTabParametres() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);

        // Carte centrale glass
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setPaint(new GradientPaint(0, 0, AdminUI.alpha(AdminUI.VIOLET, 14),
                        0, getHeight()/2f, AdminUI.alpha(AdminUI.VIOLET, 0)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight()/2+8, 16, 16);
                g2.setColor(AdminUI.alpha(AdminUI.VIOLET, 45));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 10, 8, 10);
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.gridwidth = 2;

        // Titre section
        JLabel title = new JLabel("Configuration des notifications automatiques");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(AdminUI.VIOLET);
        gc.gridx = 0; gc.gridy = 0; card.add(title, gc);

        JLabel sub = new JLabel("Définissez le seuil d'alerte et les canaux de notification simulés.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(AdminUI.TEXT_SEC);
        gc.gridy = 1; card.add(sub, gc);

        // Séparateur
        JSeparator sep = new JSeparator();
        sep.setForeground(AdminUI.alpha(AdminUI.VIOLET, 40));
        sep.setBackground(AdminUI.alpha(AdminUI.VIOLET, 40));
        gc.gridy = 2; card.add(sep, gc);

        // Checkboxes
        JCheckBox chkAuto   = styledCheck("Activer les notifications automatiques");
        JCheckBox chkEmail  = styledCheck("Simulation Email (log console)"); chkEmail.setSelected(true);
        JCheckBox chkSms    = styledCheck("Simulation SMS (log console)");
        JCheckBox chkReport = styledCheck("Générer rapport automatique à minuit");

        gc.gridy = 3; card.add(chkAuto,   gc);
        gc.gridy = 4; card.add(chkEmail,  gc);
        gc.gridy = 5; card.add(chkSms,    gc);
        gc.gridy = 6; card.add(chkReport, gc);

        // Spinner jours
        gc.gridwidth = 1; gc.gridy = 7;
        JLabel lblJ = new JLabel("Jours avant expiration :");
        lblJ.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblJ.setForeground(AdminUI.TEXT_SEC);
        gc.gridx = 0; card.add(lblJ, gc);

        JSpinner sJours = new JSpinner(new SpinnerNumberModel(7, 1, 30, 1));
        sJours.setBackground(AdminUI.SURFACE3);
        sJours.setForeground(AdminUI.TEXT);
        sJours.setPreferredSize(new Dimension(100, 34));
        gc.gridx = 1; card.add(sJours, gc);

        // Bouton sauvegarder
        gc.gridwidth = 2; gc.gridy = 8; gc.gridx = 0;
        JButton btnSave = AdminUI.createButton("Sauvegarder", AdminUI.TEAL, 160, 38);
        btnSave.addActionListener(e -> NotificationManager.show(
                parentFrame, "Préférences sauvegardées.", NotificationManager.Type.SUCCESS));
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(btnSave);
        card.add(btnWrap, gc);

        outer.add(card);
        return outer;
    }

    // ── Données ───────────────────────────────────────────────────────────────
    private void chargerAbonnesExpirants(int jours) {
        modelNotifs.setRowCount(0);
        int count = 0;
        String sql = "SELECT a.matricule, a.nom_client, a.montant, a.date_fin, " +
                "DATEDIFF(a.date_fin, NOW()) as jr, " +
                "COALESCE(a.email,'') as email, COALESCE(a.telephone,'') as tel " +
                "FROM abonnements a WHERE DATEDIFF(a.date_fin, NOW()) BETWEEN 0 AND ? ORDER BY jr ASC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, jours);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                double prix = rs.getDouble("montant");
                String formule = prix >= 1500 ? "1 AN" : prix >= 400 ? "3 MOIS"
                        : prix >= 280  ? "2 MOIS" : "1 MOIS";
                String email = rs.getString("email");
                String tel   = rs.getString("tel");
                String contact = (email.isEmpty() ? "—" : email)
                        + (tel.isEmpty() ? "" : " / " + tel);
                modelNotifs.addRow(new Object[]{
                        rs.getString("matricule"),
                        rs.getString("nom_client"),
                        formule,
                        new SimpleDateFormat("dd/MM/yyyy").format(rs.getDate("date_fin")),
                        rs.getInt("jr"),
                        contact,
                        "Notifier"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        final int fc = count;
        lblStats.setText(fc + " abonné(s) expirant dans " + jours + " jours");
        lblStats.setForeground(fc == 0 ? AdminUI.GREEN : fc <= 3 ? AdminUI.RED : AdminUI.GOLD);
    }

    private String genererTexteRapport(String date) {
        String sep  = "═".repeat(56);
        String sep2 = "─".repeat(56);
        StringBuilder sb = new StringBuilder();
        sb.append(sep).append("\n");
        sb.append("       SMART PARKING SYSTEM — RAPPORT JOURNALIER     \n");
        sb.append(sep).append("\n");
        sb.append("  Date        : ").append(date).append("\n");
        sb.append("  Généré le   : ")
                .append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())).append("\n");
        sb.append(sep2).append("\n");
        try (Connection c = Database.getConnection()) {
            // Activité
            ResultSet r1 = c.createStatement().executeQuery(
                    "SELECT COUNT(*) as nb, COALESCE(SUM(montant_paye),0) as total, " +
                            "COALESCE(AVG(montant_paye),0) as moy, COALESCE(AVG(duree_minutes),0) as duree " +
                            "FROM historique_paiements WHERE DATE(date_sortie)='" + date + "'");
            if (r1.next()) {
                sb.append("\n  ACTIVITÉ DU JOUR\n");
                sb.append("  · Véhicules traités   : ").append(r1.getInt("nb")).append("\n");
                sb.append("  · Revenus totaux      : ")
                        .append(String.format("%.2f DH", r1.getDouble("total"))).append("\n");
                sb.append("  · Revenu moyen/veh    : ")
                        .append(String.format("%.2f DH", r1.getDouble("moy"))).append("\n");
                sb.append("  · Durée moy. statiomt : ")
                        .append(String.format("%.0f min", r1.getDouble("duree"))).append("\n");
            }
            // Places
            ResultSet r2 = c.createStatement().executeQuery(
                    "SELECT SUM(CASE WHEN est_disponible=1 THEN 1 ELSE 0 END) as lib, COUNT(*) as tot FROM places");
            if (r2.next()) {
                int lib = r2.getInt("lib"), tot = r2.getInt("tot"), occ = tot - lib;
                sb.append("\n  ÉTAT DES PLACES\n");
                sb.append("  · Total / Libres / Occupées : ")
                        .append(tot).append(" / ").append(lib).append(" / ").append(occ).append("\n");
                sb.append("  · Taux d'occupation   : ")
                        .append(tot > 0 ? String.format("%.1f%%", occ * 100.0 / tot) : "0%").append("\n");
            }
            // Abonnements
            ResultSet r3 = c.createStatement().executeQuery(
                    "SELECT COUNT(*) as actifs FROM abonnements WHERE date_fin>=NOW()");
            ResultSet r4 = c.createStatement().executeQuery(
                    "SELECT COUNT(*) as expire FROM abonnements WHERE DATEDIFF(date_fin,NOW()) BETWEEN 0 AND 7");
            sb.append("\n  ABONNEMENTS\n");
            if (r3.next()) sb.append("  · Actifs              : ").append(r3.getInt("actifs")).append("\n");
            if (r4.next()) sb.append("  · Expirent sous 7j    : ").append(r4.getInt("expire")).append("\n");
            // Alertes
            try {
                ResultSet r5 = c.createStatement().executeQuery(
                        "SELECT COUNT(*) FROM alertes WHERE resolue=0");
                if (r5.next()) {
                    sb.append("\n  ALERTES\n");
                    sb.append("  · Non résolues        : ").append(r5.getInt(1)).append("\n");
                }
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            sb.append("\n  Erreur DB : ").append(e.getMessage()).append("\n");
        }
        sb.append("\n").append(sep).append("\n");
        sb.append("             Smart Parking System v1.0              \n");
        sb.append(sep).append("\n");
        return sb.toString();
    }

    private void sauvegarderRapportDB(String date) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO rapports_journaliers (date_rapport) VALUES (?) " +
                             "ON DUPLICATE KEY UPDATE genere_le=NOW()")) {
            ps.setString(1, date); ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ── Export PDF rapport ────────────────────────────────────────────────────
    private void imprimerRapportPDF() {
        if (rapportArea == null || rapportArea.getText().isEmpty()) {
            NotificationManager.show(parentFrame, "Générez d'abord le rapport.", NotificationManager.Type.WARNING);
            return;
        }
        final String[] lines = rapportArea.getText().split("\n");
        final String exportDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .format(LocalDateTime.now());

        final Color BG_DOC  = new Color(8,  10, 18);
        final Color TITLE_C = new Color(180, 80, 220);
        final Color TEXT_C  = new Color(220, 228, 255);
        final Color SEC_C   = new Color(0,  210, 255);
        final Color MUTED   = new Color(110, 125, 165);

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Rapport Journalier Smart Parking");

        PageFormat pf = job.defaultPage();
        pf.setOrientation(PageFormat.PORTRAIT);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            double pw = pageFormat.getImageableWidth();
            double ph = pageFormat.getImageableHeight();
            int    ML = 40, MT = 40;

            // Fond sombre
            g2.setColor(BG_DOC);
            g2.fillRect(0, 0, (int)pw, (int)ph);

            // Bande décorative gauche
            g2.setPaint(new GradientPaint(0, MT, TITLE_C, 0, (int)ph-40, AdminUI.alpha(TITLE_C, 40)));
            g2.fillRect(ML-6, MT, 3, (int)ph - MT*2);

            // Titre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g2.setColor(TITLE_C);
            g2.drawString("SMART PARKING", ML + 8, MT + 22);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(MUTED);
            g2.drawString("Rapport Journalier  —  Exporté le : " + exportDate, ML + 8, MT + 38);

            // Ligne séparatrice
            g2.setColor(AdminUI.alpha(TITLE_C, 60));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawLine(ML, MT + 46, (int)pw - ML, MT + 46);

            // Contenu ligne par ligne
            int y = MT + 66;
            for (String line : lines) {
                if (y > ph - 40) break;
                // Détecter type de ligne
                if (line.startsWith("═") || line.startsWith("─")) {
                    g2.setColor(AdminUI.alpha(TITLE_C, 40));
                    g2.setStroke(new BasicStroke(0.6f));
                    g2.drawLine(ML, y - 4, (int)pw - ML, y - 4);
                    y += 6;
                    continue;
                }
                if (line.trim().startsWith("SMART PARKING") || line.trim().startsWith("Smart Parking")) {
                    // Skip — déjà affiché en titre
                    continue;
                }
                // Section header (ex: "  ACTIVITÉ DU JOUR")
                if (line.trim().length() > 0 && !line.trim().startsWith("·")
                        && !line.trim().startsWith("Date") && !line.trim().startsWith("Généré")
                        && line.trim().equals(line.trim().toUpperCase())
                        && line.trim().length() > 3) {
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    g2.setColor(SEC_C);
                    g2.drawString(line, ML + 8, y);
                    y += 18;
                    continue;
                }
                // Ligne de données
                if (line.trim().startsWith("·")) {
                    // Clé
                    int colon = line.indexOf(":");
                    if (colon > 0) {
                        String key = line.substring(0, colon + 1);
                        String val = line.substring(colon + 1);
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                        g2.setColor(MUTED);
                        g2.drawString(key, ML + 8, y);
                        g2.setFont(new Font("Consolas", Font.BOLD, 10));
                        g2.setColor(TEXT_C);
                        g2.drawString(val, ML + 8 + g2.getFontMetrics(
                                new Font("Segoe UI", Font.PLAIN, 10)).stringWidth(key) + 4, y);
                    } else {
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                        g2.setColor(TEXT_C);
                        g2.drawString(line, ML + 8, y);
                    }
                    y += 16;
                    continue;
                }
                // Ligne normale
                if (!line.trim().isEmpty()) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    g2.setColor(AdminUI.alpha(TEXT_C, 140));
                    g2.drawString(line, ML + 8, y);
                    y += 14;
                } else {
                    y += 8;
                }
            }

            // Pied de page
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(MUTED);
            g2.drawString("Smart Parking System  —  " + exportDate, ML, (int)ph - 20);
            g2.setColor(AdminUI.alpha(TITLE_C, 50));
            g2.setStroke(new BasicStroke(0.6f));
            g2.drawLine(ML, (int)ph - 30, (int)pw - ML, (int)ph - 30);

            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog()) {
            try {
                job.print();
                NotificationManager.show(parentFrame, "Rapport imprimé !", NotificationManager.Type.SUCCESS);
            } catch (PrinterException e) {
                NotificationManager.show(parentFrame, "Erreur : " + e.getMessage(), NotificationManager.Type.ERROR);
            }
        }
    }

    // ── Export PDF liste notifications ────────────────────────────────────────
    private void exporterNotifsPDF() {
        if (modelNotifs.getRowCount() == 0) {
            NotificationManager.show(parentFrame, "Aucun abonné à exporter.", NotificationManager.Type.WARNING);
            return;
        }

        final Color HDR_BG  = new Color(20, 18, 40);
        final Color HDR_FG  = new Color(180, 80, 220);
        final Color ROW_ODD = new Color(18, 22, 38);
        final Color ROW_EVN = new Color(24, 30, 50);
        final Color ROW_FG  = new Color(220, 228, 255);
        final Color BORDER  = new Color(50, 35, 80);

        final String[] COLS   = {"Matricule", "Nom", "Formule", "Expire le", "Jours", "Contact"};
        final int[]    WIDTHS = {90, 130, 70, 90, 50, 170};
        final int ROW_H=22, HDR_H=26, ML=36, MT=70, MB=46;

        int rowCount = modelNotifs.getRowCount();
        Object[][] data = new Object[rowCount][6];
        for (int r=0; r<rowCount; r++)
            for (int c=0; c<6; c++) data[r][c] = modelNotifs.getValueAt(r, c);

        int twTmp = 0; for (int w : WIDTHS) twTmp += w;
        final int tableW    = twTmp;
        final String expDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Notifications Smart Parking");

        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        paper.setSize(841.89, 595.28);
        paper.setImageableArea(0, 0, 841.89, 595.28);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.LANDSCAPE);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            double pageH  = pageFormat.getImageableHeight();
            int rowsPerPg = Math.max(1, ((int)(pageH - MT - MB) - HDR_H - 28) / ROW_H);
            int totalPg   = (int) Math.ceil((double) rowCount / rowsPerPg);
            if (totalPg == 0) totalPg = 1;
            if (pageIndex >= totalPg) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            if (pageIndex == 0) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                g2.setColor(HDR_FG);
                g2.drawString("SMART PARKING — Abonnements expirants", ML, 32);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(110, 125, 165));
                g2.drawString("Exporté le : " + expDate + "   |   " + rowCount + " abonné(s)",
                        ML, 48);
                g2.setColor(HDR_FG); g2.setStroke(new BasicStroke(0.8f));
                g2.drawLine(ML, 54, ML + tableW, 54);
            }

            // Header
            int y = MT;
            g2.setColor(HDR_BG);  g2.fillRect(ML, y, tableW, HDR_H);
            g2.setColor(HDR_FG);  g2.setStroke(new BasicStroke(1f));
            g2.drawRect(ML, y, tableW, HDR_H);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            int x = ML;
            for (int c=0; c<COLS.length; c++) {
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (WIDTHS[c] - fm.stringWidth(COLS[c])) / 2;
                g2.setColor(HDR_FG); g2.drawString(COLS[c], tx, y + HDR_H - 8);
                if (c < COLS.length-1) { g2.setColor(BORDER); g2.drawLine(x+WIDTHS[c], y, x+WIDTHS[c], y+HDR_H); }
                x += WIDTHS[c];
            }
            y += HDR_H;

            // Lignes
            int startRow = pageIndex * rowsPerPg;
            int endRow   = Math.min(startRow + rowsPerPg, rowCount);
            for (int r=startRow; r<endRow; r++) {
                g2.setColor(r%2==0 ? ROW_ODD : ROW_EVN);
                g2.fillRect(ML, y, tableW, ROW_H);
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(0.4f));
                g2.drawLine(ML, y+ROW_H, ML+tableW, y+ROW_H);
                x = ML;
                for (int c=0; c<6; c++) {
                    String val = data[r][c] != null ? data[r][c].toString() : "";
                    g2.setFont(new Font(c==4?"Consolas":"Segoe UI", c==4?Font.BOLD:Font.PLAIN, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    while (fm.stringWidth(val) > WIDTHS[c]-8 && val.length()>3)
                        val = val.substring(0, val.length()-4) + "…";
                    // Couleur jours restants
                    if (c == 4) {
                        int j = 99; try { j = Integer.parseInt(data[r][4].toString()); } catch (Exception ig) {}
                        g2.setColor(j<=3 ? new Color(255,65,90) : j<=7 ? new Color(255,190,50) : new Color(0,220,130));
                    } else {
                        g2.setColor(ROW_FG);
                    }
                    int tx = (c==0||c==2||c==3||c==4) ? x+(WIDTHS[c]-fm.stringWidth(val))/2 : x+6;
                    g2.drawString(val, tx, y+ROW_H-7);
                    if (c<5) { g2.setColor(BORDER); g2.drawLine(x+WIDTHS[c], y, x+WIDTHS[c], y+ROW_H); }
                    x += WIDTHS[c];
                }
                y += ROW_H;
            }

            g2.setColor(HDR_FG); g2.setStroke(new BasicStroke(1f));
            g2.drawRect(ML, MT, tableW, HDR_H + (endRow-startRow)*ROW_H);

            // Pied de page
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(110, 125, 165));
            g2.drawString("Smart Parking System  —  Page "+(pageIndex+1)+"/"+totalPg,
                    ML, (int)(pageH-18));
            g2.drawString(expDate, ML+tableW-80, (int)(pageH-18));
            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog()) {
            try { job.print(); NotificationManager.show(parentFrame, "PDF exporté !", NotificationManager.Type.SUCCESS); }
            catch (PrinterException e) { NotificationManager.show(parentFrame, "Erreur : " + e.getMessage(), NotificationManager.Type.ERROR); }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void envoyerNotification(int row) {
        String mat    = modelNotifs.getValueAt(row, 0).toString();
        String nom    = modelNotifs.getValueAt(row, 1).toString();
        String expire = modelNotifs.getValueAt(row, 3).toString();
        int jr        = Integer.parseInt(modelNotifs.getValueAt(row, 4).toString());
        String msg = "[SIMULATION] → " + nom + " (" + mat + ")\nAbonnement expire le " + expire
                + " (dans " + jr + " jour(s)).";
        System.out.println(msg);
        enregistrerNotifDB(mat, nom,
                modelNotifs.getValueAt(row, 5).toString(),
                "Votre abonnement expire le " + expire + " (dans " + jr + " jour(s)).", expire);
        NotificationManager.show(parentFrame, "Notification envoyée à " + nom, NotificationManager.Type.SUCCESS);
    }

    private void notifierTous() {
        int count = modelNotifs.getRowCount();
        if (count == 0) { NotificationManager.show(parentFrame, "Aucun abonné à notifier.", NotificationManager.Type.INFO); return; }
        for (int i=0; i<count; i++)
            enregistrerNotifDB(
                    modelNotifs.getValueAt(i, 0).toString(),
                    modelNotifs.getValueAt(i, 1).toString(),
                    modelNotifs.getValueAt(i, 5).toString(),
                    "Votre abonnement expire le " + modelNotifs.getValueAt(i, 3) + ".",
                    modelNotifs.getValueAt(i, 3).toString());
        NotificationManager.show(parentFrame, count + " notifications envoyées.", NotificationManager.Type.SUCCESS);
    }

    private void modifierContact() {
        int row = tableNotifs.getSelectedRow();
        if (row < 0) { NotificationManager.show(parentFrame, "Sélectionnez un abonné.", NotificationManager.Type.WARNING); return; }
        String mat     = modelNotifs.getValueAt(row, 0).toString();
        String contact = modelNotifs.getValueAt(row, 5).toString();
        JTextField fEmail = styledField(contact.contains("@") ? contact.split(" / ")[0] : "");
        JTextField fTel   = styledField(contact.contains("/") ? contact.split(" / ")[1] : "");
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 12));
        form.setBackground(AdminUI.BG);
        JLabel l1 = new JLabel("Email :"); l1.setForeground(AdminUI.TEXT_SEC); form.add(l1); form.add(fEmail);
        JLabel l2 = new JLabel("Tél :");   l2.setForeground(AdminUI.TEXT_SEC); form.add(l2); form.add(fTel);
        if (JOptionPane.showConfirmDialog(this, form, "Modifier contact : " + mat,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE abonnements SET email=?, telephone=? WHERE matricule=? AND date_fin>=NOW()")) {
                ps.setString(1, fEmail.getText().trim().isEmpty() ? null : fEmail.getText().trim());
                ps.setString(2, fTel.getText().trim().isEmpty()   ? null : fTel.getText().trim());
                ps.setString(3, mat); ps.executeUpdate();
                NotificationManager.show(parentFrame, "Contact mis à jour.", NotificationManager.Type.SUCCESS);
                chargerAbonnesExpirants(7);
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void enregistrerNotifDB(String mat, String nom, String contact, String msg, String dateExp) {
        String sql = "INSERT INTO notifications_abonnements " +
                "(matricule,nom_client,email,telephone,message,statut,date_expiration,date_envoi) " +
                "VALUES (?,?,?,?,?,'ENVOYE',?,NOW())";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, mat); ps.setString(2, nom);
            ps.setString(3, contact.contains("@") ? contact : null);
            ps.setString(4, null);
            ps.setString(5, msg); ps.setString(6, dateExp);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ── Utilitaires UI ────────────────────────────────────────────────────────
    private JTextField styledField(String text) {
        JTextField f = new JTextField(text);
        f.setBackground(AdminUI.SURFACE2);
        f.setForeground(AdminUI.TEXT);
        f.setCaretColor(AdminUI.CYAN);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdminUI.alpha(AdminUI.CYAN, 50)),
                new EmptyBorder(6, 10, 6, 10)));
        return f;
    }

    private JCheckBox styledCheck(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setBackground(AdminUI.BG);
        cb.setForeground(AdminUI.TEXT_SEC);
        return cb;
    }
}