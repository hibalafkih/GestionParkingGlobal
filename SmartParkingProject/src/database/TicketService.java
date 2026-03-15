package database;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TicketService {

    // ── Ticket texte ──────────────────────────────────────────────────────────
    public static String genererContenuTicket(String matricule, double montant) {
        String date   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now());
        String statut = montant == 0.0 ? "ABONNÉ — GRATUIT" : String.format("%.2f DH", montant);
        return "==========================================\n" +
                "        P  SMART PARKING SYSTEM          \n" +
                "==========================================\n" +
                "Date/Heure : " + date + "\n" +
                "Véhicule   : " + matricule + "\n" +
                "------------------------------------------\n" +
                "TOTAL PAYÉ : " + statut + "\n" +
                "------------------------------------------\n" +
                "      Merci de votre confiance !         \n" +
                "==========================================\n";
    }

    public static void sauvegarderFichier(String matricule, String contenu) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(
                "ticket_" + matricule + "_" + System.currentTimeMillis() + ".txt"))) {
            w.write(contenu);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Export CSV ────────────────────────────────────────────────────────────
    public static boolean exporterCSV(java.sql.ResultSet rs, String chemin) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(chemin))) {
            pw.println("ID;Matricule;Durée (min);Montant (DH);Date Sortie");
            while (rs != null && rs.next()) {
                pw.printf("%d;%s;%d;%.2f;%s%n",
                        rs.getInt("id_paiement"),
                        rs.getString("immatriculation"),
                        rs.getInt("duree_minutes"),
                        rs.getDouble("montant_paye"),
                        rs.getTimestamp("date_sortie"));
            }
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── Export PDF — tableau simple multipage ─────────────────────────────────
    public static void exporterPDF(JTable table, Frame parent) {

        // Couleurs
        final Color COL_HEADER_BG  = new Color(20, 30, 55);
        final Color COL_HEADER_FG  = new Color(0, 210, 255);
        final Color COL_ROW_ODD    = new Color(18, 22, 38);
        final Color COL_ROW_EVEN   = new Color(24, 30, 50);
        final Color COL_ROW_FG     = new Color(220, 228, 255);
        final Color COL_MONTANT    = new Color(255, 190, 50);
        final Color COL_BORDER     = new Color(40, 55, 90);
        final Color COL_TITLE      = new Color(0, 210, 255);
        final Color COL_TOTAL_BG   = new Color(12, 18, 35);
        final Color COL_TOTAL_FG   = new Color(255, 190, 50);

        // Colonnes et largeurs (points)
        final String[] COLS   = {"N°", "Matricule", "Durée (min)", "Montant (DH)", "Date Sortie"};
        final int[]    WIDTHS = {32, 110, 80, 90, 140};
        final int      ROW_H  = 22;
        final int      HDR_H  = 26;
        final int      MARGIN_L = 36;
        final int      MARGIN_T = 72;
        final int      MARGIN_B = 50;

        // Collecter les données depuis la table
        int rowCount = table.getRowCount();
        Object[][] data = new Object[rowCount][5];
        double totalGeneral = 0;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < 5; c++)
                data[r][c] = table.getValueAt(r, c);
            try {
                String ms = data[r][3].toString().replace(" DH", "").trim();
                totalGeneral += Double.parseDouble(ms);
            } catch (Exception ignored) {}
        }
        final double total = totalGeneral;
        final String exportDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Historique Smart Parking");

        // Format A4 paysage
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        paper.setSize(841.89, 595.28); // A4 paysage en points
        paper.setImageableArea(0, 0, 841.89, 595.28);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.LANDSCAPE);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            double pageW = pageFormat.getImageableWidth();
            double pageH = pageFormat.getImageableHeight();

            int usableH   = (int)(pageH - MARGIN_T - MARGIN_B);
            int rowsPerPg = Math.max(1, (usableH - HDR_H - 30) / ROW_H);
            int totalPages = (int) Math.ceil((double) rowCount / rowsPerPg);
            if (totalPages == 0) totalPages = 1;
            if (pageIndex >= totalPages) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int tableW = 0; for (int w : WIDTHS) tableW += w;

            // ── Titre ─────────────────────────────────────────────────────
            if (pageIndex == 0) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                g2.setColor(COL_TITLE);
                g2.drawString("SMART PARKING — Historique des Paiements", MARGIN_L, 36);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(110, 125, 165));
                g2.drawString("Exporté le : " + exportDate + "   |   " + rowCount + " enregistrement(s)   |   Total : " + String.format("%.2f DH", total),
                        MARGIN_L, 52);

                // Ligne de séparation sous le titre
                g2.setColor(COL_TITLE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(MARGIN_L, 58, MARGIN_L + tableW, 58);
            }

            // ── En-tête du tableau ─────────────────────────────────────────
            int y = MARGIN_T;
            g2.setColor(COL_HEADER_BG);
            g2.fillRect(MARGIN_L, y, tableW, HDR_H);
            // Bordure header
            g2.setColor(COL_TITLE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(MARGIN_L, y, tableW, HDR_H);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            int x = MARGIN_L;
            for (int c = 0; c < COLS.length; c++) {
                g2.setColor(COL_HEADER_FG);
                FontMetrics fm = g2.getFontMetrics();
                // Centrer le texte dans la cellule
                int tx = x + (WIDTHS[c] - fm.stringWidth(COLS[c])) / 2;
                g2.drawString(COLS[c], tx, y + HDR_H - 8);
                // Séparateur vertical
                if (c < COLS.length - 1) {
                    g2.setColor(COL_BORDER);
                    g2.drawLine(x + WIDTHS[c], y, x + WIDTHS[c], y + HDR_H);
                }
                x += WIDTHS[c];
            }
            y += HDR_H;

            // ── Lignes de données ──────────────────────────────────────────
            int startRow = pageIndex * rowsPerPg;
            int endRow   = Math.min(startRow + rowsPerPg, rowCount);

            for (int r = startRow; r < endRow; r++) {
                Color rowBg = (r % 2 == 0) ? COL_ROW_ODD : COL_ROW_EVEN;
                g2.setColor(rowBg);
                g2.fillRect(MARGIN_L, y, tableW, ROW_H);

                // Bordure bas de ligne
                g2.setColor(COL_BORDER);
                g2.setStroke(new BasicStroke(0.4f));
                g2.drawLine(MARGIN_L, y + ROW_H, MARGIN_L + tableW, y + ROW_H);

                x = MARGIN_L;
                for (int c = 0; c < 5; c++) {
                    String val = data[r][c] != null ? data[r][c].toString() : "";
                    // Tronquer si trop long
                    g2.setFont(new Font(c == 3 ? "Courier New" : "Segoe UI",
                            c == 3 ? Font.BOLD : Font.PLAIN, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    while (fm.stringWidth(val) > WIDTHS[c] - 8 && val.length() > 3)
                        val = val.substring(0, val.length() - 4) + "…";

                    g2.setColor(c == 3 ? COL_MONTANT : COL_ROW_FG);

                    // Alignement : centré pour #, durée, montant ; gauche pour reste
                    int tx;
                    if (c == 0 || c == 2 || c == 3)
                        tx = x + (WIDTHS[c] - fm.stringWidth(val)) / 2;
                    else
                        tx = x + 6;
                    g2.drawString(val, tx, y + ROW_H - 7);

                    // Séparateur vertical
                    if (c < 4) {
                        g2.setColor(COL_BORDER);
                        g2.setStroke(new BasicStroke(0.4f));
                        g2.drawLine(x + WIDTHS[c], y, x + WIDTHS[c], y + ROW_H);
                    }
                    x += WIDTHS[c];
                }
                y += ROW_H;
            }

            // ── Bordure extérieure du tableau ──────────────────────────────
            g2.setColor(COL_TITLE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(MARGIN_L, MARGIN_T, tableW, HDR_H + (endRow - startRow) * ROW_H);

            // ── Ligne TOTAL (dernière page uniquement) ─────────────────────
            if (pageIndex == totalPages - 1) {
                y += 4;
                g2.setColor(COL_TOTAL_BG);
                g2.fillRect(MARGIN_L, y, tableW, 24);
                g2.setColor(new Color(0, 210, 255, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(MARGIN_L, y, tableW, 24);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(COL_TOTAL_FG);
                g2.drawString("TOTAL GÉNÉRAL", MARGIN_L + 8, y + 16);

                String totalStr = String.format("%.2f DH", total);
                FontMetrics fm = g2.getFontMetrics();
                int totalX = MARGIN_L + WIDTHS[0] + WIDTHS[1] + WIDTHS[2]
                        + (WIDTHS[3] - fm.stringWidth(totalStr)) / 2;
                g2.drawString(totalStr, totalX, y + 16);
            }

            // ── Pied de page ───────────────────────────────────────────────
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(110, 125, 165));
            g2.drawString("Smart Parking System  —  Page " + (pageIndex + 1) + "/" + totalPages,
                    MARGIN_L, (int)(pageH - 18));
            g2.drawString(exportDate, MARGIN_L + tableW - 80, (int)(pageH - 18));

            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException e) {
                JOptionPane.showMessageDialog(parent, "Erreur impression : " + e.getMessage());
            }
        }
    }
}