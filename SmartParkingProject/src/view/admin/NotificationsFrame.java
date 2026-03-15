package view.admin;

import database.Database;
import util.AdminUI;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.print.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationsFrame extends AdminUI.AdminDialog {

    private JTable tableNotifs;
    private DefaultTableModel modelNotifs;
    private JTextArea rapportArea;
    private JLabel lblStats;
    private Frame parentFrame; // Ajout de la référence parent

    public NotificationsFrame(Frame parent) {
        super(parent, "Notifications & Rapports", 980, 640);
        this.parentFrame = parent; // Stockage
        init(); // Appelle automatiquement buildHeader(), buildCenter() et buildFooter()
        chargerAbonnésExpirants(7);
    }

    @Override
    protected JPanel buildHeader() {
        lblStats = new JLabel("");
        lblStats.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblStats.setForeground(AdminUI.GOLD);
        return AdminUI.buildHeader(
                "Notifications & Rapports",
                "Alertes abonnements  ·  Rapport journalier PDF",
                AdminUI.PURPLE,
                lblStats
        );
    }

    @Override
    protected JComponent buildCenter() {
        JTabbedPane tabs = AdminUI.styledTabs();
        tabs.addTab("  Abonnements expirants  ", buildTabNotifications());
        tabs.addTab("  Rapport journalier  ",     buildTabRapport());
        tabs.addTab("  Paramètres  ",             buildTabParametres());
        return tabs;
    }

    @Override
    protected JPanel buildFooter() {
        JPanel f = AdminUI.footerPanel();
        JButton btnFermer = AdminUI.createButton("Fermer", AdminUI.TEXT_SEC, 100, 36);
        btnFermer.addActionListener(e -> dispose());
        f.add(btnFermer);
        return f;
    }

    // ── Onglet 1 : Notifications ──────────────────────────────────────────────
    private JPanel buildTabNotifications() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(AdminUI.BG);
        p.setBorder(new EmptyBorder(14, 14, 0, 14));

        // Barre filtre
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        topBar.setBackground(AdminUI.BG);

        JLabel lblExpire = new JLabel("Expire dans :");
        lblExpire.setForeground(AdminUI.TEXT_SEC);
        topBar.add(lblExpire);

        JComboBox<String> cJours = AdminUI.createCombo(new String[]{"3 jours", "7 jours", "14 jours", "30 jours"});
        cJours.setSelectedIndex(1);
        topBar.add(cJours);

        JButton btnFiltrer = AdminUI.createButton("Filtrer", AdminUI.TEAL, 100, 32);
        btnFiltrer.addActionListener(e -> {
            int j = new int[]{3, 7, 14, 30}[cJours.getSelectedIndex()];
            chargerAbonnésExpirants(j);
        });
        topBar.add(btnFiltrer);
        p.add(topBar, BorderLayout.NORTH);

        modelNotifs = new DefaultTableModel(
                new String[]{"Matricule", "Nom", "Formule", "Expire le", "Jours", "Email", "Tél", "Action"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 7; }
        };
        tableNotifs = new JTable(modelNotifs);
        AdminUI.styleTable(tableNotifs, AdminUI.PURPLE);

        // Colonne Jours colorée
        tableNotifs.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                setBackground(row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                int j = v != null ? Integer.parseInt(v.toString()) : 0;
                setForeground(j <= 3 ? AdminUI.RED : j <= 7 ? AdminUI.GOLD : AdminUI.GREEN);
                setFont(new Font("Consolas", Font.BOLD, 13));
                return this;
            }
        });

        // Bouton notifier
        tableNotifs.getColumnModel().getColumn(7).setCellRenderer((t, v, s, f, r, c) ->
                AdminUI.createButton("Notifier", AdminUI.PURPLE, 80, 32)
        );
        tableNotifs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (tableNotifs.columnAtPoint(e.getPoint()) == 7) {
                    int row = tableNotifs.rowAtPoint(e.getPoint());
                    if (row >= 0) envoyerNotification(row);
                }
            }
        });

        JPanel sp = new JPanel(new BorderLayout());
        sp.setBackground(AdminUI.BG);
        sp.add(AdminUI.scrollTable(tableNotifs), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btns.setBackground(AdminUI.BG);

        JButton btnModif = AdminUI.createButton("Modifier contact", AdminUI.TEAL, 160, 36);
        btnModif.addActionListener(e -> modifierContact());
        btns.add(btnModif);

        JButton btnNotifTous = AdminUI.createButton("Notifier tous", AdminUI.PURPLE, 140, 36);
        btnNotifTous.addActionListener(e -> notifierTous());
        btns.add(btnNotifTous);

        sp.add(btns, BorderLayout.SOUTH);

        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── Onglet 2 : Rapport ────────────────────────────────────────────────────
    private JPanel buildTabRapport() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(AdminUI.BG);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        topBar.setBackground(AdminUI.BG);

        JLabel lblDate = new JLabel("Date du rapport :");
        lblDate.setForeground(AdminUI.TEXT_SEC);
        topBar.add(lblDate);

        JTextField fDate = createStyledField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        fDate.setPreferredSize(new Dimension(140, 36));
        topBar.add(fDate);

        JButton btnGenerer = AdminUI.createButton("Générer", AdminUI.GOLD, 100, 36);
        btnGenerer.addActionListener(e -> {
            rapportArea.setText(genererTexteRapport(fDate.getText().trim()));
            sauvegarderRapportDB(fDate.getText().trim());
        });
        topBar.add(btnGenerer);

        JButton btnImprimer = AdminUI.createButton("Imprimer PDF", AdminUI.PURPLE, 140, 36);
        btnImprimer.addActionListener(e -> imprimerRapport());
        topBar.add(btnImprimer);

        p.add(topBar, BorderLayout.NORTH);

        rapportArea = new JTextArea();
        rapportArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        rapportArea.setBackground(AdminUI.SURFACE);
        rapportArea.setForeground(AdminUI.TEXT);
        rapportArea.setCaretColor(AdminUI.TEAL);
        rapportArea.setEditable(false);
        rapportArea.setBorder(new EmptyBorder(12, 14, 12, 14));
        rapportArea.setText(genererTexteRapport(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));

        JScrollPane sp = new JScrollPane(rapportArea);
        sp.setBorder(BorderFactory.createLineBorder(AdminUI.BORDER));
        sp.getViewport().setBackground(AdminUI.SURFACE);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── Onglet 3 : Paramètres ─────────────────────────────────────────────────
    private JPanel buildTabParametres() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(AdminUI.BG);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 20, 10, 20);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 2;

        JLabel info = new JLabel("<html><div style='color:#e1e8f5'>" +
                "<b>Configuration des notifications automatiques</b><br><br>" +
                "Configurez le nombre de jours avant expiration pour déclencher une alerte.</div></html>");

        JCheckBox chkAuto   = styledCheckbox("Activer les notifications automatiques");
        JCheckBox chkEmail  = styledCheckbox("Simulation Email (log console)"); chkEmail.setSelected(true);
        JCheckBox chkSms    = styledCheckbox("Simulation SMS (log console)");
        JCheckBox chkReport = styledCheckbox("Générer rapport automatique à minuit");

        JSpinner sJours = new JSpinner(new SpinnerNumberModel(7, 1, 30, 1));
        sJours.setBackground(AdminUI.SURFACE3);
        sJours.setForeground(AdminUI.TEXT);

        JLabel lblJours = new JLabel("Jours avant expiration :");
        lblJours.setForeground(AdminUI.TEXT_SEC);

        g.gridx = 0; g.gridy = 0; p.add(info, g);
        g.gridy = 1; p.add(chkAuto, g);
        g.gridwidth = 1;
        g.gridy = 2; g.gridx = 0; p.add(lblJours, g);
        g.gridx = 1; p.add(sJours, g);
        g.gridwidth = 2;
        g.gridy = 3; g.gridx = 0; p.add(chkEmail, g);
        g.gridy = 4; p.add(chkSms, g);
        g.gridy = 5; p.add(chkReport, g);
        g.gridy = 6;

        JButton btnSave = AdminUI.createButton("Sauvegarder", AdminUI.TEAL, 140, 36);
        btnSave.addActionListener(e -> NotificationManager.show(parentFrame, "Préférences sauvegardées.", NotificationManager.Type.SUCCESS));
        p.add(btnSave, g);

        return p;
    }

    private JCheckBox styledCheckbox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setBackground(AdminUI.BG);
        cb.setForeground(AdminUI.TEXT_SEC);
        return cb;
    }

    // Utilitaire pour créer un champ de texte stylisé
    private JTextField createStyledField(String text) {
        JTextField f = new JTextField(text);
        f.setBackground(AdminUI.SURFACE);
        f.setForeground(AdminUI.TEXT);
        f.setCaretColor(AdminUI.TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdminUI.BORDER),
                new EmptyBorder(5, 10, 5, 10)
        ));
        return f;
    }

    // =========================================================================
    // DONNÉES & ACTIONS
    // =========================================================================
    private void chargerAbonnésExpirants(int jours) {
        modelNotifs.setRowCount(0);
        int count = 0;
        String sql = "SELECT a.matricule, a.nom_client, a.montant, a.date_fin, " +
                "DATEDIFF(a.date_fin, NOW()) as jr, " +
                "COALESCE(a.email,'') as email, COALESCE(a.telephone,'') as tel " +
                "FROM abonnements a WHERE DATEDIFF(a.date_fin, NOW()) BETWEEN 0 AND ? ORDER BY jr ASC";
        try (Connection c = Database.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, jours);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                double prix = rs.getDouble("montant");
                String formule = prix >= 1500 ? "1 AN" : prix >= 400 ? "3 MOIS" : prix >= 280 ? "2 MOIS" : "1 MOIS";
                modelNotifs.addRow(new Object[]{
                        rs.getString("matricule"), rs.getString("nom_client"),
                        formule, new SimpleDateFormat("dd/MM/yyyy").format(rs.getDate("date_fin")),
                        rs.getInt("jr"),
                        rs.getString("email").isEmpty() ? "—" : rs.getString("email"),
                        rs.getString("tel").isEmpty()   ? "—" : rs.getString("tel"),
                        "Notifier"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        lblStats.setText(count + " abonné(s) expirant dans " + jours + " jours");
    }

    private String genererTexteRapport(String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════════════════════\n");
        sb.append("       SMART PARKING SYSTEM — RAPPORT JOURNALIER     \n");
        sb.append("══════════════════════════════════════════════════════\n");
        sb.append("  Date        : ").append(date).append("\n");
        sb.append("  Généré le   : ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
        sb.append("──────────────────────────────────────────────────────\n");
        try (Connection c = Database.getConnection()) {
            ResultSet r1 = c.createStatement().executeQuery(
                    "SELECT COUNT(*) as nb, COALESCE(SUM(montant_paye),0) as total " +
                            "FROM historique_paiements WHERE DATE(date_sortie)='" + date + "'");
            if (r1.next()) {
                sb.append("\n  ACTIVITÉ DU JOUR\n");
                sb.append("  · Véhicules traités   : ").append(r1.getInt("nb")).append("\n");
                sb.append("  · Revenus totaux      : ").append(String.format("%.2f DH", r1.getDouble("total"))).append("\n");
            }
            ResultSet r3 = c.createStatement().executeQuery(
                    "SELECT SUM(CASE WHEN est_disponible=1 THEN 1 ELSE 0 END) as lib, COUNT(*) as tot FROM places");
            if (r3.next()) {
                int lib = r3.getInt("lib"), tot = r3.getInt("tot"), occ = tot - lib;
                sb.append("\n  PLACES\n");
                sb.append("  · Total / Libres / Occupées : ").append(tot).append(" / ").append(lib).append(" / ").append(occ).append("\n");
                sb.append("  · Taux occupation     : ").append(tot > 0 ? String.format("%.1f%%", occ * 100.0 / tot) : "0%").append("\n");
            }
            ResultSet r4 = c.createStatement().executeQuery("SELECT COUNT(*) FROM abonnements WHERE date_fin>=NOW()");
            if (r4.next()) { sb.append("\n  ABONNEMENTS\n"); sb.append("  · Actifs              : ").append(r4.getInt(1)).append("\n"); }
            try {
                ResultSet r6 = c.createStatement().executeQuery("SELECT COUNT(*) FROM incidents WHERE resolu=0");
                if (r6.next()) { sb.append("\n  INCIDENTS\n"); sb.append("  · En cours            : ").append(r6.getInt(1)).append("\n"); }
            } catch (SQLException ignored) {}
        } catch (SQLException e) { sb.append("\n  Erreur DB : ").append(e.getMessage()).append("\n"); }
        sb.append("\n══════════════════════════════════════════════════════\n");
        return sb.toString();
    }

    private void sauvegarderRapportDB(String date) {
        try (Connection c = Database.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO rapports_journaliers (date_rapport) VALUES (?) ON DUPLICATE KEY UPDATE genere_le=NOW()")) {
            ps.setString(1, date); ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void imprimerRapport() {
        if (rapportArea.getText().isEmpty()) {
            NotificationManager.show(parentFrame, "Générez d'abord le rapport.", NotificationManager.Type.WARNING);
            return;
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            String[] lines = rapportArea.getText().split("\n");
            Font f = new Font("Courier New", Font.PLAIN, 9);
            g2.setFont(f); FontMetrics fm = g2.getFontMetrics(); int y = fm.getHeight();
            for (String line : lines) { g2.drawString(line, 0, y); y += fm.getHeight(); if (y > pf.getImageableHeight() - 20) break; }
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); NotificationManager.show(parentFrame, "Rapport imprimé.", NotificationManager.Type.SUCCESS); }
            catch (PrinterException e) { NotificationManager.show(parentFrame, "Erreur impression.", NotificationManager.Type.ERROR); }
        }
    }

    private void envoyerNotification(int row) {
        String mat    = modelNotifs.getValueAt(row, 0).toString();
        String nom    = modelNotifs.getValueAt(row, 1).toString();
        String expire = modelNotifs.getValueAt(row, 3).toString();
        int jr        = Integer.parseInt(modelNotifs.getValueAt(row, 4).toString());
        String msg = "[SIMULATION] Notification → " + nom + " (" + mat + ")\n" +
                "Abonnement expire le " + expire + " (dans " + jr + " jour(s)).";
        System.out.println(msg);
        enregistrerNotifDB(mat, nom, modelNotifs.getValueAt(row, 5).toString(),
                modelNotifs.getValueAt(row, 6).toString(),
                "Votre abonnement expire le " + expire + " (dans " + jr + " jour(s)).", expire);
        JTextArea ta = new JTextArea(msg);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setEditable(false); ta.setBackground(AdminUI.SURFACE); ta.setForeground(AdminUI.TEAL);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Notification (simulation)", JOptionPane.PLAIN_MESSAGE);
    }

    private void notifierTous() {
        int count = modelNotifs.getRowCount();
        if (count == 0) { NotificationManager.show(parentFrame, "Aucun abonné à notifier.", NotificationManager.Type.INFO); return; }
        for (int i = 0; i < count; i++) {
            enregistrerNotifDB(
                    modelNotifs.getValueAt(i, 0).toString(), modelNotifs.getValueAt(i, 1).toString(),
                    modelNotifs.getValueAt(i, 5).toString(), modelNotifs.getValueAt(i, 6).toString(),
                    "Votre abonnement expire le " + modelNotifs.getValueAt(i, 3) + ".", modelNotifs.getValueAt(i, 3).toString()
            );
        }
        NotificationManager.show(parentFrame, count + " notifications envoyées (simulation).", NotificationManager.Type.SUCCESS);
    }

    private void modifierContact() {
        int row = tableNotifs.getSelectedRow();
        if (row < 0) { NotificationManager.show(parentFrame, "Sélectionnez un abonné.", NotificationManager.Type.WARNING); return; }

        String mat = modelNotifs.getValueAt(row, 0).toString();
        JTextField fEmail = createStyledField(modelNotifs.getValueAt(row, 5).toString().equals("—") ? "" : modelNotifs.getValueAt(row, 5).toString());
        JTextField fTel   = createStyledField(modelNotifs.getValueAt(row, 6).toString().equals("—") ? "" : modelNotifs.getValueAt(row, 6).toString());

        // Formulaire fait main compatible avec notre thème
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 15));
        form.setBackground(AdminUI.BG);

        JLabel l1 = new JLabel("Email :"); l1.setForeground(AdminUI.TEXT_SEC);
        form.add(l1); form.add(fEmail);

        JLabel l2 = new JLabel("Téléphone :"); l2.setForeground(AdminUI.TEXT_SEC);
        form.add(l2); form.add(fTel);

        if (JOptionPane.showConfirmDialog(this, form, "Modifier contact : " + mat,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try (Connection c = Database.getConnection();
                 java.sql.PreparedStatement ps = c.prepareStatement(
                         "UPDATE abonnements SET email=?, telephone=? WHERE matricule=? AND date_fin>=NOW()")) {
                ps.setString(1, fEmail.getText().trim().isEmpty() ? null : fEmail.getText().trim());
                ps.setString(2, fTel.getText().trim().isEmpty()   ? null : fTel.getText().trim());
                ps.setString(3, mat); ps.executeUpdate();
                NotificationManager.show(parentFrame, "Contact mis à jour.", NotificationManager.Type.SUCCESS);
                chargerAbonnésExpirants(7);
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void enregistrerNotifDB(String mat, String nom, String email, String tel, String msg, String dateExp) {
        String sql = "INSERT INTO notifications_abonnements " +
                "(matricule,nom_client,email,telephone,message,statut,date_expiration,date_envoi) " +
                "VALUES (?,?,?,?,?,'ENVOYE',?,NOW())";
        try (Connection c = Database.getConnection(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, mat); ps.setString(2, nom);
            ps.setString(3, email.equals("—") ? null : email);
            ps.setString(4, tel.equals("—")   ? null : tel);
            ps.setString(5, msg); ps.setString(6, dateExp); ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}