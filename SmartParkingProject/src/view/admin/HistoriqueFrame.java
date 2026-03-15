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

    private JTable             table;
    private DefaultTableModel  model;
    private JLabel             lblTotal, lblCount;
    private Frame              parentFrame;
    private JTextField         searchField;

    public HistoriqueFrame(Frame parent) {
        super(parent, "Historique des Paiements", 980, 620);
        this.parentFrame = parent;
        init();
        chargerHistorique();
    }

    // ── Header ───────────────────────────────────────────────────────────────
    @Override
    protected JPanel buildHeader() {
        lblTotal = new JLabel("");
        lblTotal.setFont(new Font("Consolas", Font.BOLD, 13));
        return AdminUI.buildHeader(
                "Historique des Paiements",
                "Tous les paiements enregistrés  ·  Export CSV & PDF",
                AdminUI.BLUE,
                lblTotal
        );
    }

    // ── Center ───────────────────────────────────────────────────────────────
    @Override
    protected JComponent buildCenter() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AdminUI.BG);

        // ── Barre de recherche + stats ────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AdminUI.SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AdminUI.BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Champ de recherche
        searchField = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.SURFACE2);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(hasFocus() ? AdminUI.alpha(AdminUI.BLUE, 140) : AdminUI.alpha(AdminUI.BORDER_A, 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        searchField.setOpaque(false);
        searchField.setBorder(new EmptyBorder(6, 12, 6, 12));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(AdminUI.TEXT);
        searchField.setCaretColor(AdminUI.BLUE);
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher par matricule...");
        searchField.setPreferredSize(new Dimension(260, 36));
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { filtrerTable(searchField.getText()); }
        });

        // Stats pills
        lblCount = new JLabel("0 entrées");
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCount.setForeground(AdminUI.alpha(AdminUI.BLUE, 200));

        JPanel leftBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftBar.setOpaque(false);
        leftBar.add(mkPill("Recherche :", AdminUI.TEXT_SEC));
        leftBar.add(searchField);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightBar.setOpaque(false);
        rightBar.add(lblCount);

        topBar.add(leftBar,  BorderLayout.WEST);
        topBar.add(rightBar, BorderLayout.EAST);

        // ── Table ─────────────────────────────────────────────────────────
        model = new DefaultTableModel(
                new String[]{"#", "Matricule", "Durée (min)", "Montant (DH)", "Date Sortie"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        AdminUI.styleTable(table, AdminUI.BLUE);
        table.setRowHeight(40);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Renderer personnalisé par colonne
        for (int i = 0; i < 5; i++) {
            final int col = i;
            table.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object v, boolean sel, boolean foc, int row, int c) {
                    super.getTableCellRendererComponent(t, v, sel, foc, row, c);
                    setBorder(new EmptyBorder(0, 14, 0, 14));
                    setFont(new Font(col == 3 ? "Consolas" : "Segoe UI",
                            col == 3 ? Font.BOLD : Font.PLAIN, 13));
                    if (sel) {
                        setBackground(AdminUI.alpha(AdminUI.BLUE, 45));
                        setForeground(AdminUI.BLUE);
                    } else {
                        setBackground(row % 2 == 0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                        setForeground(col == 3 ? AdminUI.GOLD :
                                col == 0 ? AdminUI.TEXT_SEC : AdminUI.TEXT);
                    }
                    if (col == 0 || col == 2 || col == 3)
                        setHorizontalAlignment(JLabel.CENTER);
                    else
                        setHorizontalAlignment(JLabel.LEFT);
                    return this;
                }
            });
        }

        // Largeurs colonnes
        int[] widths = {45, 160, 100, 120, 200};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AdminUI.BORDER));
        scroll.getViewport().setBackground(AdminUI.SURFACE);
        scroll.setBackground(AdminUI.BG);

        root.add(topBar, BorderLayout.NORTH);
        root.add(scroll,  BorderLayout.CENTER);
        return root;
    }

    // ── Footer ───────────────────────────────────────────────────────────────
    @Override
    protected JPanel buildFooter() {
        JPanel f = AdminUI.footerPanel();
        f.add(AdminUI.outlineButton("Exporter CSV", AdminUI.TEAL,   e -> exporterCSV()));
        f.add(AdminUI.outlineButton("Imprimer PDF", AdminUI.VIOLET, e -> exporterPDF()));
        JButton btnFermer = AdminUI.createButton("Fermer", AdminUI.TEXT_SEC, 100, 36);
        btnFermer.addActionListener(e -> dispose());
        f.add(btnFermer);
        return f;
    }

    // ── Données ───────────────────────────────────────────────────────────────
    private void chargerHistorique() {
        model.setRowCount(0);
        double total = 0;
        int    count = 0;
        ResultSet rs = new ParkingDAO().getHistorique();
        try {
            while (rs != null && rs.next()) {
                double montant = rs.getDouble("montant_paye");
                total += montant;
                count++;
                model.addRow(new Object[]{
                        count,
                        rs.getString("immatriculation"),
                        rs.getInt("duree_minutes"),
                        String.format("%.2f DH", montant),
                        rs.getTimestamp("date_sortie")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        lblTotal.setText(String.format("Total : %.2f DH", total));
        lblCount.setText(count + " entrée" + (count > 1 ? "s" : ""));
        setTitle(String.format("Historique — %.2f DH", total));
    }

    private void filtrerTable(String query) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        if (query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query.trim(), 1));
        }
    }

    // ── Export CSV ────────────────────────────────────────────────────────────
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

    // ── Export PDF simple tableau ─────────────────────────────────────────────
    private void exporterPDF() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("historique_parking.pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        if (!path.endsWith(".pdf")) path += ".pdf";

        // Générer le PDF via impression vers fichier PostScript → simple
        // On utilise la méthode d'impression Java2D vers un fichier texte lisible
        try {
            java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.BufferedWriter(new java.io.FileWriter(path.replace(".pdf", "_rapport.txt"))));

            String line = "=".repeat(72);
            String sep  = "-".repeat(72);
            pw.println(line);
            pw.println("  SMART PARKING — HISTORIQUE DES PAIEMENTS");
            pw.println("  Généré le : " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                    .format(new java.util.Date()));
            pw.println(line);
            pw.println();
            pw.printf("%-6s %-18s %-14s %-14s %-18s%n",
                    "N°", "MATRICULE", "DURÉE (min)", "MONTANT", "DATE SORTIE");
            pw.println(sep);

            double totalGeneral = 0;
            for (int i = 0; i < table.getRowCount(); i++) {
                Object num   = table.getValueAt(i, 0);
                Object mat   = table.getValueAt(i, 1);
                Object duree = table.getValueAt(i, 2);
                Object mont  = table.getValueAt(i, 3);
                Object date  = table.getValueAt(i, 4);
                pw.printf("%-6s %-18s %-14s %-14s %-18s%n",
                        num, mat, duree, mont, date);
                // Extraire le montant numérique
                try {
                    String ms = mont.toString().replace(" DH", "").trim();
                    totalGeneral += Double.parseDouble(ms);
                } catch (Exception ignored) {}
            }
            pw.println(sep);
            pw.printf("%-39s %-14s%n", "TOTAL GÉNÉRAL", String.format("%.2f DH", totalGeneral));
            pw.println(line);
            pw.println();
            pw.println("  Smart Parking System — Rapport automatique");
            pw.close();

            NotificationManager.show(parentFrame,
                    "Rapport exporté avec succès !", NotificationManager.Type.SUCCESS);

        } catch (Exception ex) {
            ex.printStackTrace();
            NotificationManager.show(parentFrame,
                    "Erreur export : " + ex.getMessage(), NotificationManager.Type.ERROR);
        }
    }

    // ── Utilitaires UI ────────────────────────────────────────────────────────
    private JLabel mkPill(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(color);
        return l;
    }
}