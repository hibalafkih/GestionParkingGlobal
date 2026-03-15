package view.admin;

import database.ParkingDAO;
import util.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;

/**
 * Panneau de statistiques avec graphiques intégrés (sans bibliothèque externe).
 */
public class StatsPanel extends JPanel {

    private ParkingDAO dao = new ParkingDAO();

    public StatsPanel() {
        setLayout(new GridLayout(1, 2, 16, 0));
        setOpaque(false);
        refresh();
    }

    public void refresh() {
        removeAll();
        add(new RevenusChart(dao.getRevenus7Jours()));
        add(new HeuresChart(dao.getDistributionHoraire()));
        revalidate();
        repaint();
    }

    // =========================================================================
    // Graphique Revenus 7 jours (courbe)
    // =========================================================================
    static class RevenusChart extends JPanel {
        private List<double[]> data;

        RevenusChart(List<double[]> data) {
            this.data = data;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 180));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 50, padR = 20, padT = 30, padB = 40;

            // Fond carte
            g2.setColor(ThemeManager.card());
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(ThemeManager.border());
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            // Titre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(ThemeManager.textMuted());
            g2.drawString(" Revenus 7 derniers jours", padL, 20);

            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            // Axes
            g2.setColor(ThemeManager.border());
            g2.drawLine(padL, padT, padL, padT + chartH);
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            if (data.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                g2.setColor(ThemeManager.textMuted());
                g2.drawString("Pas de données", padL + chartW / 2 - 40, padT + chartH / 2);
                return;
            }

            double maxVal = data.stream().mapToDouble(d -> d[1]).max().orElse(1.0);
            if (maxVal == 0) maxVal = 1;

            // Grilles horizontales
            g2.setColor(new Color(ThemeManager.border().getRed(),
                    ThemeManager.border().getGreen(),
                    ThemeManager.border().getBlue(), 80));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(ThemeManager.textMuted());
            for (int i = 0; i <= 4; i++) {
                int yLine = padT + (chartH * i / 4);
                g2.drawString(String.format("%.0f", maxVal * (4 - i) / 4), 4, yLine + 4);
            }

            // Remplissage sous la courbe (gradient)
            int n = data.size();
            int[] xs = new int[n];
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = padL + (chartW * i / Math.max(1, n - 1));
                ys[i] = padT + chartH - (int) (chartH * data.get(i)[1] / maxVal);
            }

            // Zone remplie
            GradientPaint gp = new GradientPaint(
                    0, padT, new Color(ThemeManager.ACCENT_BLUE.getRed(), ThemeManager.ACCENT_BLUE.getGreen(), ThemeManager.ACCENT_BLUE.getBlue(), 80),
                    0, padT + chartH, new Color(ThemeManager.ACCENT_BLUE.getRed(), ThemeManager.ACCENT_BLUE.getGreen(), ThemeManager.ACCENT_BLUE.getBlue(), 5)
            );
            Path2D fill = new Path2D.Float();
            fill.moveTo(xs[0], padT + chartH);
            for (int i = 0; i < n; i++) fill.lineTo(xs[i], ys[i]);
            fill.lineTo(xs[n - 1], padT + chartH);
            fill.closePath();
            g2.setPaint(gp);
            g2.fill(fill);

            // Ligne de courbe
            g2.setColor(ThemeManager.ACCENT_BLUE);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D line = new Path2D.Float();
            line.moveTo(xs[0], ys[0]);
            for (int i = 1; i < n; i++) line.lineTo(xs[i], ys[i]);
            g2.draw(line);

            // Points et valeurs
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            for (int i = 0; i < n; i++) {
                g2.setColor(ThemeManager.ACCENT_BLUE);
                g2.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
                g2.setColor(ThemeManager.text());
                String val = String.format("%.0f", data.get(i)[1]);
                g2.drawString(val, xs[i] - g2.getFontMetrics().stringWidth(val) / 2, ys[i] - 8);
                // Labels jours
                g2.setColor(ThemeManager.textMuted());
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((long) data.get(i)[0]);
                String day = String.format("%02d/%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1);
                g2.drawString(day, xs[i] - 12, padT + chartH + 15);
            }
        }
    }

    // =========================================================================
    // Graphique Distribution horaire (barres)
    // =========================================================================
    static class HeuresChart extends JPanel {
        private int[] heures;

        HeuresChart(int[] heures) {
            this.heures = heures;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 180));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 35, padR = 10, padT = 30, padB = 35;

            // Fond carte
            g2.setColor(ThemeManager.card());
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(ThemeManager.border());
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            // Titre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(ThemeManager.textMuted());
            g2.drawString(" Affluence par heure", padL, 20);

            int chartW = w - padL - padR;
            int chartH = h - padT - padB;
            int max = 1;
            for (int v : heures) if (v > max) max = v;

            int barW = Math.max(1, chartW / 24 - 2);

            // Axe
            g2.setColor(ThemeManager.border());
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            // Barres
            for (int i = 0; i < 24; i++) {
                int bh = (int) ((double) heures[i] / max * chartH);
                int bx = padL + i * (chartW / 24) + 1;
                int by = padT + chartH - bh;

                // Couleur selon heure (pic = rouge, normal = cyan)
                float intensity = (float) heures[i] / max;
                Color barColor = interpolateColor(ThemeManager.ACCENT_CYAN, ThemeManager.ACCENT_RED, intensity);

                g2.setColor(new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 180));
                g2.fillRoundRect(bx, by, barW, bh, 3, 3);
                g2.setColor(barColor);
                g2.drawRoundRect(bx, by, barW, bh, 3, 3);

                // Labels h paires
                if (i % 4 == 0) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                    g2.setColor(ThemeManager.textMuted());
                    g2.drawString(String.valueOf(i) + "h", bx - 2, padT + chartH + 12);
                }
            }

            // Légende max
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(ThemeManager.textMuted());
            g2.drawString(String.valueOf(max), 2, padT + 4);
            g2.drawString("0", 8, padT + chartH);
        }

        private Color interpolateColor(Color c1, Color c2, float t) {
            return new Color(
                    (int)(c1.getRed()   + t * (c2.getRed()   - c1.getRed())),
                    (int)(c1.getGreen() + t * (c2.getGreen() - c1.getGreen())),
                    (int)(c1.getBlue()  + t * (c2.getBlue()  - c1.getBlue()))
            );
        }
    }
}