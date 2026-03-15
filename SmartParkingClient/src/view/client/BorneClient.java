package view.client;

import database.Database;
import database.ParkingDAO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Borne CLIENT — Thème Global : Vert Foncé & Texte Noir (Motion UI)
 */
public class BorneClient extends JFrame {

    // --- NOUVELLE PALETTE DE COULEURS GLOBALE ---
    private static final Color BG_LIGHT   = new Color(244, 247, 250); // Fond gris très clair
    private static final Color BG_DARKER  = new Color(226, 232, 240);
    private static final Color CARD_BG    = new Color(255, 255, 255); // Cartes blanches
    private static final Color PRIMARY    = new Color(20, 120, 60);   // VERT FONCÉ (Icônes et Boutons)
    private static final Color PRIMARY_L  = new Color(34, 160, 82);   // Vert plus clair pour les dégradés
    private static final Color TEXT_DARK  = Color.BLACK;              // TEXTE NOIR ABSOLU
    private static final Color TEXT_MUTED = new Color(80, 80, 80);    // Gris foncé pour les sous-titres
    private static final Color BORDER     = new Color(200, 210, 220); // Bordures grises douces
    private static final Color GREEN      = new Color(16, 185, 129);  // Vert succès (fluo)
    private static final Color RED        = new Color(220, 50, 50);   // Rouge erreur
    private static final Color GOLD       = new Color(240, 160, 0);   // Jaune/Or avertissement

    private final ParkingDAO dao = new ParkingDAO();
    private CardLayout cards;
    private JPanel root;

    // Etat session courante
    private String matricule = "";
    private double montantDu = 0;

    // Labels dynamiques
    private JLabel resTitle, resDetail, resInfo;
    private JLabel payMatLbl, payDureeLbl, payMontantLbl;
    private JLabel cfTitle, cfInfo, cfSub;
    private JLabel abStatusLbl, abDetailLbl;
    private JTextField abMatField;
    private JPanel placesGrid;

    // Moteurs d'animation
    private long startTime;
    private float transitionAlpha = 0f;
    private long pageLoadTime = 0;

    public BorneClient() {
        setTitle("Borne Client — Smart Parking");
        setSize(1000, 740);
        setMinimumSize(new Dimension(860, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { quitter(); }
        });

        startTime = System.currentTimeMillis();
        pageLoadTime = System.currentTimeMillis();

        getContentPane().setBackground(BG_LIGHT);
        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);
        buildPages();

        // Moteur d'animation global
        new Timer(16, e -> {
            if (transitionAlpha > 0) {
                transitionAlpha -= 0.05f;
                if (transitionAlpha < 0) transitionAlpha = 0;
            }
            root.repaint();
        }).start();

        new Timer(20000, e -> { if(placesGrid!=null) refreshPlaces(); }).start();
    }

    // =========================================================================
    // TOPBAR
    // =========================================================================
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(CARD_BG);
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(BORDER);
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
            }
        };
        bar.setPreferredSize(new Dimension(0,75));
        bar.setBorder(new EmptyBorder(0,28,0,28));
        bar.setOpaque(false);

        JLabel logo = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY); // Logo Vert Foncé
                g2.fillRoundRect(0, 10, 42, 42, 18, 18);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
                g2.setColor(Color.WHITE);
                g2.drawString("P", 13, 39);
                g2.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(50,60));

        JPanel lt = new JPanel(new GridLayout(2,1)); lt.setOpaque(false);
        JLabel t1=new JLabel("Smart Parking"); t1.setFont(new Font("Segoe UI",Font.BOLD,18)); t1.setForeground(TEXT_DARK);
        JLabel t2=new JLabel("Borne Libre-Service"); t2.setFont(new Font("Segoe UI",Font.PLAIN,13)); t2.setForeground(TEXT_MUTED);
        lt.add(t1); lt.add(t2);

        JLabel clock=new JLabel();
        clock.setFont(new Font("Segoe UI",Font.BOLD,16)); clock.setForeground(PRIMARY); // Horloge Vert Foncé
        new Timer(1000,e->clock.setText(DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()))).start();
        clock.setText(DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()));

        JButton btnOut=new JButton("Quitter");
        btnOut.setFont(new Font("Segoe UI",Font.BOLD,13)); btnOut.setForeground(TEXT_MUTED);
        btnOut.setContentAreaFilled(false); btnOut.setBorderPainted(false); btnOut.setFocusPainted(false);
        btnOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); btnOut.addActionListener(e->quitter());

        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,14,12)); left.setOpaque(false); left.add(logo); left.add(lt);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,24,24)); right.setOpaque(false); right.add(clock); right.add(btnOut);
        bar.add(left,BorderLayout.WEST); bar.add(right,BorderLayout.EAST);
        return bar;
    }

    // =========================================================================
    // PAGES ET TRANSITIONS AVEC PARALLAXE
    // =========================================================================
    private void buildPages() {
        cards=new CardLayout();
        root=new JPanel(cards) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                float time = (System.currentTimeMillis() - startTime) / 3000f;
                int xOffset = (int)(Math.sin(time) * 150);
                int yOffset = (int)(Math.cos(time) * 100);
                GradientPaint gp = new GradientPaint(xOffset, yOffset, BG_LIGHT, getWidth() + xOffset, getHeight() + yOffset, BG_DARKER);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                Point mousePos = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(mousePos, this);
                float mouseX = (mousePos != null) ? (mousePos.x - getWidth()/2f) * 0.05f : 0;
                float mouseY = (mousePos != null) ? (mousePos.y - getHeight()/2f) * 0.05f : 0;

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g2.setColor(Color.WHITE);
                for (int i = 0; i < 15; i++) {
                    float pTime = (System.currentTimeMillis() - startTime) / (1000f + i*100);
                    int px = (int) ((Math.sin(pTime + i) * 300) + (getWidth() * (i / 15f)) - mouseX * (i%3 + 1));
                    int py = (int) ((Math.cos(pTime*0.8 + i*2) * 200) + (getHeight() * ((i%4)/4f)) - mouseY * (i%3 + 1));
                    int pSize = 30 + (i * 15);
                    g2.fillOval(px, py, pSize, pSize);
                }
                g2.dispose();
            }

            @Override public void paint(Graphics g) {
                super.paint(g);
                if (transitionAlpha > 0) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(new Color(BG_LIGHT.getRed(), BG_LIGHT.getGreen(), BG_LIGHT.getBlue(), (int)(transitionAlpha * 255)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        root.setOpaque(false);
        root.add(pageAccueil(),     "ACCUEIL");
        root.add(pagePlaces(),      "PLACES");
        root.add(pageSaisie(),      "SAISIE");
        root.add(pageResultat(),    "RESULTAT");
        root.add(pagePaiement(),    "PAIEMENT");
        root.add(pagePaiementCB(),  "PAIEMENT_CB");
        root.add(pageAbonnement(),  "ABONNEMENT");
        root.add(pageAssistance(),  "ASSISTANCE");
        root.add(pageConfirm(),     "CONFIRM");
        add(root, BorderLayout.CENTER);
        show("ACCUEIL");
    }

    private void show(String page) {
        transitionAlpha = 1.0f;
        pageLoadTime = System.currentTimeMillis();
        cards.show(root, page);
    }

    // ── Page ACCUEIL ────────────────────────────────────────────────────────
    private JPanel pageAccueil() {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false);
        GridBagConstraints g=gbc();

        JLabel titre = new JLabel("Bienvenue") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float time = (System.currentTimeMillis() - startTime) / 600f;
                int floatY = (int) (Math.sin(time) * 6);
                g2.setFont(new Font("Trebuchet MS", Font.BOLD, 56));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + floatY;

                g2.setColor(new Color(0, 0, 0, 25)); g2.drawString(getText(), x + 4, y + 6);

                // Dégradé Vert Foncé -> Vert Clair
                GradientPaint gp = new GradientPaint(x, y - fm.getAscent(), PRIMARY, x, y, PRIMARY_L);
                g2.setPaint(gp); g2.drawString(getText(), x, y); g2.dispose();
            }
        };
        titre.setPreferredSize(new Dimension(400, 80));

        JLabel sous=new JLabel("Que souhaitez-vous faire aujourd'hui ?"); sous.setFont(new Font("Segoe UI",Font.ITALIC,19)); sous.setForeground(TEXT_MUTED);

        g.gridx=0;g.gridy=0;g.gridwidth=2;g.insets=new Insets(0,0,6,0);  p.add(titre,g);
        g.gridy=1;                          g.insets=new Insets(0,0,45,0); p.add(sous,g);

        // Toutes les cartes utilisent PRIMARY (Vert Foncé)
        JPanel b1=actionBtn("Entrée / Sortie",    "Enregistrer votre véhicule",  PRIMARY,  "CAR", 0);
        JPanel b2=actionBtn("Payer maintenant",   "Régler votre stationnement",  PRIMARY,  "CARD", 1);
        JPanel b3=actionBtn("Places Disponibles", "Voir le plan en temps réel",  PRIMARY,  "PIN", 2);
        JPanel b4=actionBtn("Mon Abonnement",     "Consulter votre statut",      PRIMARY,  "STAR", 3);
        JPanel b5=actionBtn("Appeler Assistance", "Contacter un agent",          PRIMARY,  "SOS", 4);

        b1.addMouseListener(onClick(e->{ setSaisieMode("ES"); show("SAISIE"); }));
        b2.addMouseListener(onClick(e->{ setSaisieMode("PAY"); show("SAISIE"); }));
        b3.addMouseListener(onClick(e->{ refreshPlaces(); show("PLACES"); }));
        b4.addMouseListener(onClick(e->{ resetAbonnement(); show("ABONNEMENT"); }));
        b5.addMouseListener(onClick(e->show("ASSISTANCE")));

        g.gridx=0;g.gridy=2;g.gridwidth=1;g.fill=GridBagConstraints.BOTH;g.weightx=1;
        g.insets=new Insets(0,32,0,16); p.add(b1,g);
        g.gridx=1;g.insets=new Insets(0,16,0,32); p.add(b2,g);
        g.gridx=0;g.gridy=3;g.insets=new Insets(24,32,0,16); p.add(b3,g);
        g.gridx=1;g.insets=new Insets(24,16,0,32); p.add(b4,g);
        g.gridx=0;g.gridy=4;g.gridwidth=2;g.insets=new Insets(24,32,0,32); p.add(b5,g);
        return p;
    }

    // ── Page PLACES DISPONIBLES ───────────────────────────────────────────────
    private JPanel pagePlaces() {
        JPanel p=new JPanel(new BorderLayout(0,0)); p.setOpaque(false);

        JPanel h=new JPanel(new FlowLayout(FlowLayout.LEFT,20,16)); h.setBackground(CARD_BG);
        h.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER));
        JLabel titre=new JLabel("Plan du Parking — Temps Réel"); titre.setFont(new Font("Segoe UI",Font.BOLD,18)); titre.setForeground(TEXT_DARK);

        JButton btnRef=bigBtn("Actualiser",PRIMARY,Color.WHITE,false); btnRef.setPreferredSize(new Dimension(140,40)); btnRef.addActionListener(e->refreshPlaces());
        JButton btnBack=bigBtn("Retour",CARD_BG,TEXT_DARK,false); btnBack.setPreferredSize(new Dimension(110,40)); btnBack.addActionListener(e->show("ACCUEIL"));

        h.add(titre); h.add(Box.createHorizontalStrut(20)); h.add(btnRef); h.add(btnBack);
        p.add(h, BorderLayout.NORTH);

        JPanel legende=new JPanel(new FlowLayout(FlowLayout.LEFT,24,10)); legende.setOpaque(false);
        for(String[] l : new String[][]{{"PLACE LIBRE", GREEN.getRed()+","+GREEN.getGreen()+","+GREEN.getBlue()},{"VÉHICULE STATIONNÉ", PRIMARY.getRed()+","+PRIMARY.getGreen()+","+PRIMARY.getBlue()}}){
            String[] rgb=l[1].split(","); Color c=new Color(Integer.parseInt(rgb[0]),Integer.parseInt(rgb[1]),Integer.parseInt(rgb[2]));
            JLabel dot=new JLabel(){@Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(c);g2.fillRoundRect(0,4,20,12,4,4);g2.dispose();}};
            dot.setPreferredSize(new Dimension(24,20));
            JLabel lbl=new JLabel(l[0]); lbl.setFont(new Font("Segoe UI",Font.BOLD,12)); lbl.setForeground(TEXT_MUTED);
            legende.add(dot); legende.add(lbl);
        }
        p.add(legende, BorderLayout.AFTER_LAST_LINE);

        JPanel gridWrapper=new JPanel(new BorderLayout()); gridWrapper.setOpaque(false); gridWrapper.setBorder(new EmptyBorder(30,40,30,40));
        placesGrid=new JPanel(new GridLayout(0, 6, 12, 70)); placesGrid.setOpaque(false);
        JPanel gridAligner = new JPanel(new BorderLayout()); gridAligner.setOpaque(false); gridAligner.add(placesGrid, BorderLayout.NORTH);
        JScrollPane sp=new JScrollPane(gridAligner); sp.setBorder(null); sp.getViewport().setOpaque(false); sp.setOpaque(false); sp.getVerticalScrollBar().setUnitIncrement(20);
        gridWrapper.add(sp, BorderLayout.CENTER); p.add(gridWrapper, BorderLayout.CENTER);
        return p;
    }

    private void refreshPlaces() {
        if(placesGrid==null) return;
        placesGrid.removeAll();
        String sql="SELECT p.numero_place, p.type_place, p.est_disponible, v.immatriculation FROM places p LEFT JOIN vehicules v ON p.id_place = v.id_place ORDER BY CAST(p.numero_place AS UNSIGNED)";
        try(Connection c=Database.getConnection(); ResultSet rs=c.createStatement().executeQuery(sql)){
            int index = 0;
            while(rs.next()){ placesGrid.add(makeSpotCard(rs.getString("numero_place"), rs.getString("type_place"), rs.getBoolean("est_disponible"), rs.getString("immatriculation"), false, index)); index++; }
        } catch(SQLException e){ e.printStackTrace(); }
        placesGrid.revalidate(); placesGrid.repaint();
    }

    private JPanel makeSpotCard(String num, String type, boolean libre, String mat, boolean incident, int index) {
        Color accent = incident ? GOLD : (libre ? GREEN : PRIMARY);
        boolean openTop = (index / 6) % 2 != 0;
        JPanel sp = new JPanel(null){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(232, 236, 241)); g2.fillRect(0, 0, w, h);
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(6f));
                g2.drawLine(3, 0, 3, h); g2.drawLine(w-3, 0, w-3, h);
                if (!openTop) g2.drawLine(0, 3, w, 3); else g2.drawLine(0, h-3, w, h-3);
                g2.setColor(accent); if (!openTop) g2.fillRect(12, h-10, w-24, 10); else g2.fillRect(12, 0, w-24, 10);
                if(libre) {
                    g2.setFont(new Font("Segoe UI",Font.BOLD, 34)); g2.setColor(new Color(180, 190, 205));
                    FontMetrics fm=g2.getFontMetrics(); g2.drawString(num,(w-fm.stringWidth(num))/2, h/2 + 12);
                } else {
                    drawTopDownCar(g2, 16, 15, w-32, h-30, accent, openTop);
                    if(mat!=null){
                        String m = mat.length()>8?mat.substring(0,8):mat;
                        g2.setColor(Color.WHITE); g2.fillRoundRect(w/2 - 28, h/2 - 8, 56, 16, 4, 4);
                        g2.setColor(TEXT_DARK); g2.setFont(new Font("Courier New",Font.BOLD,11));
                        FontMetrics fm=g2.getFontMetrics(); g2.drawString(m,(w-fm.stringWidth(m))/2, h/2 + 3);
                    }
                }
                g2.dispose();
            }
        };
        sp.setOpaque(false); sp.setPreferredSize(new Dimension(110, 160)); return sp;
    }

    private void drawTopDownCar(Graphics2D g2, int x, int y, int w, int h, Color carColor, boolean faceDown) {
        g2.setColor(new Color(0, 0, 0, 40)); g2.fillRoundRect(x + 4, y + 4, w, h, 15, 15);
        g2.setColor(carColor); g2.fillRoundRect(x, y, w, h, 16, 16);
        g2.setColor(new Color(20, 25, 35));
        if (faceDown) {
            g2.fillRoundRect(x + 4, y + h - 25, w - 8, 15, 6, 6); g2.fillRoundRect(x + 6, y + 10, w - 12, 10, 4, 4);
            g2.setColor(carColor.brighter()); g2.fillRoundRect(x + 8, y + 25, w - 16, h - 55, 8, 8);
            g2.setColor(new Color(255, 255, 200)); g2.fillOval(x + 4, y + h - 6, 8, 5); g2.fillOval(x + w - 12, y + h - 6, 8, 5);
        } else {
            g2.fillRoundRect(x + 4, y + 10, w - 8, 15, 6, 6); g2.fillRoundRect(x + 6, y + h - 20, w - 12, 10, 4, 4);
            g2.setColor(carColor.brighter()); g2.fillRoundRect(x + 8, y + 30, w - 16, h - 55, 8, 8);
            g2.setColor(new Color(255, 255, 200)); g2.fillOval(x + 4, y + 1, 8, 5); g2.fillOval(x + w - 12, y + 1, 8, 5);
        }
    }

    // ── Page SAISIE PLAQUE ────────────────────────────────────────────────────
    private String saisieMode="ES"; private JTextField fPlaqueSaisie;
    private void setSaisieMode(String mode){ saisieMode=mode; if(fPlaqueSaisie!=null) fPlaqueSaisie.setText(""); }

    private JPanel pageSaisie() {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); GridBagConstraints g=gbc();
        JLabel titre=new JLabel("Saisissez votre plaque"); titre.setFont(new Font("Segoe UI",Font.BOLD,36)); titre.setForeground(TEXT_DARK);
        JLabel hint=new JLabel("Exemple : AB 1234 CD"); hint.setFont(new Font("Segoe UI",Font.ITALIC,15)); hint.setForeground(TEXT_MUTED);
        fPlaqueSaisie = cbField(""); fPlaqueSaisie.setFont(new Font("Courier New",Font.BOLD,42)); fPlaqueSaisie.setHorizontalAlignment(JTextField.CENTER); fPlaqueSaisie.setPreferredSize(new Dimension(480,80));
        JPanel kbd=clavier(fPlaqueSaisie);
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.CENTER,20,0)); btns.setOpaque(false);
        JButton ok=bigBtn("Valider",PRIMARY,Color.WHITE, false); JButton back=bigBtn("Annuler",CARD_BG,TEXT_DARK, false); back.addActionListener(e->show("ACCUEIL"));
        ok.addActionListener(e->{ String mat=fPlaqueSaisie.getText().trim().toUpperCase().replaceAll("\\s+"," "); if(mat.isEmpty()) return; matricule=mat; if("PAY".equals(saisieMode)) lancerPaiement(mat); else lancerEntreeSortie(mat); });
        btns.add(ok); btns.add(back);
        g.gridx=0;g.gridy=0;g.insets=new Insets(0,0,8,0);  p.add(titre,g);
        g.gridy=1;             g.insets=new Insets(0,0,28,0); p.add(hint,g);
        g.gridy=2;             g.insets=new Insets(0,0,30,0); p.add(fPlaqueSaisie,g);
        g.gridy=3;             g.insets=new Insets(0,0,35,0); p.add(kbd,g);
        g.gridy=4;             g.insets=new Insets(0,0,0,0);  p.add(btns,g);
        return p;
    }

    private JPanel pageResultat() {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); GridBagConstraints g=gbc();
        resTitle=new JLabel(""); resTitle.setFont(new Font("Segoe UI",Font.BOLD,38)); resTitle.setForeground(TEXT_DARK); resTitle.setHorizontalAlignment(JLabel.CENTER);
        resDetail=new JLabel(""); resDetail.setFont(new Font("Segoe UI",Font.PLAIN,20)); resDetail.setForeground(TEXT_MUTED); resDetail.setHorizontalAlignment(JLabel.CENTER);
        resInfo=new JLabel("");   resInfo.setFont(new Font("Segoe UI",Font.BOLD,28));   resInfo.setForeground(PRIMARY); resInfo.setHorizontalAlignment(JLabel.CENTER);
        JButton back=bigBtn("Retour à l'accueil",PRIMARY,Color.WHITE, false); back.addActionListener(e->show("ACCUEIL"));
        g.gridx=0;g.gridy=0;g.insets=new Insets(0,0,16,0); p.add(resTitle,g); g.gridy=1; g.insets=new Insets(0,0,12,0); p.add(resDetail,g);
        g.gridy=2; g.insets=new Insets(0,0,50,0); p.add(resInfo,g); g.gridy=3; p.add(back,g); return p;
    }

    private JPanel pagePaiement() {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); GridBagConstraints g=gbc();
        JLabel titre=new JLabel("Règlement"); titre.setFont(new Font("Segoe UI",Font.BOLD,40)); titre.setForeground(TEXT_DARK);
        payMatLbl=new JLabel(""); payMatLbl.setFont(new Font("Segoe UI",Font.BOLD,18)); payMatLbl.setForeground(TEXT_DARK);
        payDureeLbl=new JLabel(""); payDureeLbl.setFont(new Font("Segoe UI",Font.BOLD,18)); payDureeLbl.setForeground(TEXT_DARK);
        payMontantLbl=new JLabel(""); payMontantLbl.setFont(new Font("Segoe UI",Font.BOLD,68)); payMontantLbl.setForeground(PRIMARY);
        JPanel recap=new JPanel(new GridLayout(2,2,10,8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,10)); g2.fillRoundRect(3, 5, getWidth()-6, getHeight()-6, 20, 20);
                g2.setColor(CARD_BG); g2.fillRoundRect(0,0,getWidth()-4,getHeight()-4,20,20);
                g2.setColor(BORDER); g2.drawRoundRect(0,0,getWidth()-5,getHeight()-5,20,20); g2.dispose();
            }
        };
        recap.setOpaque(false); recap.setBorder(new EmptyBorder(16,24,16,24)); recap.setPreferredSize(new Dimension(500,100));
        recap.add(muted("Véhicule :")); recap.add(payMatLbl); recap.add(muted("Durée :")); recap.add(payDureeLbl);
        JPanel modes=new JPanel(new GridLayout(1,2,24,0)); modes.setOpaque(false); modes.setPreferredSize(new Dimension(660,110));
        JPanel bEsp=actionBtn("Espèces","Paiement à la sortie",PRIMARY,"CASH", 0); JPanel bCarte=actionBtn("Carte Bancaire","Paiement sécurisé",PRIMARY,"CARD", 1);
        bEsp.addMouseListener(onClick(e->payerEspeces())); bCarte.addMouseListener(onClick(e->show("PAIEMENT_CB"))); modes.add(bEsp); modes.add(bCarte);
        JButton back=bigBtn("Annuler",CARD_BG,TEXT_DARK, false); back.addActionListener(e->show("ACCUEIL"));
        g.gridx=0;g.gridy=0;g.insets=new Insets(0,0,20,0); p.add(titre,g); g.gridy=1; g.insets=new Insets(0,0,20,0); p.add(recap,g);
        g.gridy=2; g.insets=new Insets(0,0,12,0); p.add(muted("Montant à régler :"),g); g.gridy=3; g.insets=new Insets(0,0,40,0); p.add(payMontantLbl,g);
        g.gridy=4; g.insets=new Insets(0,0,40,0); p.add(modes,g); g.gridy=5; g.insets=new Insets(0,0,0,0);  p.add(back,g); return p;
    }

    private JPanel pagePaiementCB() {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); GridBagConstraints g=gbc();
        JLabel titre=new JLabel("Paiement par Carte"); titre.setFont(new Font("Segoe UI",Font.BOLD,34)); titre.setForeground(TEXT_DARK);
        JTextField fNom=cbField("Nom du titulaire"); JTextField fNum=cbField("Numéro de carte (16 chiffres)");
        JPanel rowCB=new JPanel(new GridLayout(1,2,16,0)); rowCB.setOpaque(false); rowCB.setPreferredSize(new Dimension(480,60));
        rowCB.add(cbField("MM/AA")); rowCB.add(cbField("CVV"));
        JButton ok = bigBtn("Confirmer le paiement", PRIMARY, Color.WHITE, true); JButton back = bigBtn("Retour", CARD_BG, TEXT_DARK, false); back.addActionListener(e->show("PAIEMENT"));
        ok.addActionListener(e->{
            String nom=fNom.getText().trim(), num=fNum.getText().replaceAll("[^0-9]","");
            if(nom.isEmpty()||nom.equals("Nom du titulaire")||num.length()<12){ JOptionPane.showMessageDialog(this,"Informations carte incomplètes.","Erreur",JOptionPane.WARNING_MESSAGE); return; }
            ok.setEnabled(false); ok.putClientProperty("loading", true); ok.setText("Traitement...");
            new Timer(2500,ev->{ ((Timer)ev.getSource()).stop(); dao.enregistrerSortie(matricule);
                cfTitle.setForeground(GREEN); cfTitle.setText("Paiement validé !"); cfInfo.setText(String.format("%.2f DH réglés par CB",montantDu));
                cfSub.setText("Transaction : #TR-"+String.format("%08d",new Random().nextInt(99999999)));
                ok.setEnabled(true); ok.putClientProperty("loading", false); ok.setText("Confirmer le paiement"); show("CONFIRM");
            }).start();
        });
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.CENTER,16,0)); btns.setOpaque(false); btns.add(ok); btns.add(back);
        g.gridx=0;g.gridy=0;g.insets=new Insets(0,0,35,0); p.add(titre,g); g.gridy=1; g.insets=new Insets(0,0,16,0); p.add(fNom,g);
        g.gridy=2; g.insets=new Insets(0,0,16,0); p.add(fNum,g); g.gridy=3; g.insets=new Insets(0,0,40,0); p.add(rowCB,g);
        g.gridy=4; g.insets=new Insets(0,0,0,0);  p.add(btns,g); return p;
    }

    private JPanel pageAbonnement() {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); GridBagConstraints g=gbc();
        JLabel titre=new JLabel("Vérification d'Abonnement"); titre.setFont(new Font("Segoe UI",Font.BOLD,36)); titre.setForeground(TEXT_DARK);
        abMatField = cbField("Exemple : AB 1234 CD"); abMatField.setFont(new Font("Courier New",Font.BOLD,28)); abMatField.setHorizontalAlignment(JTextField.CENTER); abMatField.setPreferredSize(new Dimension(420,70));
        abStatusLbl=new JLabel("Saisissez votre plaque"); abStatusLbl.setFont(new Font("Segoe UI",Font.BOLD,20)); abStatusLbl.setForeground(TEXT_MUTED); abStatusLbl.setHorizontalAlignment(JLabel.CENTER);
        abDetailLbl=new JLabel(""); abDetailLbl.setFont(new Font("Segoe UI",Font.PLAIN,15)); abDetailLbl.setForeground(TEXT_DARK); abDetailLbl.setHorizontalAlignment(JLabel.CENTER);
        JButton check=bigBtn("Vérifier",PRIMARY,Color.WHITE, true); JButton back=bigBtn("Retour",CARD_BG,TEXT_DARK, false);
        check.addActionListener(e->{ String mat=abMatField.getText().trim().toUpperCase();
            if(!mat.isEmpty() && !mat.equals("EXEMPLE : AB 1234 CD")){
                check.putClientProperty("loading", true); check.setText(""); check.setEnabled(false);
                new Timer(800, ev -> { ((Timer)ev.getSource()).stop(); matricule=mat; verifierAbonnement(mat); check.putClientProperty("loading", false); check.setText("Vérifier"); check.setEnabled(true); }).start();
            }
        });
        back.addActionListener(e->show("ACCUEIL"));
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.CENTER,16,0)); btns.setOpaque(false); btns.add(check); btns.add(back);
        g.gridx=0;g.gridy=0;g.insets=new Insets(0,0,40,0); p.add(titre,g); g.gridy=1; g.insets=new Insets(0,0,30,0); p.add(abMatField,g);
        g.gridy=2; g.insets=new Insets(0,0,30,0); p.add(btns,g); g.gridy=3; g.insets=new Insets(0,0,10,0); p.add(abStatusLbl,g);
        g.gridy=4; g.insets=new Insets(0,0,0,0);  p.add(abDetailLbl,g); return p;
    }

    // ── Page ASSISTANCE (Avec Texte Noir & Bouton Vert Foncé) ─────────────────
    private JPanel pageAssistance() {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        JPanel h = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20)); h.setOpaque(false);
        JButton btnBack = bigBtn("Retour", CARD_BG, TEXT_DARK, false);
        btnBack.setPreferredSize(new Dimension(110, 40)); btnBack.addActionListener(e -> show("ACCUEIL"));
        h.add(btnBack); p.add(h, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout()); center.setOpaque(false);
        JPanel intercomBox = new JPanel(); intercomBox.setLayout(new BoxLayout(intercomBox, BoxLayout.Y_AXIS)); intercomBox.setOpaque(false);

        JLabel statusLabel = new JLabel("Appuyez pour appeler l'assistance", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 26)); statusLabel.setForeground(TEXT_DARK); statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel descLabel = new JLabel("Un agent va vous répondre dans quelques instants.", SwingConstants.CENTER);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16)); descLabel.setForeground(TEXT_DARK); descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel animatedButton = new JPanel() {
            int state = 0; float pulse = 0f; Timer animTimer;
            {
                setPreferredSize(new Dimension(240, 240)); setMaximumSize(new Dimension(240, 240)); setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
                animTimer = new Timer(16, e -> { if (state == 1) { pulse += 0.015f; if (pulse > 1f) pulse = 0f; repaint(); } });
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (state == 0) {
                            state = 1; statusLabel.setText("Appel en cours..."); statusLabel.setForeground(GOLD); descLabel.setText("Veuillez patienter, connexion à l'interphone..."); animTimer.start();
                            new Timer(4000, evt -> {
                                if (state == 1) {
                                    state = 2; animTimer.stop(); statusLabel.setText("Agent connecté"); statusLabel.setForeground(PRIMARY); descLabel.setText("Parlez vers le microphone de la borne."); repaint();
                                    new Timer(6000, ev2 -> {
                                        state = 0; pulse = 0f; statusLabel.setText("Appuyez pour appeler l'assistance"); statusLabel.setForeground(TEXT_DARK); descLabel.setText("Un agent va vous répondre dans quelques instants."); repaint(); ((Timer)ev2.getSource()).stop();
                                    }).start();
                                }
                                ((Timer)evt.getSource()).stop();
                            }).start();
                        }
                    }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                if (state == 1) {
                    g2.setStroke(new BasicStroke(4f)); Color waveColor = GOLD;
                    g2.setColor(new Color(waveColor.getRed(), waveColor.getGreen(), waveColor.getBlue(), (int)(255 * (1 - pulse))));
                    int r1 = 70 + (int)(45 * pulse); g2.drawOval(cx - r1, cy - r1, r1 * 2, r1 * 2);
                    float p2 = (pulse + 0.5f) % 1f;
                    g2.setColor(new Color(waveColor.getRed(), waveColor.getGreen(), waveColor.getBlue(), (int)(255 * (1 - p2))));
                    int r2 = 70 + (int)(45 * p2); g2.drawOval(cx - r2, cy - r2, r2 * 2, r2 * 2);
                } else if (state == 2) {
                    g2.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 40)); g2.fillOval(cx - 95, cy - 95, 190, 190);
                }
                g2.setColor(new Color(0, 0, 0, 15)); g2.fillOval(cx - 66, cy - 64, 140, 140);
                Color c = (state == 0) ? PRIMARY : (state == 1 ? GOLD : GREEN);
                g2.setColor(c); g2.fillOval(cx - 70, cy - 70, 140, 140);
                g2.setColor(new Color(0, 0, 0, 30)); g2.fillOval(cx - 60, cy - 60, 120, 120);

                g2.setColor(TEXT_DARK); // Texte Noir dans le bouton
                g2.setFont(new Font("Segoe UI", Font.BOLD, 34)); String txt = state == 0 ? "SOS" : (state == 1 ? "..." : "🎤");
                FontMetrics fm = g2.getFontMetrics(); g2.drawString(txt, cx - fm.stringWidth(txt) / 2, cy + 12); g2.dispose();
            }
        };

        intercomBox.add(Box.createVerticalStrut(20)); intercomBox.add(animatedButton); intercomBox.add(Box.createVerticalStrut(40));
        intercomBox.add(statusLabel); intercomBox.add(Box.createVerticalStrut(10)); intercomBox.add(descLabel);
        center.add(intercomBox); p.add(center, BorderLayout.CENTER); return p;
    }

    private JPanel pageConfirm() {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); GridBagConstraints g=gbc();
        JPanel ico = new JPanel() {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gr = (Graphics2D) g2.create(); gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float anim = (float)Math.abs(Math.sin((System.currentTimeMillis() - startTime) / 500.0));
                gr.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), (int)(20 + anim*30))); gr.fillOval(0,0,100,100);
                gr.setColor(PRIMARY); gr.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gr.drawOval(10,10,80,80); gr.drawLine(30,50,45,65); gr.drawLine(45,65,75,35); gr.dispose();
            }
        };
        ico.setPreferredSize(new Dimension(100, 100)); ico.setOpaque(false);
        cfTitle=new JLabel(""); cfTitle.setFont(new Font("Segoe UI",Font.BOLD,36)); cfTitle.setForeground(TEXT_DARK); cfTitle.setHorizontalAlignment(JLabel.CENTER);
        cfInfo=new JLabel("");  cfInfo.setFont(new Font("Segoe UI",Font.BOLD,24)); cfInfo.setForeground(PRIMARY); cfInfo.setHorizontalAlignment(JLabel.CENTER);
        cfSub=new JLabel("");   cfSub.setFont(new Font("Segoe UI",Font.PLAIN,18)); cfSub.setForeground(TEXT_MUTED); cfSub.setHorizontalAlignment(JLabel.CENTER);
        JButton back=bigBtn("Terminer",PRIMARY,Color.WHITE, false); back.addActionListener(e->show("ACCUEIL"));
        g.gridx=0;g.gridy=0;g.insets=new Insets(0,0,24,0); p.add(ico,g); g.gridy=1; g.insets=new Insets(0,0,14,0); p.add(cfTitle,g);
        g.gridy=2; g.insets=new Insets(0,0,14,0);  p.add(cfInfo,g); g.gridy=3; g.insets=new Insets(0,0,50,0); p.add(cfSub,g);
        g.gridy=4; p.add(back,g); return p;
    }

    // =========================================================================
    // LOGIQUE METIER (Identique)
    // =========================================================================
    private void lancerEntreeSortie(String mat) {
        boolean present = false;
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM vehicules WHERE immatriculation=?")) {
            ps.setString(1, mat); ResultSet rs = ps.executeQuery(); if (rs.next()) present = rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }

        if (!present) {
            try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id_place, numero_place FROM places WHERE est_disponible = 1 LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int idPlace = rs.getInt("id_place"); String numPlace = rs.getString("numero_place");
                    if (dao.enregistrerEntree(mat, idPlace, "VOITURE")) { genererTicketEntree(mat, "VOITURE", numPlace); afficherResultat("Entrée Autorisée", "Bienvenue ! Vous êtes stationné à la place :", numPlace, PRIMARY); } else { afficherResultat("Erreur", "Impossible d'enregistrer l'entrée.", "Veuillez contacter l'assistance.", RED); }
                } else { afficherResultat("Parking Complet", "Aucune place disponible.", "Revenez plus tard.", RED); }
            } catch (SQLException e) { afficherResultat("Erreur Système", "Problème de connexion BD.", "Veuillez patienter.", RED); }
        } else { lancerPaiement(mat); }
    }

    private void lancerPaiement(String mat) {
        if (dao.aAbonnementActif(mat)) { dao.enregistrerSortie(mat); afficherResultat("Abonnement Actif", "Sortie autorisée.", "Passez une bonne journée !", PRIMARY); return; }
        String sql = "SELECT type_vehicule, TIMESTAMPDIFF(MINUTE, heure_entree, NOW()) as duree FROM vehicules WHERE immatriculation = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mat); ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String typeV = rs.getString("type_vehicule"); long duree = rs.getLong("duree");
                double montant = dao.calculerMontant(duree, typeV); this.montantDu = montant;
                if (montant > 0) { payMatLbl.setText(mat); payDureeLbl.setText(duree + " min"); payMontantLbl.setText(String.format("%.2f DH", montant)); show("PAIEMENT");
                } else { dao.enregistrerSortie(mat); afficherResultat("Gratuit", "Durée inférieure à 15 min.", "Sortie autorisée.", PRIMARY); }
            } else { afficherResultat("Introuvable", "Véhicule non trouvé.", "Vérifiez votre plaque.", RED); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void payerEspeces() { dao.enregistrerSortie(matricule); cfTitle.setForeground(TEXT_DARK); cfTitle.setText("Paiement à la borne de sortie"); cfInfo.setText(String.format("%.2f DH", montantDu)); cfSub.setText("Veuillez régler en espèces directement auprès de l'agent."); show("CONFIRM"); }
    private void verifierAbonnement(String mat) { if (dao.aAbonnementActif(mat)) { abStatusLbl.setForeground(PRIMARY); abStatusLbl.setText("Abonnement Actif"); abDetailLbl.setText("Accès autorisé sans frais supplémentaires."); } else { abStatusLbl.setForeground(RED); abStatusLbl.setText("Aucun abonnement valide"); abDetailLbl.setText("Veuillez régulariser votre situation."); } }
    private void envoyerAssistance() { JOptionPane.showMessageDialog(this, "Un agent a été notifié et se dirige vers la borne.", "Assistance Demandée", JOptionPane.INFORMATION_MESSAGE); }
    private void afficherResultat(String titre, String detail, String info, Color couleur) { resTitle.setText(titre); resTitle.setForeground(couleur); resDetail.setText(detail); resInfo.setText(info); resInfo.setForeground(TEXT_DARK); show("RESULTAT"); }
    private void quitter() { if (JOptionPane.showConfirmDialog(this, "Mettre la borne hors service ?", "Quitter", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) System.exit(0); }
    private void genererTicketEntree(String matricule, String type, String numPlace) { JOptionPane.showMessageDialog(this, "Impression du ticket en cours...\nMatricule : " + matricule + "\nPlace : " + numPlace, "Impression", JOptionPane.INFORMATION_MESSAGE); }
    private void resetAbonnement() { if (abMatField != null) { abMatField.setText("Exemple : AB 1234 CD"); abMatField.setForeground(TEXT_MUTED); } if (abStatusLbl != null) { abStatusLbl.setForeground(TEXT_MUTED); abStatusLbl.setText("Saisissez votre plaque"); } if (abDetailLbl != null) abDetailLbl.setText(""); }

    // =========================================================================
    // UTILITAIRES GRAPHIQUES
    // =========================================================================
    private JLabel muted(String t) { JLabel l = new JLabel(t); l.setFont(new Font("Segoe UI", Font.PLAIN, 15)); l.setForeground(TEXT_MUTED); return l; }
    private GridBagConstraints gbc() { GridBagConstraints g = new GridBagConstraints(); g.fill = GridBagConstraints.NONE; return g; }

    private JButton bigBtn(String txt, Color bg, Color fg, boolean allowLoading) {
        JButton b = new JButton(txt) {
            Point clickPoint = new Point(0,0); float rippleRadius = 0; float rippleAlpha = 0;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        clickPoint = e.getPoint(); rippleRadius = 10; rippleAlpha = 0.5f;
                        new Timer(15, t -> { rippleRadius += 8; rippleAlpha -= 0.02f; if (rippleAlpha <= 0) { rippleAlpha = 0; ((Timer)t.getSource()).stop(); } repaint(); }).start();
                    }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,15)); g2.fillRoundRect(3, 5, getWidth()-6, getHeight()-6, getHeight(), getHeight());

                if (bg == CARD_BG) {
                    g2.setColor(CARD_BG); g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-4, getHeight(), getHeight());
                    g2.setColor(BORDER); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(0, 0, getWidth()-3, getHeight()-5, getHeight(), getHeight());
                } else {
                    GradientPaint gp = new GradientPaint(0, 0, PRIMARY_L, getWidth(), getHeight(), bg);
                    g2.setPaint(gp); g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-4, getHeight(), getHeight());
                }

                if (rippleAlpha > 0) {
                    g2.setColor(new Color(255, 255, 255, (int)(rippleAlpha * 255))); g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-4, getHeight(), getHeight()));
                    g2.fillOval((int)(clickPoint.x - rippleRadius), (int)(clickPoint.y - rippleRadius), (int)(rippleRadius*2), (int)(rippleRadius*2)); g2.setClip(null);
                }

                g2.setColor(fg); g2.setFont(getFont()); FontMetrics fm = g2.getFontMetrics();
                boolean isLoading = allowLoading && Boolean.TRUE.equals(getClientProperty("loading"));
                if (isLoading) {
                    float time = (System.currentTimeMillis() - startTime) / 1000f; int angle = (int)(time * 360) % 360;
                    g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc((getWidth()/2) - 80, (getHeight()/2) - 10, 20, 20, -angle, 270);
                    g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2 + 15, (getHeight() + fm.getAscent() - fm.getDescent() - 4) / 2);
                } else { g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent() - 4) / 2); }
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 18)); b.setPreferredSize(new Dimension(300, 56)); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        return b;
    }

    private JPanel actionBtn(String title, String desc, Color accent, String iconType, int staggerIndex) {
        JPanel p = new JPanel(new BorderLayout()) {
            float hoverVal = 0; Point clickPoint = new Point(0,0); float rippleRadius = 0; float rippleAlpha = 0;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { setCursor(new Cursor(Cursor.HAND_CURSOR)); }
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        clickPoint = e.getPoint(); rippleRadius = 20; rippleAlpha = 0.4f;
                        new Timer(15, t -> { rippleRadius += 12; rippleAlpha -= 0.02f; if(rippleAlpha <= 0){rippleAlpha=0; ((Timer)t.getSource()).stop();} repaint(); }).start();
                    }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                long elapsed = System.currentTimeMillis() - pageLoadTime; long delay = staggerIndex * 150L;
                float introProgress = Math.min(1.0f, Math.max(0f, (elapsed - delay) / 400f));
                int currentYOffset = (int)(40 * (1.0f - introProgress));

                Point mouseLoc = getMousePosition(); boolean hovered = (mouseLoc != null);
                if (hovered && hoverVal < 1) hoverVal += 0.1f; else if (!hovered && hoverVal > 0) hoverVal -= 0.1f;
                if(hoverVal < 0) hoverVal = 0; if(hoverVal > 1) hoverVal = 1;

                int elevateY = (int)(hoverVal * 6);
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, introProgress));
                g2.translate(0, currentYOffset - elevateY);

                g2.setColor(new Color(0, 0, 0, 8 + (int)(hoverVal * 12))); g2.fillRoundRect(5, 5 + elevateY/2, getWidth()-10, getHeight()-10, 20, 20);
                g2.setColor(hoverVal > 0.1 ? new Color(248, 252, 255) : CARD_BG); g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 20, 20);

                if(rippleAlpha > 0) {
                    g2.setClip(new RoundRectangle2D.Float(2, 2, getWidth()-4, getHeight()-4, 20, 20));
                    g2.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), (int)(rippleAlpha * 255)));
                    g2.fillOval((int)(clickPoint.x - rippleRadius), (int)(clickPoint.y - rippleRadius), (int)(rippleRadius*2), (int)(rippleRadius*2));
                    g2.setClip(null);
                }

                g2.setColor(hoverVal > 0.1 ? accent : BORDER); g2.setStroke(new BasicStroke(hoverVal > 0.1 ? 2f : 1f)); g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 20, 20);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        JLabel lblTitle = new JLabel(" " + title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 19)); lblTitle.setForeground(TEXT_DARK);
        JLabel lblDesc = new JLabel(" " + desc); lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 15)); lblDesc.setForeground(TEXT_MUTED);
        JPanel texts = new JPanel(new GridLayout(2, 1)); texts.setOpaque(false); texts.add(lblTitle); texts.add(lblDesc);

        JPanel iconPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float time = (System.currentTimeMillis() - startTime) / 400f; int floatY = (int)(Math.sin(time) * 4);
                drawVectorIcon(g2, iconType, 18, 25 + floatY, 44, 44, accent); g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(85, 90)); iconPanel.setOpaque(false);
        p.add(iconPanel, BorderLayout.WEST); p.add(texts, BorderLayout.CENTER); p.setPreferredSize(new Dimension(360, 105));
        return p;
    }

    private void drawVectorIcon(Graphics2D g2, String type, int x, int y, int w, int h, Color c) {
        g2.setColor(c); g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = x + w/2, cy = y + h/2;
        switch(type) {
            case "CAR": g2.drawRoundRect(x, y+15, w, h-15, 8, 8); g2.drawArc(x+5, y+3, w-10, 24, 0, 180); g2.fillOval(x+6, y+h-6, 10, 10); g2.fillOval(x+w-16, y+h-6, 10, 10); break;
            case "CARD": g2.drawRoundRect(x, y+5, w, h-10, 8, 8); g2.fillRect(x, y+16, w, 8); g2.fillRect(x+8, y+32, 12, 6); break;
            case "PIN": g2.drawArc(x+5, y, w-10, w-10, 0, 360); g2.drawLine(x+5, cy, cx, y+h+4); g2.drawLine(x+w-5, cy, cx, y+h+4); g2.fillOval(cx-5, y+10, 10, 10); break;
            case "STAR": g2.drawOval(x, y, w, h); g2.drawLine(cx, y+8, cx, y+h-8); g2.drawLine(x+8, cy, x+w-8, cy); g2.drawLine(x+12, y+12, x+w-12, y+h-12); g2.drawLine(x+12, y+h-12, x+w-12, y+12); break;
            case "SOS": g2.drawOval(x, y, w, h); g2.drawOval(x+10, y+10, w-20, h-20); g2.drawLine(cx, y, cx, y+10); g2.drawLine(cx, y+h-10, cx, y+h); g2.drawLine(x, cy, x+10, cy); g2.drawLine(x+w-10, cy, x+w, cy); break;
            case "CASH": g2.drawRoundRect(x, y+10, w, h-20, 4, 4); g2.drawOval(cx-8, cy-8, 16, 16); g2.drawLine(x+5, y+5, x+w-5, y+5); break;
        }
    }

    private JTextField cbField(String ph) {
        JTextField tf = new JTextField(ph) {
            float focusVal = 0;
            @Override protected void paintComponent(Graphics g) {
                if(hasFocus() && focusVal < 1) focusVal += 0.1f; else if(!hasFocus() && focusVal > 0) focusVal -= 0.1f;
                if(focusVal < 0) focusVal = 0; if(focusVal > 1) focusVal = 1;
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                int r = (int)(BORDER.getRed() + (PRIMARY_L.getRed() - BORDER.getRed()) * focusVal); int gr= (int)(BORDER.getGreen() + (PRIMARY_L.getGreen() - BORDER.getGreen()) * focusVal); int b = (int)(BORDER.getBlue() + (PRIMARY_L.getBlue() - BORDER.getBlue()) * focusVal);
                g2.setColor(new Color(r, gr, b)); g2.setStroke(new BasicStroke(1.5f + focusVal)); g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16); super.paintComponent(g); g2.dispose();
            }
        };
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 18)); tf.setForeground(TEXT_MUTED); tf.setOpaque(false); tf.setCaretColor(PRIMARY); tf.setBorder(new EmptyBorder(8, 20, 8, 20)); tf.setPreferredSize(new Dimension(460, 60));
        tf.addFocusListener(new java.awt.event.FocusAdapter() { public void focusGained(java.awt.event.FocusEvent e) { if (tf.getText().equals(ph)) { tf.setText(""); tf.setForeground(TEXT_DARK); } } public void focusLost(java.awt.event.FocusEvent e) { if (tf.getText().isEmpty()) { tf.setText(ph); tf.setForeground(TEXT_MUTED); } } }); return tf;
    }

    private java.awt.event.MouseAdapter onClick(Consumer<java.awt.event.MouseEvent> action) { return new java.awt.event.MouseAdapter() { @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.accept(e); } }; }

    private JPanel clavier(JTextField target) {
        JPanel kbd = new JPanel(new GridLayout(4, 10, 10, 10)); kbd.setOpaque(false);
        String[] touches = {"1","2","3","4","5","6","7","8","9","0", "A","Z","E","R","T","Y","U","I","O","P", "Q","S","D","F","G","H","J","K","L","M", "W","X","C","V","B","N","-"," ","<-","CLR"};
        for (String t : touches) {
            JButton btn = new JButton(t) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isSpecial = t.equals("<-") || t.equals("CLR"); boolean pressed = getModel().isPressed(); int depth = pressed ? 1 : 5;
                    g2.setColor(new Color(180, 190, 205)); g2.fillRoundRect(0, depth, getWidth(), getHeight()-depth, 12, 12);
                    g2.setColor(isSpecial ? new Color(241, 245, 249) : CARD_BG); if (pressed) g2.setColor(new Color(230, 235, 240));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight()-depth, 12, 12);
                    g2.setColor(t.equals("CLR") ? RED : (t.equals("<-") ? GOLD : TEXT_DARK)); g2.setFont(getFont()); FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2, (getHeight() + fm.getAscent() - fm.getDescent() - depth) / 2); g2.dispose();
                }
            };
            btn.setFont(new Font("Segoe UI", Font.BOLD, 22)); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                if (t.equals("<-")) { String text = target.getText(); if (text.length() > 0) target.setText(text.substring(0, text.length() - 1)); }
                else if (t.equals("CLR")) { target.setText(""); } else { target.setText(target.getText() + t); } target.setForeground(TEXT_DARK);
            });
            kbd.add(btn);
        }
        return kbd;
    }
}