package util;

import javax.swing.*;           // <--- AJOUTER POUR JTable
import java.awt.*;             // <--- AJOUTER POUR Frame
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TicketService {

    /**
     * Génère le texte formaté du ticket de caisse.
     */
    public static String genererContenuTicket(String matricule, double montant) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("========== TICKET DE PARKING ==========\n");
        sb.append("         SMART PARKING SYSTEM          \n");
        sb.append("=======================================\n");
        sb.append("Matricule : ").append(matricule).append("\n");
        sb.append("Date      : ").append(LocalDateTime.now().format(dtf)).append("\n");
        sb.append("---------------------------------------\n");
        sb.append("TOTAL A PAYER : ").append(String.format("%.2f DH", montant)).append("\n");
        sb.append("=======================================\n");
        sb.append("      Merci et bonne route !           \n");
        return sb.toString();
    }

    /**
     * Exporte les données en CSV via le service d'export
     */
    public static boolean exporterCSV(java.sql.ResultSet rs, String path) {
        return ExportService.exportToCSV(rs, path);
    }

    /**
     * Imprime le contenu d'une JTable en PDF
     */
    public static void exporterPDF(JTable table, Frame parent) {
        try {
            boolean complete = table.print(JTable.PrintMode.FIT_WIDTH,
                    new java.text.MessageFormat("Historique des Paiements"),
                    new java.text.MessageFormat("Page {0}"));
            if (complete) {
                NotificationManager.show(parent, "Impression terminée", NotificationManager.Type.SUCCESS);
            }
        } catch (java.awt.print.PrinterException e) {
            e.printStackTrace();
            NotificationManager.show(parent, "Erreur d'impression", NotificationManager.Type.ERROR);
        }
    }

    public static void sauvegarderFichier(String matricule, String contenu) {
        String filename = "ticket_" + matricule + "_" + System.currentTimeMillis() + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(contenu);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}