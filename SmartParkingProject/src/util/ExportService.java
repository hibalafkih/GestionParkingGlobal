package util;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ExportService {

    /**
     * Exporte les données d'un ResultSet vers un fichier CSV.
     *
     * @param rs Le ResultSet à exporter.
     * @param filePath Le chemin complet du fichier CSV de destination.
     * @return true si l'exportation a réussi, false sinon.
     */
    public static boolean exportToCSV(ResultSet rs, String filePath) {
        if (rs == null) {
            return false;
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Écrire l'en-tête du CSV
            for (int i = 1; i <= columnCount; i++) {
                writer.append(escapeCsv(metaData.getColumnLabel(i)));
                if (i < columnCount) {
                    writer.append(',');
                }
            }
            writer.append('\n');

            // Écrire les lignes de données
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    writer.append(escapeCsv(value != null ? value.toString() : ""));
                    if (i < columnCount) {
                        writer.append(',');
                    }
                }
                writer.append('\n');
            }

            writer.flush();
            return true;

        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escapeCsv(String data) {
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }
}