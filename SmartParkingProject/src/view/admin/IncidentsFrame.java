package view.admin;

import database.Database;
import util.AdminUI;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class IncidentsFrame extends AdminUI.AdminDialog {

    private JTable tableIncidents, tablePlaces;
    private DefaultTableModel modelIncidents, modelPlaces;
    private JLabel lblStats;
    private Frame parentFrame;

    public IncidentsFrame(Frame parent) {
        super(parent, "Incidents & Places", 1040, 640);
        this.parentFrame = parent;
        init();
        add(buildFooter(), BorderLayout.SOUTH);
        chargerDonnees();
    }

    @Override
    protected JPanel buildHeader() {
        lblStats = new JLabel("");
        lblStats.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStats.setForeground(AdminUI.ORANGE);
        return AdminUI.buildHeader(
                "Incidents & Places",
                "Signaler  ·  Suivre  ·  Résoudre",
                AdminUI.RED,
                lblStats
        );
    }

    @Override
    protected JComponent buildCenter() {
        JPanel p = new JPanel(new GridLayout(1, 2, 14, 0));
        p.setBackground(AdminUI.BG);
        p.setBorder(new EmptyBorder(16, 16, 0, 16));
        p.add(buildPanelIncidents());
        p.add(buildPanelPlaces());
        return p;
    }

    // À la ligne 51 de IncidentsFrame.java
    @Override
    protected JPanel buildFooter() { // Changé de private à protected
        JPanel f = AdminUI.footerPanel();

        JButton btnHist = AdminUI.createButton("Historique incidents", AdminUI.ORANGE, 180, 36);
        btnHist.addActionListener(e -> afficherHistorique());
        f.add(btnHist);

        JButton btnFermer = AdminUI.createButton("Fermer", AdminUI.TEXT_SEC, 100, 36);
        btnFermer.addActionListener(e -> dispose());
        f.add(btnFermer);

        return f;
    }

    // ── Panel incidents actifs ────────────────────────────────────────────────
    private JPanel buildPanelIncidents() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(AdminUI.BG);
        p.add(AdminUI.sectionLabel("INCIDENTS EN COURS", AdminUI.RED), BorderLayout.NORTH);

        modelIncidents = new DefaultTableModel(
                new String[]{"ID", "Place", "Type", "Description", "Signalé le", ""}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };
        tableIncidents = new JTable(modelIncidents);
        AdminUI.styleTable(tableIncidents, AdminUI.RED);
        tableIncidents.setRowHeight(42);
        tableIncidents.getColumnModel().getColumn(0).setMaxWidth(40);

        // Bouton "Résolu"
        tableIncidents.getColumnModel().getColumn(5).setPreferredWidth(80);
        tableIncidents.getColumnModel().getColumn(5).setCellRenderer((t, v, s, f, r, c) -> {
            return AdminUI.createButton("✓ Résolu", AdminUI.GREEN, 80, 32);
        });

        tableIncidents.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (tableIncidents.columnAtPoint(e.getPoint()) == 5) {
                    int row = tableIncidents.rowAtPoint(e.getPoint());
                    if (row >= 0) resoudreIncident(row);
                }
            }
        });

        // Colonne type colorée
        tableIncidents.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setHorizontalAlignment(CENTER);
                setBorder(new EmptyBorder(0, 6, 0, 6));
                String type = v != null ? v.toString() : "";
                setForeground(switch (type) {
                    case "BLOQUEE"   -> AdminUI.RED;
                    case "PANNE"     -> AdminUI.ORANGE;
                    case "ACCIDENT"  -> new Color(255, 60, 60);
                    case "NETTOYAGE" -> AdminUI.BLUE;
                    default          -> AdminUI.TEXT;
                });
                return this;
            }
        });

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton btnSig = AdminUI.createButton("+ Signaler un incident", AdminUI.RED, 180, 36);
        btnSig.addActionListener(e -> signalerIncident());
        south.add(btnSig, BorderLayout.WEST);

        p.add(AdminUI.scrollTable(tableIncidents), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // ── Panel places ──────────────────────────────────────────────────────────
    private JPanel buildPanelPlaces() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(AdminUI.BG);
        p.add(AdminUI.sectionLabel("ÉTAT DES PLACES", AdminUI.ORANGE), BorderLayout.NORTH);

        modelPlaces = new DefaultTableModel(
                new String[]{"#", "Numéro", "Type", "Statut", "Véhicule", "Incident"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablePlaces = new JTable(modelPlaces);
        AdminUI.styleTable(tablePlaces, AdminUI.ORANGE);
        tablePlaces.setRowHeight(38);
        tablePlaces.getColumnModel().getColumn(0).setMaxWidth(36);

        // Colonne Statut colorée
        tablePlaces.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setHorizontalAlignment(CENTER);
                String val = v != null ? v.toString() : "";
                setForeground(val.equals("Libre") ? AdminUI.TEAL : val.equals("Occupée") ? AdminUI.RED : AdminUI.ORANGE);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                return this;
            }
        });

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton btnAct = AdminUI.createButton("↻ Actualiser", AdminUI.TEXT_SEC, 120, 36);
        btnAct.addActionListener(e -> chargerDonnees());
        south.add(btnAct, BorderLayout.WEST);

        p.add(AdminUI.scrollTable(tablePlaces), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // =========================================================================
    // DONNÉES
    // =========================================================================
    private void chargerDonnees() {
        // Incidents actifs
        modelIncidents.setRowCount(0);
        String sql1 = "SELECT i.id_incident, p.numero_place, i.type_incident, i.description, i.date_signale " +
                "FROM incidents i JOIN places p ON i.id_place=p.id_place WHERE i.resolu=0 ORDER BY i.date_signale DESC";
        try (Connection c = Database.getConnection(); ResultSet rs = c.createStatement().executeQuery(sql1)) {
            while (rs.next()) {
                modelIncidents.addRow(new Object[]{
                        rs.getInt("id_incident"),
                        "P-" + rs.getString("numero_place"),
                        rs.getString("type_incident"),
                        rs.getString("description"),
                        new SimpleDateFormat("dd/MM HH:mm").format(rs.getTimestamp("date_signale")),
                        "Résolu"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Places
        modelPlaces.setRowCount(0);
        String sql2 = "SELECT p.id_place, p.numero_place, p.type_place, p.est_disponible, " +
                "v.immatriculation, " +
                "(SELECT type_incident FROM incidents i WHERE i.id_place=p.id_place AND i.resolu=0 LIMIT 1) as incident " +
                "FROM places p LEFT JOIN vehicules v ON p.id_place=v.id_place ORDER BY CAST(p.numero_place AS UNSIGNED)";
        int row = 0;
        try (Connection c = Database.getConnection(); ResultSet rs = c.createStatement().executeQuery(sql2)) {
            while (rs.next()) {
                row++;
                String incident = rs.getString("incident");
                String statut = incident != null ? "Incident" : (rs.getBoolean("est_disponible") ? "Libre" : "Occupée");
                modelPlaces.addRow(new Object[]{
                        row,
                        rs.getString("numero_place"),
                        rs.getString("type_place"),
                        statut,
                        rs.getString("immatriculation") != null ? rs.getString("immatriculation") : "—",
                        incident != null ? incident : "—"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Stats
        try (Connection c = Database.getConnection(); ResultSet rs = c.createStatement()
                .executeQuery("SELECT COUNT(*) FROM incidents WHERE resolu=0")) {
            if (rs.next()) lblStats.setText(rs.getInt(1) + " incident(s) non résolu(s)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    // Petite méthode utilitaire pour styliser les champs de texte
    private JTextField createStyledField() {
        JTextField f = new JTextField();
        f.setBackground(AdminUI.SURFACE);
        f.setForeground(AdminUI.TEXT);
        f.setCaretColor(AdminUI.TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdminUI.BORDER),
                new EmptyBorder(5, 10, 5, 10)
        ));
        return f;
    }

    private void signalerIncident() {
        JComboBox<String> comboPlace = AdminUI.createCombo(new String[]{});
        java.util.Map<String, Integer> placeMap = new java.util.LinkedHashMap<>();
        try (Connection c = Database.getConnection();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT id_place,numero_place FROM places ORDER BY CAST(numero_place AS UNSIGNED)")) {
            while (rs.next()) {
                String label = "Place " + rs.getString("numero_place");
                placeMap.put(label, rs.getInt("id_place"));
                comboPlace.addItem(label);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        JTextField fDesc    = createStyledField();
        JTextField fSignale = createStyledField();
        JComboBox<String> comboType = AdminUI.createCombo(new String[]{"BLOQUEE", "PANNE", "NETTOYAGE", "ACCIDENT", "AUTRE"});

        // Construction manuelle du formulaire
        JPanel form = new JPanel(new GridLayout(4, 2, 10, 15));
        form.setBackground(AdminUI.BG);

        JLabel l1 = new JLabel("Place :"); l1.setForeground(AdminUI.TEXT_SEC);
        form.add(l1); form.add(comboPlace);

        JLabel l2 = new JLabel("Type :"); l2.setForeground(AdminUI.TEXT_SEC);
        form.add(l2); form.add(comboType);

        JLabel l3 = new JLabel("Description :"); l3.setForeground(AdminUI.TEXT_SEC);
        form.add(l3); form.add(fDesc);

        JLabel l4 = new JLabel("Signalé par :"); l4.setForeground(AdminUI.TEXT_SEC);
        form.add(l4); form.add(fSignale);

        if (JOptionPane.showConfirmDialog(this, form, "Signaler un Incident",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Integer idPlace = placeMap.get(comboPlace.getSelectedItem().toString());
            if (idPlace == null) return;
            String type = comboType.getSelectedItem().toString();
            String desc = fDesc.getText().trim();
            String sig  = fSignale.getText().trim().isEmpty() ? "Admin" : fSignale.getText().trim();
            try (Connection c = Database.getConnection();
                 java.sql.PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO incidents (id_place,type_incident,description,signale_par) VALUES (?,?,?,?)")) {
                ps.setInt(1, idPlace); ps.setString(2, type);
                ps.setString(3, desc.isEmpty() ? type : desc); ps.setString(4, sig);
                ps.executeUpdate();
                if (type.equals("BLOQUEE") || type.equals("ACCIDENT"))
                    c.createStatement().executeUpdate("UPDATE places SET est_disponible=0 WHERE id_place=" + idPlace);
                NotificationManager.show(parentFrame, "Incident signalé.", NotificationManager.Type.WARNING);
                chargerDonnees();
            } catch (SQLException e) {
                NotificationManager.show(parentFrame, "Erreur : " + e.getMessage(), NotificationManager.Type.ERROR);
            }
        }
    }

    private void resoudreIncident(int row) {
        int id = (int) modelIncidents.getValueAt(row, 0);
        String place = modelIncidents.getValueAt(row, 1).toString();
        if (JOptionPane.showConfirmDialog(this,
                "Marquer l'incident de " + place + " comme résolu ?",
                "Confirmer", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection c = Database.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "UPDATE incidents SET resolu=1, date_resolu=NOW() WHERE id_incident=?")) {
            ps.setInt(1, id); ps.executeUpdate();
            ResultSet rs = c.createStatement().executeQuery(
                    "SELECT p.id_place, v.immatriculation FROM incidents i " +
                            "JOIN places p ON i.id_place=p.id_place LEFT JOIN vehicules v ON p.id_place=v.id_place " +
                            "WHERE i.id_incident=" + id);
            if (rs.next() && rs.getString("immatriculation") == null)
                c.createStatement().executeUpdate(
                        "UPDATE places SET est_disponible=1 WHERE id_place=" + rs.getInt("id_place"));
            NotificationManager.show(parentFrame, "Incident résolu.", NotificationManager.Type.SUCCESS);
            chargerDonnees();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void afficherHistorique() {
        JDialog dlg = new JDialog(this, "Historique des Incidents", true);
        dlg.setSize(840, 500); dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(AdminUI.BG);
        dlg.setLayout(new java.awt.BorderLayout());

        DefaultTableModel m = new DefaultTableModel(
                new String[]{"Place", "Type", "Description", "Signalé par", "Date", "Résolu le"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        AdminUI.styleTable(t, AdminUI.ORANGE);

        String sql = "SELECT p.numero_place, i.type_incident, i.description, i.signale_par, " +
                "i.date_signale, i.date_resolu, i.resolu " +
                "FROM incidents i JOIN places p ON i.id_place=p.id_place ORDER BY i.date_signale DESC LIMIT 100";
        try (Connection c = Database.getConnection(); ResultSet rs = c.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                m.addRow(new Object[]{
                        "P-" + rs.getString("numero_place"),
                        rs.getString("type_incident"),
                        rs.getString("description"),
                        rs.getString("signale_par"),
                        new SimpleDateFormat("dd/MM/yyyy HH:mm").format(rs.getTimestamp("date_signale")),
                        rs.getInt("resolu") == 1 ? (rs.getTimestamp("date_resolu") != null ?
                                new SimpleDateFormat("dd/MM HH:mm").format(rs.getTimestamp("date_resolu")) : "Oui") : "En cours"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        dlg.add(AdminUI.scrollTable(t), java.awt.BorderLayout.CENTER);
        JPanel fp = AdminUI.footerPanel();

        JButton btnFermerDlg = AdminUI.createButton("Fermer", AdminUI.TEXT_SEC, 100, 36);
        btnFermerDlg.addActionListener(e -> dlg.dispose());
        fp.add(btnFermerDlg);

        dlg.add(fp, java.awt.BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}