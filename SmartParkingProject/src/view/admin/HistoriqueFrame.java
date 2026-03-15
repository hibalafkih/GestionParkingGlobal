package view.admin;

import database.*;
import util.AdminUI;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.sql.*;

public class HistoriqueFrame extends AdminUI.AdminDialog {

    private JTable table;
    private DefaultTableModel model;
    private JLabel lblTotal;
    private Frame parentFrame; // <-- Ajout de la variable ici !

    public HistoriqueFrame(Frame parent) {
        super(parent, "Historique des Paiements", 860, 560);
        this.parentFrame = parent; // <-- Sauvegarde de la référence ici !
        init();
        chargerHistorique();
    }

    @Override
    protected JPanel buildHeader() {
        lblTotal = new JLabel("");
        lblTotal.setFont(new Font("Consolas", Font.BOLD, 13));
        lblTotal.setForeground(AdminUI.GOLD);
        return AdminUI.buildHeader(
                "Historique",
                "Tous les paiements enregistrés",
                AdminUI.BLUE,
                lblTotal
        );
    }

    @Override
    protected JComponent buildCenter() {
        model = new DefaultTableModel(
                new String[]{"ID", "Matricule", "Durée (min)", "Montant (DH)", "Date Sortie"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        AdminUI.styleTable(table, AdminUI.BLUE);

        // Colonne montant en or
        DefaultTableCellRenderer moneyRenderer = AdminUI.centeredRenderer(AdminUI.BLUE);
        moneyRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Consolas", col == 3 ? Font.BOLD : Font.PLAIN, 12));
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (sel) { setBackground(new Color(99,179,237,18)); setForeground(AdminUI.BLUE); }
                else {
                    setBackground(row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                    setForeground(col == 3 ? AdminUI.GOLD : AdminUI.TEXT);
                }
                return this;
            }
        };
        for (int i = 0; i < 5; i++) table.getColumnModel().getColumn(i).setCellRenderer(moneyRenderer);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(AdminUI.SURFACE);
        p.add(AdminUI.scrollTable(table), BorderLayout.CENTER);
        return p;
    }

    @Override
    protected JPanel buildFooter() {
        JPanel f = AdminUI.footerPanel();

        // Boutons d'export
        f.add(AdminUI.outlineButton("Exporter CSV", AdminUI.TEAL,  e -> exporterCSV()));
        f.add(AdminUI.outlineButton("Imprimer PDF", AdminUI.PURPLE, e -> TicketService.exporterPDF(table, parentFrame)));

        // Bouton Fermer (corrigé avec les bonnes dimensions int)
        JButton btnFermer = AdminUI.createButton("Fermer", AdminUI.TEXT_SEC, 100, 36);
        btnFermer.addActionListener(e -> dispose());
        f.add(btnFermer);

        return f;
    }

    private void chargerHistorique() {
        model.setRowCount(0);
        double total = 0;
        ResultSet rs = new ParkingDAO().getHistorique();
        try {
            while (rs != null && rs.next()) {
                double montant = rs.getDouble("montant_paye");
                total += montant;
                model.addRow(new Object[]{
                        rs.getInt("id_paiement"),
                        rs.getString("immatriculation"),
                        rs.getInt("duree_minutes"),
                        String.format("%.2f DH", montant),
                        rs.getTimestamp("date_sortie")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        lblTotal.setText(String.format("Total : %.2f DH", total));
        setTitle(String.format("Historique — Chiffre d'Affaires : %.2f DH", total));
    }

    private void exporterCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("historique_parking.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            ResultSet rs = new ParkingDAO().getHistorique();
            boolean ok = TicketService.exporterCSV(rs, fc.getSelectedFile().getAbsolutePath());
            NotificationManager.show(parentFrame,
                    ok ? "Export CSV réussi !" : "Erreur lors de l'export.",
                    ok ? NotificationManager.Type.SUCCESS : NotificationManager.Type.ERROR);
        }
    }
}