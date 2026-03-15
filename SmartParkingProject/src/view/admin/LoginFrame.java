package view.admin;

import database.Database;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;

public class LoginFrame extends JFrame {

    // Palette Mode Clair & Vert Forêt
    private static final Color BG_MAIN       = new Color(250, 252, 250);
    private static final Color BG_CARD       = new Color(255, 255, 255);
    private static final Color FOREST_GREEN  = new Color(34, 139, 34);
    private static final Color FOREST_LIGHT  = new Color(50, 168, 82);
    private static final Color TEXT_MAIN     = new Color(30, 40, 35);
    private static final Color TEXT_MUTED    = new Color(120, 135, 125);
    private static final Color BORDER        = new Color(225, 235, 230);
    private static final Color FIELD_BG      = new Color(245, 248, 246);
    private static final Color RED_ERROR     = new Color(220, 53, 69);

    private JTextField     fLogin;
    private JPasswordField fPassword;
    private JLabel         errLabel;
    private JButton        btnLogin;

    // Variables pour l'animation
    private int   attempts   = 0;
    private int[] drag       = {0, 0};
    private float animTick   = 0f;
    private int   slideY     = 40;
    private float opacity    = 0f;

    public LoginFrame() {
        setTitle("Smart Parking — Login");
        setSize(460, 640);
        setMinimumSize(new Dimension(400, 540));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setBackground(new Color(0,0,0,0));

        try { setOpacity(0f); } catch (Exception ignored) {}

        buildUI();
        addDragSupport();
        startAnimations();
    }

    private void startAnimations() {
        Timer introTimer = new Timer(15, null);
        introTimer.addActionListener(e -> {
            boolean stop = true;
            if (opacity < 1f) {
                opacity = Math.min(1f, opacity + 0.04f);
                try { setOpacity(opacity); } catch (Exception ignored) {}
                stop = false;
            }
            if (slideY > 0) {
                slideY -= Math.max(1, slideY / 6);
                placeComponents();
                stop = false;
            }
            if (stop) introTimer.stop();
        });
        introTimer.start();

        Timer bgTimer = new Timer(30, e -> {
            animTick += 0.04f;
            repaint();
        });
        bgTimer.start();
    }

    private void buildUI() {
        JPanel root = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(BG_MAIN);
                g2.fillRoundRect(0,0,getWidth(),getHeight(), 30, 30);

                int mX1 = (int)(Math.sin(animTick) * 40);
                int mY1 = (int)(Math.cos(animTick * 0.8) * 30);
                int mX2 = (int)(Math.cos(animTick * 1.2) * 50);
                int mY2 = (int)(Math.sin(animTick * 0.9) * 40);

                drawBlob(g2, -100 + mX1, -50 + mY1, 400, FOREST_GREEN, 0.05f);
                drawBlob(g2, getWidth()-250 + mX2, getHeight()-200 + mY2, 450, FOREST_LIGHT, 0.04f);

                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1, 30, 30);

                g2.dispose();
            }
            private void drawBlob(Graphics2D g, int x, int y, int r, Color c, float a) {
                RadialGradientPaint rg = new RadialGradientPaint(
                        new Point2D.Float(x+r/2f, y+r/2f), r/2f,
                        new float[]{0f, 1f},
                        new Color[]{new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(a*255)), new Color(c.getRed(),c.getGreen(),c.getBlue(),0)}
                );
                g.setPaint(rg); g.fillOval(x,y,r,r);
            }
        };
        root.setOpaque(false);
        setContentPane(root);

        JButton btnClose = roundBtn("×", RED_ERROR, 30);
        btnClose.addActionListener(e -> System.exit(0));
        root.add(btnClose);

        JPanel logo = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = 76, x = (getWidth()-s)/2;
                int floatY = 8 + (int)(Math.sin(animTick * 1.5) * 6);

                g2.setColor(new Color(0,0,0,15));
                g2.fillRoundRect(x+2, floatY+6, s, s, 25, 25);

                g2.setPaint(new GradientPaint(x, floatY, FOREST_LIGHT, x, floatY+s, FOREST_GREEN));
                g2.fillRoundRect(x, floatY, s, s, 25, 25);

                g2.setPaint(new GradientPaint(x, floatY, new Color(255,255,255,50), x, floatY+s/2f, new Color(255,255,255,0)));
                g2.fillRoundRect(x, floatY, s, s/2, 25, 25);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("P", x+s/2-fm.stringWidth("P")/2, floatY+s/2+fm.getAscent()/2-4);
                g2.dispose();
            }
        };
        logo.setOpaque(false); logo.setPreferredSize(new Dimension(200, 110));
        root.add(logo);

        JLabel appName = styledLabel("Smart Parking", 28, Font.BOLD, TEXT_MAIN);
        appName.setHorizontalAlignment(JLabel.CENTER);
        JLabel appSub  = styledLabel("Espace Administration", 13, Font.PLAIN, TEXT_MUTED);
        appSub.setHorizontalAlignment(JLabel.CENTER);

        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 32;
                int shadowSpread = 14;

                for (int i = 0; i < shadowSpread; i++) {
                    int alpha = 10 - (i * 10 / shadowSpread);
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.fillRoundRect(shadowSpread - i, shadowSpread - i + 5,
                            getWidth() - shadowSpread*2 + i*2,
                            getHeight() - shadowSpread*2 + i*2,
                            arc + i, arc + i);
                }

                g2.setColor(BG_CARD);
                g2.fillRoundRect(shadowSpread, shadowSpread, getWidth()-shadowSpread*2, getHeight()-shadowSpread*2, arc, arc);

                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(shadowSpread, shadowSpread, getWidth()-shadowSpread*2-1, getHeight()-shadowSpread*2-1, arc, arc);

                g2.dispose();
            }
        };
        card.setOpaque(false);

        JLabel lblLogin = fieldLabel("IDENTIFIANT");
        fLogin = customField("admin");

        JLabel lblPwd = fieldLabel("MOT DE PASSE");
        fPassword = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                paintCustomField(g, this); super.paintComponent(g);
            }
        };
        styleField(fPassword);
        fPassword.setEchoChar('●');

        JToggleButton eye = new JToggleButton("○") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? FOREST_GREEN : TEXT_MUTED);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,16));
                g2.drawString(isSelected()?"●":"○", 3, 18);
                g2.dispose();
            }
        };
        eye.setContentAreaFilled(false); eye.setBorderPainted(false); eye.setFocusPainted(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.addActionListener(e -> fPassword.setEchoChar(eye.isSelected()?(char)0:'●'));

        JLabel badge = new JLabel("ACCÈS SÉCURISÉ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(FOREST_GREEN.getRed(),FOREST_GREEN.getGreen(),FOREST_GREEN.getBlue(),20));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.setFont(getFont()); g2.setColor(FOREST_GREEN);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        badge.setFont(new Font("Segoe UI",Font.BOLD,10));

        errLabel = new JLabel(" ");
        errLabel.setFont(new Font("Segoe UI",Font.PLAIN,12));
        errLabel.setForeground(RED_ERROR); errLabel.setHorizontalAlignment(JLabel.CENTER);

        btnLogin = new JButton("Se Connecter") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(200,210,205));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                } else {
                    g2.setPaint(getModel().isRollover()
                            ? new GradientPaint(0,0,FOREST_LIGHT,getWidth(),0,FOREST_GREEN)
                            : new GradientPaint(0,0,FOREST_GREEN,getWidth(),0,new Color(28, 115, 28)));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);

                    if (getModel().isPressed()) {
                        g2.setColor(new Color(0,0,0,30));
                        g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                    }
                }
                g2.setFont(new Font("Segoe UI",Font.BOLD,14));
                g2.setColor(Color.WHITE);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btnLogin.setContentAreaFilled(false); btnLogin.setBorderPainted(false); btnLogin.setFocusPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> doLogin());

        JLabel hint = styledLabel("Compte par défaut : admin / admin123", 11, Font.PLAIN, TEXT_MUTED);
        hint.setHorizontalAlignment(JLabel.CENTER);

        KeyAdapter enter = new KeyAdapter(){public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER)doLogin();}};
        fLogin.addKeyListener(enter); fPassword.addKeyListener(enter);

        int shadow = 14;
        int cw = 380, pad = 30 + shadow;

        lblLogin.setBounds(pad, 36 + shadow, cw-pad*2, 16);
        fLogin.setBounds(pad, 56 + shadow, cw-pad*2, 50);

        lblPwd.setBounds(pad, 122 + shadow, cw-pad*2, 16);
        fPassword.setBounds(pad, 142 + shadow, cw-pad*2, 50);
        eye.setBounds(cw-pad-32, 154 + shadow, 24, 26);

        badge.setBounds(pad, 212 + shadow, cw-pad*2, 28);
        errLabel.setBounds(pad, 246 + shadow, cw-pad*2, 20);
        btnLogin.setBounds(pad, 272 + shadow, cw-pad*2, 52);
        hint.setBounds(pad, 336 + shadow, cw-pad*2, 16);

        card.setPreferredSize(new Dimension(cw, 400));
        card.add(lblLogin); card.add(fLogin);
        card.add(lblPwd);   card.add(fPassword); card.add(eye);
        card.add(badge);    card.add(errLabel);  card.add(btnLogin); card.add(hint);

        root.setLayout(null);
        addComponentListener(new ComponentAdapter(){
            public void componentResized(ComponentEvent e){ placeComponents(); }
        });

        root.add(logo); root.add(appName); root.add(appSub); root.add(card); root.add(btnClose);

        root.putClientProperty("logo",    logo);
        root.putClientProperty("appName", appName);
        root.putClientProperty("appSub",  appSub);
        root.putClientProperty("card",    card);
        root.putClientProperty("close",   btnClose);

        placeComponents();
    }

    private void placeComponents() {
        int w=getWidth(), cx=w/2;
        JPanel logo=(JPanel)((JPanel)getContentPane()).getClientProperty("logo");
        if(logo!=null) logo.setBounds(cx-100, 45 + slideY, 200, 110);
        JLabel an=(JLabel)((JPanel)getContentPane()).getClientProperty("appName");
        if(an!=null) an.setBounds(cx-180, 160 + slideY, 360, 34);
        JLabel as=(JLabel)((JPanel)getContentPane()).getClientProperty("appSub");
        if(as!=null) as.setBounds(cx-180, 195 + slideY, 360, 20);
        JPanel card=(JPanel)((JPanel)getContentPane()).getClientProperty("card");
        if(card!=null) card.setBounds(cx-190, 235 + slideY, 380, 400);
        JButton cl=(JButton)((JPanel)getContentPane()).getClientProperty("close");
        if(cl!=null) cl.setBounds(w-44, 14, 30, 30);
    }

    // =========================================================================
    // LA CORRECTION EST ICI :
    // =========================================================================
    private void doLogin() {
        String login = fLogin.getText().trim();
        String pwd   = new String(fPassword.getPassword()).trim();
        if (login.isEmpty()||pwd.isEmpty()) { errLabel.setText("Veuillez remplir tous les champs."); return; }

        btnLogin.setEnabled(false); btnLogin.setText("Connexion...");

        SwingWorker<String,Void> w = new SwingWorker<>(){
            @Override protected String doInBackground(){ return checkCredentials(login,pwd); }
            @Override protected void done(){
                try {
                    String role=get();
                    if(role!=null){
                        btnLogin.setText("Bienvenue !");

                        // ✅ CORRECTION : On demande au Timer de ne s'exécuter qu'UNE SEULE fois
                        Timer delayTimer = new Timer(700, e -> {
                            dispose();
                            new AdminDashboard(login,role).setVisible(true);
                        });
                        delayTimer.setRepeats(false); // <--- C'est cette ligne qui sauve votre écran !
                        delayTimer.start();

                    } else {
                        attempts++;
                        if(attempts>=3){
                            errLabel.setText("Compte bloqué 15s.");
                            btnLogin.setEnabled(false);
                            Timer blockTimer = new Timer(15000, e -> {
                                btnLogin.setEnabled(true);
                                btnLogin.setText("Se Connecter");
                                attempts=0;
                                errLabel.setText(" ");
                            });
                            blockTimer.setRepeats(false); // ✅ Corrigé ici aussi par précaution
                            blockTimer.start();
                        } else {
                            errLabel.setText("Identifiants incorrects. ("+(3-attempts)+" essai(s))");
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Se Connecter");
                            fPassword.setText("");
                            fPassword.requestFocus();
                            shake();
                        }
                    }
                } catch(Exception ex){ errLabel.setText("Erreur base de données."); btnLogin.setEnabled(true); btnLogin.setText("Se Connecter"); }
            }
        };
        w.execute();
    }

    private String checkCredentials(String login, String pwd) {
        try(Connection c=Database.getConnection(); PreparedStatement ps=c.prepareStatement("SELECT role FROM operateurs WHERE login=? AND mot_de_passe=? AND actif=1")){
            ps.setString(1,login); ps.setString(2,pwd); ResultSet rs=ps.executeQuery();
            return rs.next()?rs.getString("role"):null;
        }catch(SQLException e){ e.printStackTrace(); return null; }
    }

    private void shake() {
        int ox=getX(); int[] d={12,-12,9,-9,6,-6,3,-3,0}; int[] i={0};
        Timer t=new Timer(30,null);
        t.addActionListener(e->{ if(i[0]<d.length) setLocation(ox+d[i[0]++],getY()); else{ setLocation(ox,getY()); t.stop(); } });
        t.start(); // Ce timer s'arrête tout seul grâce au "t.stop()" dans la condition
    }

    private void addDragSupport(){
        addMouseListener(new MouseAdapter(){ public void mousePressed(MouseEvent e){ drag[0]=e.getX(); drag[1]=e.getY(); } });
        addMouseMotionListener(new MouseMotionAdapter(){ public void mouseDragged(MouseEvent e){ setLocation(getX()+e.getX()-drag[0],getY()+e.getY()-drag[1]); } });
    }

    private void paintCustomField(Graphics g, JComponent c) {
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        boolean focus = c.hasFocus();
        g2.setColor(focus ? Color.WHITE : FIELD_BG);
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 16, 16);

        g2.setColor(focus ? FOREST_GREEN : BORDER);
        g2.setStroke(new BasicStroke(focus ? 1.5f : 1f));
        g2.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 16, 16);

        if(focus) {
            g2.setColor(new Color(FOREST_GREEN.getRed(), FOREST_GREEN.getGreen(), FOREST_GREEN.getBlue(), 20));
            g2.drawRoundRect(1, 1, c.getWidth()-3, c.getHeight()-3, 14, 14);
        }
        g2.dispose();
    }

    private JTextField customField(String val) {
        JTextField tf = new JTextField(val) {
            @Override protected void paintComponent(Graphics g){ paintCustomField(g,this); super.paintComponent(g); }
        };
        styleField(tf); return tf;
    }

    private void styleField(JTextComponent tf) {
        tf.setFont(new Font("Segoe UI",Font.PLAIN,16));
        tf.setForeground(TEXT_MAIN);
        tf.setBackground(new Color(0,0,0,0));
        tf.setCaretColor(FOREST_GREEN);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(10,16,10,16));
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { tf.repaint(); }
            @Override public void focusLost(FocusEvent e) { tf.repaint(); }
        });
    }

    private JLabel fieldLabel(String txt) {
        JLabel l=new JLabel(txt);
        l.setFont(new Font("Segoe UI",Font.BOLD,11));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JLabel styledLabel(String txt,int size,int style,Color c) {
        JLabel l=new JLabel(txt);
        l.setFont(new Font("Segoe UI",style,size));
        l.setForeground(c);
        return l;
    }

    private JButton roundBtn(String txt, Color c, int size) {
        return new JButton(txt) {
            {
                setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(size, size));
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

                if(getModel().isRollover()) {
                    g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),25));
                    g2.fillOval(0,0,size,size);
                }

                g2.setFont(new Font("Segoe UI",Font.BOLD,18));
                g2.setColor(getModel().isRollover() ? c : TEXT_MUTED);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(size-fm.stringWidth(getText()))/2,(size+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        UIManager.put("defaultFont", new Font("Segoe UI",Font.PLAIN,13));
        SwingUtilities.invokeLater(()->new LoginFrame().setVisible(true));
    }
}