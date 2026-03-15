package view.admin;

import database.*;
import util.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingMapPanel extends JPanel {
    private static final int SPOT_W = 85, SPOT_H = 65, GAP = 15, COLS = 5, PADDING = 25;
    private List<PlaceInfo> places = new ArrayList<>();

    private static class PlaceInfo {
        int id; String numero; boolean libre; String matricule; String typePlace; String typeVehicule;
        PlaceInfo(int id, String num, boolean libre, String mat, String tp, String tv) {
            this.id = id; this.numero = num; this.libre = libre; this.matricule = mat;
            this.typePlace    = tp != null ? tp.toUpperCase() : "STANDARD";
            this.typeVehicule = tv != null ? tv : "";
        }
    }

    public ParkingMapPanel() { setOpaque(false); refreshData(); }

    public void refreshData() {
        places.clear();
        ResultSet rs = new ParkingDAO().getToutesLesPlaces();
        try {
            while (rs != null && rs.next()) {
                places.add(new PlaceInfo(
                        rs.getInt("id_place"),
                        rs.getString("numero_place"),    // ✅ vrai nom
                        rs.getBoolean("est_disponible"), // ✅ vrai nom
                        rs.getString("immatriculation"),
                        rs.getString("type_place"),
                        rs.getString("type_vehicule")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        int rows = (int) Math.ceil((double) places.size() / COLS);
        setPreferredSize(new Dimension(COLS*(SPOT_W+GAP)+PADDING*2, rows*(SPOT_H+GAP)+PADDING*2+60));
        revalidate(); repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ThemeManager.card2());
        g2.fillRoundRect(5, 5, getWidth()-10, getHeight()-10, 20, 20);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("PLAN TEMPS RÉEL", PADDING, 35);
        drawLegend(g2);
        for (int idx = 0; idx < places.size(); idx++) {
            int col = idx % COLS, row = idx / COLS;
            drawSpot(g2, PADDING + col*(SPOT_W+GAP), PADDING+70+row*(SPOT_H+GAP), places.get(idx));
        }
    }

    private void drawSpot(Graphics2D g2, int x, int y, PlaceInfo p) {
        Color c; String icon = "";
        if (!p.libre) { c = new Color(255,82,82); }
        else switch (p.typePlace) {
            case "HANDICAP":   c = new Color(33,150,243);  icon = "♿"; break;
            case "ELECTRIQUE": c = new Color(156,39,176);  icon = "⚡"; break;
            default:           c = new Color(76,175,80);   break;
        }
        g2.setColor(new Color(0,0,0,50));
        g2.fillRoundRect(x+2, y+2, SPOT_W, SPOT_H, 12, 12);
        g2.setPaint(new GradientPaint(x, y, new Color(c.getRed(),c.getGreen(),c.getBlue(),60),
                x, y+SPOT_H, new Color(c.getRed(),c.getGreen(),c.getBlue(),20)));
        g2.fillRoundRect(x, y, SPOT_W, SPOT_H, 12, 12);
        g2.setColor(c); g2.setStroke(new BasicStroke(p.libre ? 1.5f : 3f));
        g2.drawRoundRect(x, y, SPOT_W, SPOT_H, 12, 12);
        g2.setColor(c); g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString(p.numero, x+8, y+18);
        if (!p.libre) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10)); g2.setColor(Color.WHITE);
            String mat = p.matricule != null ? p.matricule : "---";
            g2.drawString(mat, x+(SPOT_W-g2.getFontMetrics().stringWidth(mat))/2, y+40);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(255,255,255,180));
            String tv = p.typeVehicule.isEmpty() ? "AUTO" : p.typeVehicule;
            g2.drawString(tv, x+(SPOT_W-g2.getFontMetrics().stringWidth(tv))/2, y+55);
        } else {
            if (!icon.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 20));
                g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),150));
                g2.drawString(icon, x+45, y+45);
            } else {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),150));
                g2.drawString("L", x+(SPOT_W-10)/2, y+45);
            }
        }
    }

    private void drawLegend(Graphics2D g2) {
        int lx = PADDING, ly = 55;
        legend(g2, lx,      ly, new Color(76,175,80),  "Libre");
        legend(g2, lx+70,   ly, new Color(255,82,82),  "Occupé");
        legend(g2, lx+150,  ly, new Color(33,150,243), "Handicap");
        legend(g2, lx+240,  ly, new Color(156,39,176), "Électrique");
    }
    private void legend(Graphics2D g2, int x, int y, Color c, String t) {
        g2.setColor(c); g2.fillOval(x, y-8, 8, 8);
        g2.setColor(ThemeManager.textMuted()); g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString(t, x+12, y);
    }
}
