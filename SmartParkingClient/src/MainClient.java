import com.formdev.flatlaf.FlatDarkLaf;
import view.client.BorneClient;
import javax.swing.*;
import java.awt.*;

/**
 * Point d'entree — Projet CLIENT (Borne de parking).
 * Meme base MySQL que SmartParkingProject (Admin).
 * localhost:3306/parking_db
 */
public class MainClient {
    public static void main(String[] args) {
        try { FlatDarkLaf.setup(); } catch (Exception ignored) {}
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        SwingUtilities.invokeLater(() -> new BorneClient().setVisible(true));
    }
}