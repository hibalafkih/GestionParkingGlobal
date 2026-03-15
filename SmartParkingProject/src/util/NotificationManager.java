package util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Toast notifications — Glassmorphism Dark
 */
public class NotificationManager {

    public enum Type { SUCCESS, ERROR, WARNING, INFO }

    private static final int WIDTH  = 320;
    private static final int HEIGHT = 68;
    private static final int MARGIN = 16;
    private static int currentY = 0;

    public static void show(Frame parent, String message, Type type) {
        Color accent;
        Color bg = AdminUI.alpha(AdminUI.SURFACE, 230);
        switch (type) {
            case SUCCESS: accent = AdminUI.GREEN;  break;
            case ERROR:   accent = AdminUI.RED;    break;
            case WARNING: accent = AdminUI.ORANGE; break;
            default:      accent = AdminUI.CYAN;   break;
        }
        final Color ac = accent;

        JWindow toast = new JWindow(parent) {
            @Override public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fond glass
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                // Bordure colorée
                g2.setColor(AdminUI.alpha(ac, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 12, 12));
                // Barre gauche néon
                g2.setColor(ac);
                g2.fillRoundRect(0, 0, 4, getHeight(), 6, 6);
                // Point lumineux
                g2.setColor(AdminUI.alpha(ac, 30));
                g2.fillOval(14, (HEIGHT - 22) / 2, 22, 22);
                g2.setColor(ac); g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(14, (HEIGHT - 22) / 2, 22, 22);
                drawIcon(g2, type, ac, 14, (HEIGHT - 22) / 2, 22);
                // Message
                g2.setColor(AdminUI.TEXT);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                FontMetrics fm = g2.getFontMetrics();
                String msg = message;
                while (fm.stringWidth(msg) > WIDTH - 60 && msg.length() > 8)
                    msg = msg.substring(0, msg.length() - 4) + "…";
                g2.drawString(msg, 46, HEIGHT / 2 + fm.getAscent() / 2 - 1);
            }
        };
        toast.setSize(WIDTH, HEIGHT);
        try { toast.setOpacity(0.96f); } catch (Exception ignored) {}
        toast.setBackground(new Color(0, 0, 0, 0));

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        toast.setLocation(screen.width - WIDTH - MARGIN, screen.height - HEIGHT - MARGIN - currentY);
        currentY += HEIGHT + 8;
        toast.setVisible(true);

        Timer timer = new Timer(3200, e -> {
            Timer fade = new Timer(25, null);
            final float[] op = {0.96f};
            fade.addActionListener(ev -> {
                op[0] -= 0.06f;
                if (op[0] <= 0f) { toast.dispose(); currentY = Math.max(0, currentY - HEIGHT - 8); ((Timer) ev.getSource()).stop(); }
                else try { toast.setOpacity(Math.max(0f, op[0])); } catch (Exception ignored) {}
            });
            fade.start();
        });
        timer.setRepeats(false); timer.start();
    }

    private static void drawIcon(Graphics2D g2, Type type, Color color, int x, int y, int size) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = x + size / 2, cy = y + size / 2;
        switch (type) {
            case SUCCESS:
                g2.drawLine(x + 5, cy,     cx - 1, y + size - 6);
                g2.drawLine(cx - 1, y + size - 6, x + size - 4, y + 5);
                break;
            case ERROR:
                g2.drawLine(x + 6, y + 6,  x + size - 6, y + size - 6);
                g2.drawLine(x + size - 6, y + 6, x + 6, y + size - 6);
                break;
            case WARNING:
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx, y + 6, cx, cy + 1);
                g2.fillOval(cx - 2, y + size - 8, 4, 4);
                break;
            default:
                g2.fillOval(cx - 2, y + 6, 4, 4);
                g2.drawLine(cx, cy, cx, y + size - 6);
                break;
        }
    }
}