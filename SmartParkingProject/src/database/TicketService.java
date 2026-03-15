package database;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.*;
import java.awt.print.*;
import javax.swing.*;

public class TicketService {

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

    public static boolean exporterCSV(java.sql.ResultSet rs, String chemin) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(chemin))) {
            pw.println("ID;Matricule;Durée (min);Montant (DH);Date Sortie");
            while (rs != null && rs.next()) {
                pw.printf("%d;%s;%d;%.2f;%s%n",
                        rs.getInt("id_paiement"),
                        rs.getString("immatriculation"),
                        rs.getInt("duree_minutes"),
                        rs.getDouble("montant_paye"),
                        rs.getTimestamp("date_sortie")   // ✅ vrai nom colonne
                );
            }
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static void exporterPDF(JTable table, Frame parent) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Historique Parking");
        job.setPrintable((graphics, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.drawString("SMART PARKING — Historique des Paiements", 20, 30);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.drawString("Exporté le : " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()), 20, 50);
            String[] cols = {"ID","Matricule","Durée (min)","Montant (DH)","Date Sortie"};
            int[] widths  = {30, 100, 80, 80, 130};
            int x = 20, y = 75;
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            for (int i = 0; i < cols.length; i++) { g2.drawString(cols[i], x, y); x += widths[i]; }
            g2.drawLine(20, y+5, 450, y+5); y += 20;
            g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
            for (int row = 0; row < table.getRowCount(); row++) {
                x = 20;
                for (int col = 0; col < table.getColumnCount(); col++) {
                    Object val = table.getValueAt(row, col);
                    g2.drawString(val != null ? val.toString() : "", x, y);
                    x += widths[col];
                }
                y += 15;
                if (y > pf.getImageableHeight() - 30) break;
            }
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException e) { JOptionPane.showMessageDialog(parent, "Erreur : " + e.getMessage()); }
        }
    }
}
