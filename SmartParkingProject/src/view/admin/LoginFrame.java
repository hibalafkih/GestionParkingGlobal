package view.admin;

import database.Database;
import util.AdminUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;

public class LoginFrame extends JFrame {

    private static final Color BG1        = AdminUI.BG;
    private static final Color BG2        = AdminUI.BG2;
    private static final Color SURFACE    = AdminUI.SURFACE;
    private static final Color CYAN       = AdminUI.CYAN;
    private static final Color VIOLET     = AdminUI.VIOLET;
    private static final Color TEXT       = AdminUI.TEXT;
    private static final Color TEXT_SEC   = AdminUI.TEXT_SEC;
    private static final Color BORDER     = AdminUI.BORDER;
    private static final Color RED_ERR    = AdminUI.RED;

    private JTextField     fLogin;
    private JPasswordField fPassword;
    private JLabel         errLabel;
    private JButton        btnLogin;

    private int   attempts = 0;
    private int[] drag     = {0, 0};
    private float animTick = 0f;
    private int   slideY   = 30;
    private float opacity  = 0f;

    public LoginFrame() {
        setTitle("Smart Parking — Login");
        setSize(460, 620);
        setMinimumSize(new Dimension(400, 540));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        try { setOpacity(0f); } catch (Exception ignored) {}
        buildUI();
        addDragSupport();
        startAnimations();
    }

    private void startAnimations() {
        Timer introTimer = new Timer(14, null);
        introTimer.addActionListener(e -> {
            boolean stop = true;
            if (opacity < 1f) { opacity = Math.min(1f, opacity + 0.05f); try { setOpacity(opacity); } catch (Exception ignored) {} stop = false; }
            if (slideY > 0)   { slideY -= Math.max(1, slideY / 5); placeComponents(); stop = false; }
            if (stop) introTimer.stop();
        });
        introTimer.start();
        new Timer(25, e -> { animTick += 0.025f; repaint(); }).start();
    }

    private void buildUI() {
        JPanel root = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fond dégradé sombre
                g2.setPaint(new GradientPaint(0, 0, BG1, getWidth(), getHeight(), BG2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                // Blobs animés
                paintBlob(g2, -80 + (int)(Math.sin(animTick) * 35), -60 + (int)(Math.cos(animTick * 0.7f) * 25), 380, CYAN, 0.07f);
                paintBlob(g2, getWidth() - 250 + (int)(Math.cos(animTick * 1.1f) * 40), getHeight() - 200 + (int)(Math.sin(animTick * 0.9f) * 30), 420, VIOLET, 0.06f);
                paintBlob(g2, getWidth() / 2 - 100, getHeight() / 2 - 80, 260, new Color(0, 220, 130), 0.035f);
                // Bordure subtile
                g2.setColor(AdminUI.alpha(CYAN, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 28, 28);
                g2.dispose();
            }
            private void paintBlob(Graphics2D g, int x, int y, int r, Color c, float a) {
                RadialGradientPaint rg = new RadialGradientPaint(
                        new Point2D.Float(x + r / 2f, y + r / 2f), r / 2f,
                        new float[]{0f, 1f},
                        new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(a * 255)), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}
                );
                g.setPaint(rg); g.fillOval(x, y, r, r);
            }
        };
        root.setOpaque(false);
        setContentPane(root);

        // Bouton fermer
        JButton btnClose = makeCloseBtn();
        btnClose.addActionListener(e -> System.exit(0));
        root.add(btnClose);

        // Logo
        JPanel logo = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = 72, x = (getWidth() - s) / 2;
                int fy = 8 + (int)(Math.sin(animTick * 1.4f) * 5);
                // Halo
                g2.setColor(AdminUI.alpha(CYAN, 18));
                g2.fillOval(x - 12, fy - 12, s + 24, s + 24);
                // Fond glassmorphism
                g2.setColor(AdminUI.alpha(CYAN, 22));
                g2.fillRoundRect(x, fy, s, s, 22, 22);
                g2.setColor(AdminUI.alpha(CYAN, 90));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, fy, s, s, 22, 22);
                // Lettre P
                g2.setFont(new Font("Segoe UI", Font.BOLD, 38));
                g2.setColor(CYAN);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("P", x + s / 2 - fm.stringWidth("P") / 2, fy + s / 2 + fm.getAscent() / 2 - 4);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        root.add(logo);

        JLabel appName = mkLabel("Smart Parking", 26, Font.BOLD, TEXT);
        appName.setHorizontalAlignment(JLabel.CENTER);
        JLabel appSub = mkLabel("Espace Administration", 12, Font.PLAIN, TEXT_SEC);
        appSub.setHorizontalAlignment(JLabel.CENTER);

        // Carte centrale glassmorphism
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int sh = 12, arc = 28;
                // Ombre portée
                for (int i = sh; i > 0; i--) {
                    int alpha = (int)(8.0 * i / sh);
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.fillRoundRect(sh - i, sh - i + 4, getWidth() - (sh - i) * 2, getHeight() - (sh - i) * 2, arc + i, arc + i);
                }
                // Fond glass
                g2.setColor(AdminUI.alpha(SURFACE, 200));
                g2.fillRoundRect(sh, sh, getWidth() - sh * 2, getHeight() - sh * 2, arc, arc);
                // Reflet léger en haut
                g2.setPaint(new GradientPaint(sh, sh, AdminUI.alpha(CYAN, 12), sh, sh + 60, AdminUI.alpha(CYAN, 0)));
                g2.fillRoundRect(sh, sh, getWidth() - sh * 2, 60, arc, arc);
                // Bordure glass
                g2.setColor(AdminUI.alpha(CYAN, 35));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(sh, sh, getWidth() - sh * 2 - 1, getHeight() - sh * 2 - 1, arc, arc);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // Champs
        JLabel lblLogin = fieldLabel("IDENTIFIANT");
        fLogin = customField("admin");

        JLabel lblPwd = fieldLabel("MOT DE PASSE");
        fPassword = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) { paintCustomField(g, this); super.paintComponent(g); }
        };
        styleField(fPassword);
        fPassword.setEchoChar('●');

        JToggleButton eye = new JToggleButton("○") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? AdminUI.alpha(CYAN, 160) : TEXT_SEC);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                g2.drawString(isSelected() ? "●" : "○", 3, 18);
                g2.dispose();
            }
        };
        eye.setContentAreaFilled(false); eye.setBorderPainted(false); eye.setFocusPainted(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.addActionListener(e -> fPassword.setEchoChar(eye.isSelected() ? (char) 0 : '●'));

        // Badge accès
        JLabel badge = new JLabel("ACCÈS SÉCURISÉ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AdminUI.alpha(CYAN, 15)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(AdminUI.alpha(CYAN, 60)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9)); g2.setColor(AdminUI.alpha(CYAN, 200));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 9));

        errLabel = new JLabel(" ");
        errLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errLabel.setForeground(RED_ERR);
        errLabel.setHorizontalAlignment(JLabel.CENTER);

        // Bouton connexion
        btnLogin = new JButton("Se Connecter") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(AdminUI.alpha(TEXT_SEC, 40)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                } else {
                    boolean ro = getModel().isRollover(), pr = getModel().isPressed();
                    g2.setColor(ro ? AdminUI.alpha(CYAN, 50) : AdminUI.alpha(CYAN, 28));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    if (pr) { g2.setColor(AdminUI.alpha(CYAN, 20)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14); }
                    g2.setColor(ro ? AdminUI.alpha(CYAN, 220) : AdminUI.alpha(CYAN, 140));
                    g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.setColor(isEnabled() ? CYAN : AdminUI.alpha(TEXT_SEC, 120));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btnLogin.setContentAreaFilled(false); btnLogin.setBorderPainted(false); btnLogin.setFocusPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> doLogin());

        JLabel hint = mkLabel("Compte par défaut : admin / admin123", 10, Font.PLAIN, AdminUI.alpha(TEXT_SEC, 130));
        hint.setHorizontalAlignment(JLabel.CENTER);

        KeyAdapter enter = new KeyAdapter() { public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin(); } };
        fLogin.addKeyListener(enter); fPassword.addKeyListener(enter);

        int sh = 12, cw = 380, pad = 30 + sh;
        lblLogin.setBounds(pad, 36 + sh, cw - pad * 2, 14);
        fLogin.setBounds(pad, 54 + sh, cw - pad * 2, 48);
        lblPwd.setBounds(pad, 116 + sh, cw - pad * 2, 14);
        fPassword.setBounds(pad, 134 + sh, cw - pad * 2, 48);
        eye.setBounds(cw - pad - 30, 146 + sh, 24, 26);
        badge.setBounds(pad, 198 + sh, cw - pad * 2, 26);
        errLabel.setBounds(pad, 230 + sh, cw - pad * 2, 18);
        btnLogin.setBounds(pad, 254 + sh, cw - pad * 2, 50);
        hint.setBounds(pad, 316 + sh, cw - pad * 2, 14);

        card.setPreferredSize(new Dimension(cw, 374));
        card.add(lblLogin); card.add(fLogin);
        card.add(lblPwd); card.add(fPassword); card.add(eye);
        card.add(badge); card.add(errLabel); card.add(btnLogin); card.add(hint);

        root.setLayout(null);
        addComponentListener(new ComponentAdapter() { public void componentResized(ComponentEvent e) { placeComponents(); } });
        root.add(logo); root.add(appName); root.add(appSub); root.add(card); root.add(btnClose);
        root.putClientProperty("logo",    logo);
        root.putClientProperty("appName", appName);
        root.putClientProperty("appSub",  appSub);
        root.putClientProperty("card",    card);
        root.putClientProperty("close",   btnClose);
        placeComponents();
    }

    private void placeComponents() {
        int w = getWidth(), cx = w / 2;
        JPanel logo    = (JPanel) ((JPanel) getContentPane()).getClientProperty("logo");
        JLabel an      = (JLabel) ((JPanel) getContentPane()).getClientProperty("appName");
        JLabel as      = (JLabel) ((JPanel) getContentPane()).getClientProperty("appSub");
        JPanel card    = (JPanel) ((JPanel) getContentPane()).getClientProperty("card");
        JButton cl     = (JButton) ((JPanel) getContentPane()).getClientProperty("close");
        if (logo  != null) logo.setBounds(cx - 100, 40 + slideY, 200, 100);
        if (an    != null) an.setBounds(cx - 180, 146 + slideY, 360, 32);
        if (as    != null) as.setBounds(cx - 180, 178 + slideY, 360, 18);
        if (card  != null) card.setBounds(cx - 190, 214 + slideY, 380, 374);
        if (cl    != null) cl.setBounds(w - 42, 12, 28, 28);
    }

    private void doLogin() {
        String login = fLogin.getText().trim();
        String pwd = new String(fPassword.getPassword()).trim();
        if (login.isEmpty() || pwd.isEmpty()) { errLabel.setText("Veuillez remplir tous les champs."); return; }
        btnLogin.setEnabled(false); btnLogin.setText("Connexion...");
        SwingWorker<String, Void> w = new SwingWorker<>() {
            @Override protected String doInBackground() { return checkCredentials(login, pwd); }
            @Override protected void done() {
                try {
                    String role = get();
                    if (role != null) {
                        btnLogin.setText("Bienvenue !");
                        Timer t = new Timer(600, e -> { dispose(); new AdminDashboard(login, role).setVisible(true); });
                        t.setRepeats(false); t.start();
                    } else {
                        attempts++;
                        if (attempts >= 3) {
                            errLabel.setText("Compte bloqué 15s."); btnLogin.setEnabled(false);
                            Timer bt = new Timer(15000, e -> { btnLogin.setEnabled(true); btnLogin.setText("Se Connecter"); attempts = 0; errLabel.setText(" "); });
                            bt.setRepeats(false); bt.start();
                        } else {
                            errLabel.setText("Identifiants incorrects. (" + (3 - attempts) + " essai(s))");
                            btnLogin.setEnabled(true); btnLogin.setText("Se Connecter");
                            fPassword.setText(""); fPassword.requestFocus(); shake();
                        }
                    }
                } catch (Exception ex) { errLabel.setText("Erreur base de données."); btnLogin.setEnabled(true); btnLogin.setText("Se Connecter"); }
            }
        };
        w.execute();
    }

    private String checkCredentials(String login, String pwd) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT role, mot_de_passe, actif FROM operateurs WHERE TRIM(login)=TRIM(?)")) {
            ps.setString(1, login.trim());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("[LOGIN] Aucun utilisateur trouve : " + login);
                return null;
            }
            String dbPwd = rs.getString("mot_de_passe");
            int    actif = rs.getInt("actif");
            System.out.println("[LOGIN] pwd saisi='" + pwd + "' | pwd base='" + dbPwd + "' | actif=" + actif);
            if (!pwd.trim().equals(dbPwd.trim())) {
                System.out.println("[LOGIN] Mot de passe incorrect.");
                return null;
            }
            if (actif != 1) {
                System.out.println("[LOGIN] Compte inactif.");
                return null;
            }
            return rs.getString("role");
        } catch (SQLException e) {
            System.out.println("[LOGIN] Erreur SQL : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void shake() {
        int ox = getX(); int[] d = {10, -10, 8, -8, 5, -5, 2, -2, 0}; int[] i = {0};
        Timer t = new Timer(28, null);
        t.addActionListener(e -> { if (i[0] < d.length) setLocation(ox + d[i[0]++], getY()); else { setLocation(ox, getY()); t.stop(); } });
        t.start();
    }

    private void addDragSupport() {
        addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { drag[0] = e.getX(); drag[1] = e.getY(); } });
        addMouseMotionListener(new MouseMotionAdapter() { public void mouseDragged(MouseEvent e) { setLocation(getX() + e.getX() - drag[0], getY() + e.getY() - drag[1]); } });
    }

    private void paintCustomField(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean focus = c.hasFocus();
        g2.setColor(focus ? AdminUI.alpha(CYAN, 15) : AdminUI.SURFACE2);
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 12, 12);
        g2.setColor(focus ? AdminUI.alpha(CYAN, 140) : AdminUI.alpha(AdminUI.BORDER_A, 120));
        g2.setStroke(new BasicStroke(focus ? 1.5f : 1f));
        g2.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 12, 12);
        g2.dispose();
    }

    private JTextField customField(String val) {
        JTextField tf = new JTextField(val) {
            @Override protected void paintComponent(Graphics g) { paintCustomField(g, this); super.paintComponent(g); }
        };
        styleField(tf); return tf;
    }

    private void styleField(JTextComponent tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tf.setForeground(TEXT); tf.setBackground(new Color(0, 0, 0, 0));
        tf.setCaretColor(CYAN); tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(10, 14, 10, 14));
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { tf.repaint(); }
            public void focusLost(FocusEvent e)   { tf.repaint(); }
        });
    }

    private JLabel fieldLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(AdminUI.alpha(CYAN, 160));
        return l;
    }

    private JLabel mkLabel(String txt, int size, int style, Color c) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(c); return l;
    }

    private JButton makeCloseBtn() {
        return new JButton("×") {
            { setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) { g2.setColor(AdminUI.alpha(RED_ERR, 30)); g2.fillOval(0, 0, 28, 28); }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.setColor(getModel().isRollover() ? RED_ERR : TEXT_SEC);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("×", (28 - fm.stringWidth("×")) / 2, (28 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}