import com.formdev.flatlaf.FlatDarkLaf;
import view.admin.LoginFrame;
import javax.swing.*;
import java.awt.*;

/**
 * Point d'entree — SmartParkingProject (Admin).
 * Lance l'ecran de login. LoginFrame ouvre AdminDashboard apres authentification.
 */
public class Main {
    public static void main(String[] args) {
        try { FlatDarkLaf.setup(); } catch (Exception ignored) {}
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}