package util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Système de notifications toast flottantes (coin bas-droite)
 */
public class NotificationManager {

    public enum Type { SUCCESS, ERROR, WARNING, INFO }

    private static final int WIDTH  = 320;
    private static final int HEIGHT = 70;
    private static final int MARGIN = 15;
    private static int currentY = 0; // pile de toasts

    public static void show(Frame parent, String message, Type type) {
        Color bg, iconColor;
        switch (type) {
            case SUCCESS: bg = new Color(22, 60, 36);  iconColor = ThemeManager.ACCENT_GREEN;  break;
            case ERROR:   bg = new Color(67, 17, 17);  iconColor = ThemeManager.ACCENT_RED;    break;
            case WARNING: bg = new Color(67, 50, 10);  iconColor = ThemeManager.ACCENT_ORANGE; break;
            default:      bg = new Color(17, 45, 78);  iconColor = ThemeManager.ACCENT_BLUE;   break;
        }

        JWindow toast = new JWindow(parent) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(iconColor);
                g2.setStroke(new BasicStroke(2));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                // Barre colorée à gauche
                g2.fillRoundRect(0, 0, 5, getHeight(), 6, 6);
                // Icône dessinée en Java2D (sans emoji)
                drawNotifIcon(g2, type, iconColor, 12, 22, 26);
                // Message
                g2.setColor(ThemeManager.DARK_TEXT);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                FontMetrics fm = g2.getFontMetrics();
                // Tronquer si trop long
                String msg = message;
                while (fm.stringWidth(msg) > WIDTH - 65 && msg.length() > 10) {
                    msg = msg.substring(0, msg.length() - 4) + "...";
                }
                g2.drawString(msg, 50, 44);
            }
        };

        toast.setSize(WIDTH, HEIGHT);
        toast.setOpacity(0.95f);
        toast.setBackground(new Color(0, 0, 0, 0));

        // Position : coin bas-droite de l'écran
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screen.width  - WIDTH  - MARGIN;
        int y = screen.height - HEIGHT - MARGIN - currentY;
        toast.setLocation(x, y);
        currentY += HEIGHT + 10;
        toast.setVisible(true);

        // Auto-fermeture après 3 secondes avec animation de disparition
        Timer timer = new Timer(3000, e -> {
            Timer fade = new Timer(30, null);
            final float[] opacity = {0.95f};
            fade.addActionListener(ev -> {
                opacity[0] -= 0.05f;
                if (opacity[0] <= 0f) {
                    toast.dispose();
                    currentY = Math.max(0, currentY - HEIGHT - 10);
                    ((Timer) ev.getSource()).stop();
                } else {
                    try { toast.setOpacity(Math.max(0f, opacity[0])); }
                    catch (Exception ignored) {}
                }
            });
            fade.start();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static void drawNotifIcon(Graphics2D g2, Type type, Color color, int x, int y, int size) {
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
        g2.fillOval(x, y, size, size);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawOval(x, y, size, size);
        int cx = x + size/2, cy = y + size/2;
        switch (type) {
            case SUCCESS: // coche
                g2.drawLine(x + 5, cy + 1, cx - 1, y + size - 6);
                g2.drawLine(cx - 1, y + size - 6, x + size - 4, y + 5);
                break;
            case ERROR: // croix
                g2.drawLine(x + 7, y + 7, x + size - 7, y + size - 7);
                g2.drawLine(x + size - 7, y + 7, x + 7, y + size - 7);
                break;
            case WARNING: // point d'exclamation
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx, y + 6, cx, cy + 2);
                g2.fillOval(cx - 2, y + size - 8, 4, 4);
                break;
            default: // i
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.fillOval(cx - 2, y + 6, 4, 4);
                g2.drawLine(cx, cy, cx, y + size - 6);
                break;
        }
    }
}