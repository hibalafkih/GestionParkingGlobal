package view.admin;

import database.Database;
import util.AdminUI;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalTime;

public class TarifsFrame extends AdminUI.AdminDialog {

    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> simType;
    private JSpinner simDuree;
    private JLabel simResultat;
    private Frame parentFrame;

    public TarifsFrame(Frame parent) {
        super(parent, "Tarification Spéciale", 1000, 640);
        this.parentFrame = parent;
        init();
        chargerTarifs();
    }

    @Override
    protected JPanel buildHeader() {
        JLabel clock = new JLabel();
        clock.setFont(new Font("Consolas", Font.BOLD, 14));
        clock.setForeground(AdminUI.GOLD);
        new Timer(1000, e -> clock.setText(
                LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        )).start();
        return AdminUI.buildHeader(
                "Tarification Spéciale",
                "Nuit  ·  Weekend  ·  Véhicule  ·  Promotions",
                AdminUI.GOLD,
                clock
        );
    }

    @Override
    protected JComponent buildCenter() {
        JPanel p = new JPanel(new GridLayout(1, 2, 14, 0));
        p.setBackground(AdminUI.BG);
        p.setBorder(new EmptyBorder(16, 16, 0, 16));
        p.add(buildPanelTarifs());
        p.add(buildPanelDroite());
        return p;
    }

    @Override
    protected JPanel buildFooter() {
        JPanel f = AdminUI.footerPanel();
        JButton btnFermer = AdminUI.createButton("Fermer", AdminUI.TEXT_SEC, 100, 36);
        btnFermer.addActionListener(e -> dispose());
        f.add(btnFermer);
        return f;
    }

    // ── Liste des tarifs ──────────────────────────────────────────────────────
    private JPanel buildPanelTarifs() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(AdminUI.BG);
        p.add(AdminUI.sectionLabel("TARIFS CONFIGURÉS", AdminUI.GOLD), BorderLayout.NORTH);

        model = new DefaultTableModel(
                new String[]{"ID", "Nom", "Type", "Horaire/Jours", "Véhicule", "Multiplicateur", "Actif"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        AdminUI.styleTable(table, AdminUI.GOLD);
        table.getColumnModel().getColumn(0).setMinWidth(0); table.getColumnModel().getColumn(0).setMaxWidth(0);

        // Colonne multiplicateur
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setHorizontalAlignment(CENTER);
                setFont(new Font("Consolas", Font.BOLD, 13));
                double d = 1.0;
                try { d = Double.parseDouble(v.toString().replace("x", "")); } catch (Exception ignored) {}
                setForeground(d < 1.0 ? AdminUI.GREEN : d > 1.0 ? AdminUI.ORANGE : AdminUI.TEXT_SEC);
                return this;
            }
        });
        // Colonne type
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setHorizontalAlignment(CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                String type = v != null ? v.toString() : "";
                setForeground(switch (type) {
                    case "NUIT"     -> AdminUI.PURPLE;
                    case "WEEKEND"  -> AdminUI.BLUE;
                    case "VEHICULE" -> AdminUI.GOLD;
                    case "PROMO"    -> AdminUI.GREEN;
                    default         -> AdminUI.TEXT;
                });
                return this;
            }
        });
        // Colonne actif
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setHorizontalAlignment(CENTER);
                boolean actif = v != null && (Boolean) v;
                setText(actif ? "● Actif" : "○ Inactif");
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                setForeground(actif ? AdminUI.TEAL : AdminUI.TEXT_SEC);
                return this;
            }
        });

        JPanel btns = new JPanel(new GridLayout(1, 3, 8, 0));
        btns.setOpaque(false);
        btns.setBorder(new EmptyBorder(8, 0, 0, 0));
        btns.add(AdminUI.outlineButton("+ Ajouter",        AdminUI.TEAL,   e -> ajouterTarif()));
        btns.add(AdminUI.outlineButton("Activer / Désact.", AdminUI.GOLD,  e -> toggleActif()));
        btns.add(AdminUI.outlineButton("Supprimer",        AdminUI.RED,    e -> supprimerTarif()));

        p.add(AdminUI.scrollTable(table), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    // ── Panneau droit : infos + simulateur ───────────────────────────────────
    private JPanel buildPanelDroite() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(AdminUI.BG);
        p.add(buildCardsInfo(), BorderLayout.NORTH);
        p.add(buildSimulateur(), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCardsInfo() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setOpaque(false);
        grid.add(infoCard("NUIT",    "Tarif réduit 22h–6h",       AdminUI.PURPLE, "×0.5"));
        grid.add(infoCard("WEEKEND", "Majoration Sam & Dim",       AdminUI.BLUE,   "×1.2"));
        grid.add(infoCard("VÉHICULE","Selon type (Camion…)",       AdminUI.GOLD,   "×2.0"));
        grid.add(infoCard("PROMO",   "Réduction personnalisée",    AdminUI.GREEN,  "×?"));
        return grid;
    }

    private JPanel infoCard(String type, String desc, Color accent, String mult) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(AdminUI.SURFACE);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(0, 56));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        JLabel t1 = new JLabel(type);
        t1.setFont(new Font("Segoe UI", Font.BOLD, 11)); t1.setForeground(accent);
        JLabel t2 = new JLabel(desc);
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        t2.setForeground(AdminUI.TEXT_SEC);
        left.add(t1); left.add(t2);

        JLabel badge = new JLabel(mult, SwingConstants.CENTER);
        badge.setFont(new Font("Consolas", Font.BOLD, 14));
        badge.setForeground(accent);

        card.add(left, BorderLayout.CENTER);
        card.add(badge, BorderLayout.EAST);
        return card;
    }

    private JPanel buildSimulateur() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(AdminUI.SURFACE);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));
        p.add(AdminUI.sectionLabel("SIMULATEUR DE PRIX", AdminUI.GOLD), BorderLayout.NORTH);

        simType  = AdminUI.createCombo(new String[]{"Voiture", "Moto", "Camion"});
        simDuree = new JSpinner(new SpinnerNumberModel(60, 1, 1440, 15));
        simDuree.setBackground(AdminUI.SURFACE3); simDuree.setForeground(AdminUI.TEXT);

        JComboBox<String> simJour  = AdminUI.createCombo(new String[]{"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"});
        JComboBox<String> simHeure = AdminUI.createCombo(new String[]{"08:00","12:00","14:00","18:00","22:00","00:00","03:00"});

        JPanel form = createLocalFormPanel(new Object[][]{
                {"Type véhicule :", simType},
                {"Durée (min) :", simDuree},
                {"Jour :", simJour},
                {"Heure entrée :", simHeure}
        });

        simResultat = new JLabel("— DH");
        simResultat.setFont(new Font("Consolas", Font.BOLD, 32));
        simResultat.setForeground(AdminUI.GOLD);
        simResultat.setHorizontalAlignment(JLabel.CENTER);

        JButton btnSim = AdminUI.outlineButton("Calculer le tarif", AdminUI.GOLD, e -> {
            double montant = simulerTarif(
                    simType.getSelectedItem().toString(), (int) simDuree.getValue(),
                    simJour.getSelectedItem().toString(), simHeure.getSelectedItem().toString()
            );
            simResultat.setText(String.format("%.2f DH", montant));
        });
        btnSim.setPreferredSize(new Dimension(0, 40));

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(form, BorderLayout.NORTH);
        center.add(simResultat, BorderLayout.CENTER);
        center.add(btnSim, BorderLayout.SOUTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    // ── Méthodes utilitaires locales ─────────────────────────────────────────
    private JPanel createLocalFormPanel(Object[][] elements) {
        JPanel panel = new JPanel(new GridLayout(elements.length, 2, 10, 10));
        panel.setOpaque(false);
        for (Object[] row : elements) {
            JLabel label = new JLabel(row[0].toString());
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setForeground(AdminUI.TEXT_SEC);
            panel.add(label);
            if (row[1] instanceof Component) {
                panel.add((Component) row[1]);
            }
        }
        return panel;
    }

    // Correction ici : Nouvelle méthode locale pour créer les champs de texte
    private JTextField createLocalField(String tooltipText) {
        JTextField field = new JTextField();
        field.setToolTipText(tooltipText);
        field.setBackground(AdminUI.SURFACE3);
        field.setForeground(AdminUI.TEXT);
        field.setCaretColor(AdminUI.TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdminUI.SURFACE2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return field;
    }

    // =========================================================================
    // DONNÉES
    // =========================================================================
    private void chargerTarifs() {
        model.setRowCount(0);
        try (Connection c = Database.getConnection();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM tarifs_speciaux ORDER BY type_tarif,id_tarif")) {
            while (rs.next()) {
                String horaire = "-";
                if (rs.getString("heure_debut") != null)
                    horaire = rs.getString("heure_debut").substring(0, 5) + " – " + rs.getString("heure_fin").substring(0, 5);
                else if (rs.getString("jours") != null)
                    horaire = rs.getString("jours");
                model.addRow(new Object[]{
                        rs.getInt("id_tarif"), rs.getString("nom_tarif"), rs.getString("type_tarif"),
                        horaire, rs.getString("type_vehicule") != null ? rs.getString("type_vehicule") : "—",
                        String.format("%.2fx", rs.getDouble("multiplicateur")), rs.getInt("actif") == 1
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================
    private void ajouterTarif() {
        // Correction ici : Utilisation de notre méthode createLocalField
        JTextField fNom  = createLocalField("Nom du tarif");
        JTextField fHdeb = createLocalField("HH:MM (ex : 22:00)");
        JTextField fHfin = createLocalField("HH:MM (ex : 06:00)");
        JTextField fJours = createLocalField("SAT,SUN");

        JComboBox<String> cType = AdminUI.createCombo(new String[]{"NUIT", "WEEKEND", "VEHICULE", "PROMO"});
        JComboBox<String> cVeh  = AdminUI.createCombo(new String[]{"—", "VOITURE", "MOTO", "CAMION"});

        JSpinner sMult = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 5.0, 0.1));
        sMult.setBackground(AdminUI.SURFACE3); sMult.setForeground(AdminUI.TEXT);

        JPanel form = createLocalFormPanel(new Object[][]{
                {"Nom :", fNom}, {"Type :", cType},
                {"Heure début :", fHdeb}, {"Heure fin :", fHfin},
                {"Jours :", fJours}, {"Type véhicule :", cVeh}, {"Multiplicateur :", sMult}
        });

        if (JOptionPane.showConfirmDialog(this, form, "Nouveau Tarif Spécial",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String nom = fNom.getText().trim();
        if (nom.isEmpty()) { NotificationManager.show(parentFrame, "Nom obligatoire.", NotificationManager.Type.WARNING); return; }
        try (Connection c = Database.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO tarifs_speciaux (nom_tarif,type_tarif,heure_debut,heure_fin,jours,type_vehicule,multiplicateur) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, nom); ps.setString(2, cType.getSelectedItem().toString());
            ps.setString(3, fHdeb.getText().trim().isEmpty() ? null : fHdeb.getText().trim());
            ps.setString(4, fHfin.getText().trim().isEmpty() ? null : fHfin.getText().trim());
            ps.setString(5, fJours.getText().trim().isEmpty() ? null : fJours.getText().trim());
            ps.setString(6, cVeh.getSelectedItem().toString().equals("—") ? null : cVeh.getSelectedItem().toString());
            ps.setDouble(7, (double) sMult.getValue());
            ps.executeUpdate();
            NotificationManager.show(parentFrame, "Tarif ajouté : " + nom, NotificationManager.Type.SUCCESS);
            chargerTarifs();
        } catch (SQLException e) { NotificationManager.show(parentFrame, "Erreur : " + e.getMessage(), NotificationManager.Type.ERROR); }
    }

    private void toggleActif() {
        int row = table.getSelectedRow();
        if (row < 0) { NotificationManager.show(parentFrame, "Sélectionnez un tarif.", NotificationManager.Type.WARNING); return; }
        int id = (int) model.getValueAt(row, 0); boolean actif = (Boolean) model.getValueAt(row, 6);
        try (Connection c = Database.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement("UPDATE tarifs_speciaux SET actif=? WHERE id_tarif=?")) {
            ps.setInt(1, actif ? 0 : 1); ps.setInt(2, id); ps.executeUpdate();
            NotificationManager.show(parentFrame, "Tarif " + (actif ? "désactivé" : "activé") + ".", NotificationManager.Type.INFO);
            chargerTarifs();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void supprimerTarif() {
        int row = table.getSelectedRow();
        if (row < 0) { NotificationManager.show(parentFrame, "Sélectionnez un tarif.", NotificationManager.Type.WARNING); return; }
        String nom = model.getValueAt(row, 1).toString(); int id = (int) model.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Supprimer le tarif \"" + nom + "\" ?",
                "Confirmer", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection c = Database.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement("DELETE FROM tarifs_speciaux WHERE id_tarif=?")) {
            ps.setInt(1, id); ps.executeUpdate();
            NotificationManager.show(parentFrame, "Tarif supprimé.", NotificationManager.Type.SUCCESS);
            chargerTarifs();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private double simulerTarif(String typeV, int dureeMin, String jour, String heure) {
        double tarifBase = typeV.equals("Moto") ? 3.0 : typeV.equals("Camion") ? 10.0 : 5.0;
        double base = (dureeMin / 60.0) * tarifBase;
        double mult = 1.0;
        try (Connection c = Database.getConnection();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM tarifs_speciaux WHERE actif=1")) {
            LocalTime h = LocalTime.parse(heure);
            boolean isWeekend = jour.equals("Samedi") || jour.equals("Dimanche");
            while (rs.next()) {
                String type = rs.getString("type_tarif");
                if (type.equals("NUIT") && rs.getString("heure_debut") != null) {
                    LocalTime hdeb = LocalTime.parse(rs.getString("heure_debut"));
                    LocalTime hfin = LocalTime.parse(rs.getString("heure_fin"));
                    boolean nuit = hdeb.isAfter(hfin)
                            ? (h.isAfter(hdeb) || h.isBefore(hfin))
                            : (h.isAfter(hdeb) && h.isBefore(hfin));
                    if (nuit) mult *= rs.getDouble("multiplicateur");
                }
                if (type.equals("WEEKEND") && isWeekend) mult *= rs.getDouble("multiplicateur");
                if (type.equals("VEHICULE") && rs.getString("type_vehicule") != null &&
                        rs.getString("type_vehicule").equalsIgnoreCase(typeV)) mult *= rs.getDouble("multiplicateur");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Math.round(base * mult * 100.0) / 100.0;
    }

    // Méthode statique appelée par ParkingDAO
    public static double calculerAvecTarifsSpeciaux(double tarifBase, String typeVehicule) {
        double mult = 1.0;
        LocalTime now = LocalTime.now();
        java.time.DayOfWeek dow = java.time.LocalDate.now().getDayOfWeek();
        boolean isWeekend = dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY;
        try (Connection c = Database.getConnection();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM tarifs_speciaux WHERE actif=1")) {
            while (rs.next()) {
                String type = rs.getString("type_tarif");
                if (type.equals("NUIT") && rs.getString("heure_debut") != null) {
                    LocalTime hdeb = LocalTime.parse(rs.getString("heure_debut"));
                    LocalTime hfin = LocalTime.parse(rs.getString("heure_fin"));
                    boolean nuit = hdeb.isAfter(hfin)
                            ? (now.isAfter(hdeb) || now.isBefore(hfin))
                            : (now.isAfter(hdeb) && now.isBefore(hfin));
                    if (nuit) mult *= rs.getDouble("multiplicateur");
                }
                if (type.equals("WEEKEND") && isWeekend) mult *= rs.getDouble("multiplicateur");
                if (type.equals("VEHICULE") && rs.getString("type_vehicule") != null &&
                        rs.getString("type_vehicule").equalsIgnoreCase(typeVehicule)) mult *= rs.getDouble("multiplicateur");
            }
        } catch (Exception e) { /* fallback tarifBase */ }
        return Math.round(tarifBase * mult * 100.0) / 100.0;
    }
}