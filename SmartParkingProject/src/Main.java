import view.admin.LoginFrame;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}