package view.admin;

import database.ParkingDAO;
import util.AdminUI;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.Random;

/**
 * AbonnementsFrame — Adapté au thème "Luxe / Prestige" (AdminUI)
 */
public class AbonnementsFrame extends AdminUI.AdminDialog {

    private static final double[] PRIX  = {150.0, 280.0, 400.0, 1500.0};
    private static final int[]    JOURS = {30, 60, 90, 365};
    private static final String[] LBLS  = {"1 MOIS","2 MOIS","3 MOIS","1 AN"};
    private static final Color[]  COLS  = {
            AdminUI.GREEN, AdminUI.BLUE,
            AdminUI.GOLD,  AdminUI.PURPLE
    };

    private JTable            table;
    private DefaultTableModel model;
    private ParkingDAO        dao    = new ParkingDAO();
    private Frame             parent;
    private CartePanel        cartePanel;
    private JLabel            lblCount;

    public AbonnementsFrame(Frame parent) {
        super(parent, "Abonnements — Smart Parking", 1080, 700);
        this.parent = parent;
        init();
        add(buildFooter(), BorderLayout.SOUTH);
        chargerDonnees();
    }

    @Override
    protected JPanel buildHeader() {
        lblCount = new JLabel("0 abonnés");
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCount.setForeground(AdminUI.GOLD);
        return AdminUI.buildHeader(
                "Gestion des Abonnements",
                "Cartes membres  ·  Paiement  ·  Reçus  ·  Aperçu carte",
                AdminUI.GOLD, lblCount);
    }

    @Override
    protected JComponent buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), buildRight());
        split.setDividerLocation(620);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setOpaque(false);
        split.setBackground(AdminUI.BG);
        return split;
    }

    private JPanel buildLeft() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false); p.setBorder(new EmptyBorder(14, 16, 0, 8));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); bar.setOpaque(false);
        bar.add(AdminUI.sectionLabel("Abonnés enregistrés", AdminUI.GOLD));
        p.add(bar, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new String[]{"ID","Matricule","Nom Client","Formule","Mode","Statut","Expiration"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        AdminUI.styleTable(table, AdminUI.GOLD);
        table.getColumnModel().getColumn(0).setMinWidth(0); table.getColumnModel().getColumn(0).setMaxWidth(0);

        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String s = v != null ? v.toString() : "";
                Color col = s.equals("EXPIRÉ") ? AdminUI.RED : s.contains("(!)") ? AdminUI.ORANGE : AdminUI.GREEN;
                setForeground(col);
                setBackground(sel ? AdminUI.alpha(AdminUI.GOLD, 40) : r%2==0 ? AdminUI.SURFACE : AdminUI.SURFACE2);
                setHorizontalAlignment(CENTER); setFont(new Font("Segoe UI", Font.BOLD, 11));
                setBorder(new EmptyBorder(0,10,0,10)); return this;
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) updateCarte(table.getSelectedRow());
        });
        p.add(AdminUI.scrollTable(table), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRight() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false); p.setBorder(new EmptyBorder(14, 8, 0, 16));
        p.add(buildTarifsPanel(), BorderLayout.NORTH);

        JPanel cw = new JPanel(new BorderLayout(0, 8)); cw.setOpaque(false);
        cw.add(AdminUI.sectionLabel("Aperçu carte membre", AdminUI.GOLD), BorderLayout.NORTH);
        cartePanel = new CartePanel();
        cw.add(cartePanel, BorderLayout.CENTER);
        p.add(cw, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTarifsPanel() {
        JPanel w = new JPanel(new BorderLayout(0, 8)); w.setOpaque(false);
        w.add(AdminUI.sectionLabel("Formules d'abonnement", AdminUI.GOLD), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(1, 4, 8, 0)); grid.setOpaque(false);
        for (int i = 0; i < 4; i++) grid.add(makeTarifCard(i));
        grid.setPreferredSize(new Dimension(0, 96));
        w.add(grid, BorderLayout.CENTER);
        return w;
    }

    private JPanel makeTarifCard(int i) {
        boolean pop = (i == 2); Color c = COLS[i];
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight();
                g2.setColor(AdminUI.SURFACE); g2.fillRoundRect(0, 0, W, H, 12, 12);
                g2.setPaint(new GradientPaint(0,0,AdminUI.alpha(c,30),0,H/2f,new Color(0,0,0,0))); g2.fillRoundRect(0,0,W,H/2,12,12);
                g2.setPaint(new GradientPaint(0,0,c,W,0,AdminUI.alpha(c,80))); g2.fillRoundRect(0,0,W,4,4,4);
                g2.setColor(pop ? AdminUI.alpha(c,140) : AdminUI.BORDER); g2.setStroke(new BasicStroke(pop?1.5f:1f)); g2.drawRoundRect(0,0,W-1,H-1,12,12);
                int ty = 14;
                if (pop) {
                    g2.setColor(c); g2.fillRoundRect(W/2-32,0,64,15,8,8);
                    g2.setFont(new Font("Segoe UI",Font.BOLD,8)); g2.setColor(AdminUI.BG);
                    FontMetrics fm=g2.getFontMetrics(); g2.drawString("POPULAIRE",W/2-fm.stringWidth("POPULAIRE")/2,11); ty=20;
                }
                g2.setFont(new Font("Segoe UI",Font.BOLD,11)); g2.setColor(c); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(LBLS[i],(W-fm.stringWidth(LBLS[i]))/2,ty+fm.getAscent());
                String ps=String.format("%.0f",PRIX[i]);
                g2.setFont(new Font("Segoe UI",Font.BOLD,22)); g2.setColor(AdminUI.TEXT); fm=g2.getFontMetrics();
                int px=(W-fm.stringWidth(ps))/2-7; g2.drawString(ps,px,ty+40);
                g2.setFont(new Font("Segoe UI",Font.BOLD,10)); g2.setColor(c); g2.drawString("DH",px+fm.stringWidth(ps)+2,ty+40);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(AdminUI.TEXT_SEC); fm=g2.getFontMetrics();
                g2.drawString(JOURS[i]+" jours",(W-fm.stringWidth(JOURS[i]+" jours"))/2,ty+54); g2.dispose();
            }
        };
        card.setOpaque(false); return card;
    }

    @Override
    protected JPanel buildFooter() {
        JPanel f = AdminUI.footerPanel();

        // Boutons connectés aux méthodes existantes
        f.add(AdminUI.createButton("Nouvel Abonné", AdminUI.TEAL, e -> formulaireAjout()));
        f.add(AdminUI.createButton("Imprimer Reçu", AdminUI.BLUE, e -> afficherRecu()));
        f.add(AdminUI.createButton("Supprimer", AdminUI.RED, e -> supprimer()));
        f.add(AdminUI.createButton("Fermer", AdminUI.SURFACE3, e -> dispose()));

        return f;
    }

    // =========================================================================
    // UTILITAIRES DE FORMULAIRE
    // =========================================================================
    private JTextField createStyledField(String text) {
        JTextField f = new JTextField(text);
        f.setBackground(AdminUI.SURFACE);
        f.setForeground(AdminUI.TEXT);
        f.setCaretColor(AdminUI.TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdminUI.BORDER),
                new EmptyBorder(5, 10, 5, 10)
        ));
        return f;
    }

    private void addRow(JPanel f,GridBagConstraints g,String label,JComponent c,int row){
        g.gridwidth=1;g.weightx=0.35;g.gridx=0;g.gridy=row;
        JLabel l=new JLabel(label);l.setFont(new Font("Segoe UI",Font.PLAIN,12));l.setForeground(AdminUI.TEXT_SEC);f.add(l,g);
        g.gridx=1;g.weightx=0.65;f.add(c,g);
    }

    // =========================================================================
    // DONNÉES
    // =========================================================================
    private void chargerDonnees() {
        model.setRowCount(0);
        try {
            java.sql.ResultSet rs = dao.getTousAbonnements();
            int count = 0;
            while (rs != null && rs.next()) {
                int id=rs.getInt("id_abonnement"); String mat=rs.getString("matricule"); String nom=rs.getString("nom_client");
                java.sql.Timestamp fin=rs.getTimestamp("date_fin"); String mode=rs.getString("mode_paiement");
                double montant=rs.getDouble("montant"); int jr=rs.getInt("jours_restants");
                String statut = jr>0?"ACTIF":"EXPIRÉ"; if(jr>0&&jr<5) statut="ACTIF (!)";
                model.addRow(new Object[]{id,mat,nom,deduireFormule(montant),mode,statut,
                        fin!=null?new java.text.SimpleDateFormat("dd/MM/yyyy").format(fin):"N/A"});
                count++;
            }
            if (lblCount!=null) lblCount.setText(count+" abonné"+(count>1?"s":""));
        } catch (SQLException e) {
            e.printStackTrace();
            NotificationManager.show(parent,"Erreur SQL : "+e.getMessage(),NotificationManager.Type.ERROR);
        }
    }

    private String deduireFormule(double prix) {
        if(prix>=1500) return "1 AN"; if(prix>=400) return "3 MOIS"; if(prix>=280) return "2 MOIS"; return "1 MOIS";
    }

    private void updateCarte(int row) {
        if(row<0||row>=model.getRowCount()) return;
        cartePanel.setData(model.getValueAt(row,2).toString(),model.getValueAt(row,1).toString(),
                model.getValueAt(row,3).toString(),model.getValueAt(row,6).toString(),
                model.getValueAt(row,5).toString(),prixFormule(model.getValueAt(row,3).toString()),
                model.getValueAt(row,4).toString());
    }

    private double prixFormule(String f) {
        if(f.contains("1 AN")) return PRIX[3]; if(f.contains("3")) return PRIX[2]; if(f.contains("2")) return PRIX[1]; return PRIX[0];
    }

    private void formulaireAjout() {
        // CORRECTION: Utilisation de 'parent' au lieu de 'this'
        JDialog dlg = new JDialog(parent,"Nouvel Abonnement",true);
        dlg.setSize(480,340); dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(AdminUI.BG);
        JPanel root=(JPanel)dlg.getContentPane(); root.setLayout(new BorderLayout());
        root.add(AdminUI.buildHeader("Nouvel Abonnement","Remplissez les informations",AdminUI.GOLD,null),BorderLayout.NORTH);

        JPanel form=new JPanel(new GridBagLayout()); form.setOpaque(false); form.setBorder(new EmptyBorder(20,30,12,30));
        GridBagConstraints gbc=new GridBagConstraints(); gbc.fill=GridBagConstraints.HORIZONTAL; gbc.insets=new Insets(8,6,8,6);

        JTextField fMat = createStyledField("");
        JTextField fNom = createStyledField("");
        String[] opts=new String[4]; for(int i=0;i<4;i++) opts[i]=LBLS[i]+"  —  "+(int)PRIX[i]+" DH  ("+JOURS[i]+" jours)";
        JComboBox<String> combo = AdminUI.createCombo(opts);

        addRow(form,gbc,"Matricule :",fMat,0); addRow(form,gbc,"Nom complet :",fNom,1); addRow(form,gbc,"Formule :",combo,2);
        root.add(form,BorderLayout.CENTER);

        JPanel fp = AdminUI.footerPanel();
        JButton ok=AdminUI.createButton("Suivant »",AdminUI.GOLD,120,34);
        JButton ann=AdminUI.createButton("Annuler",AdminUI.TEXT_SEC,90,34);
        ann.addActionListener(e->dlg.dispose());
        ok.addActionListener(e->{
            String mat=fMat.getText().trim().toUpperCase(),nom=fNom.getText().trim();
            if(mat.isEmpty()||nom.isEmpty()){
                NotificationManager.show(parent,"Remplissez tous les champs.",NotificationManager.Type.WARNING);
                return;
            }
            int idx=combo.getSelectedIndex(); dlg.dispose(); ouvrirPaiement(mat,nom,JOURS[idx],PRIX[idx],LBLS[idx]);
        });
        fp.add(ok); fp.add(ann); root.add(fp,BorderLayout.SOUTH); dlg.setVisible(true);
    }

    private void ouvrirPaiement(String mat,String nom,int jours,double montant,String formule) {
        // CORRECTION: Utilisation de 'parent'
        JDialog pay=new JDialog(parent,"Confirmation Paiement",true);
        pay.setSize(460,300); pay.setLocationRelativeTo(this);
        pay.getContentPane().setBackground(AdminUI.BG);
        JPanel root=(JPanel)pay.getContentPane(); root.setLayout(new BorderLayout());
        root.add(AdminUI.buildHeader("Paiement",formule+" — "+(int)montant+" DH",AdminUI.GREEN,null),BorderLayout.NORTH);

        JPanel body=new JPanel(new GridLayout(4,2,10,10)); body.setOpaque(false); body.setBorder(new EmptyBorder(18,28,10,28));
        JComboBox<String> comboMode=AdminUI.createCombo(new String[]{"ESPECES","CARTE_BANCAIRE"});
        Object[][] rows2={{"Client :",nom},{"Matricule :",mat},{"Durée :",jours+" jours"},{"Mode :",comboMode}};
        for(Object[] r:rows2){
            JLabel l=new JLabel(r[0].toString());l.setForeground(AdminUI.TEXT_SEC);l.setFont(new Font("Segoe UI",Font.PLAIN,12));body.add(l);
            if(r[1] instanceof JComponent)body.add((JComponent)r[1]);
            else{JLabel v=new JLabel(r[1].toString());v.setForeground(AdminUI.TEXT);v.setFont(new Font("Segoe UI",Font.BOLD,12));body.add(v);}
        }
        root.add(body,BorderLayout.CENTER);

        JPanel fp=AdminUI.footerPanel();
        JLabel totalLbl=new JLabel("TOTAL : "+String.format("%.0f DH",montant)); totalLbl.setFont(new Font("Segoe UI",Font.BOLD,16)); totalLbl.setForeground(AdminUI.GOLD);
        JButton btnPay=AdminUI.createButton("Confirmer",AdminUI.GREEN,120,34);
        JButton btnAnn=AdminUI.createButton("Annuler",AdminUI.TEXT_SEC,90,34);
        btnAnn.addActionListener(e->pay.dispose());
        btnPay.addActionListener(e->{
            String mode=comboMode.getSelectedItem().toString(); String numTrans=mode.contains("CARTE")?"TXN-"+String.format("%08d",new Random().nextInt(99999999)):null;
            if(dao.ajouterAbonnement(mat,nom,jours,montant,mode,"PAYE",numTrans)){
                pay.dispose();
                NotificationManager.show(parent,"Abonnement enregistré !",NotificationManager.Type.SUCCESS);
                chargerDonnees();
            }
            else NotificationManager.show(parent,"Erreur base de données.",NotificationManager.Type.ERROR);
        });
        fp.add(totalLbl); fp.add(Box.createHorizontalStrut(20)); fp.add(btnPay); fp.add(btnAnn); root.add(fp,BorderLayout.SOUTH); pay.setVisible(true);
    }

    private void afficherRecu() {
        int row=table.getSelectedRow();
        if(row<0){NotificationManager.show(parent,"Sélectionnez un abonnement.",NotificationManager.Type.WARNING);return;}
        String info="— SMART PARKING — REÇU ABONNEMENT —\n\nMatricule : "+model.getValueAt(row,1)+"\nNom       : "+model.getValueAt(row,2)+"\nFormule   : "+model.getValueAt(row,3)+"\nExpire le : "+model.getValueAt(row,6)+"\nMontant   : "+String.format("%.0f DH",prixFormule(model.getValueAt(row,3).toString()));
        JTextArea ta=new JTextArea(info); ta.setFont(new Font("Courier New",Font.BOLD,12)); ta.setEditable(false); ta.setBackground(AdminUI.SURFACE); ta.setForeground(AdminUI.TEXT); ta.setBorder(new EmptyBorder(12,16,12,16));
        // CORRECTION: Utilisation de 'parent'
        JOptionPane.showMessageDialog(parent,new JScrollPane(ta),"Reçu d'abonnement",JOptionPane.PLAIN_MESSAGE);
    }

    private void supprimer() {
        int row=table.getSelectedRow();
        if(row<0){NotificationManager.show(parent,"Sélectionnez un abonnement.",NotificationManager.Type.WARNING);return;}
        String mat=model.getValueAt(row,1).toString(); int id=(int)model.getValueAt(row,0);
        // CORRECTION: Utilisation de 'parent'
        if(JOptionPane.showConfirmDialog(parent,"Supprimer l'abonnement de "+mat+" ?","Confirmer",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION&&dao.supprimerAbonnement(id)){
            NotificationManager.show(parent,"Abonnement supprimé.",NotificationManager.Type.SUCCESS); cartePanel.clearData(); chargerDonnees();
        }
    }

    // =========================================================================
    // CARTE MEMBRE (Adaptée au thème Luxe)
    // =========================================================================
    static class CartePanel extends JPanel {
        String nom="",mat="",formule="",fin="",statut="",mode=""; double prix=0; boolean ok=false;
        CartePanel(){setOpaque(false);setPreferredSize(new Dimension(380,220));}
        void setData(String n,String m,String f,String df,String st,double p,String md){nom=n;mat=m;formule=f;fin=df;statut=st;prix=p;mode=md;ok=true;repaint();}
        void clearData(){ok=false;repaint();}
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int cw=Math.min(getWidth()-16,420),ch=215,cx=(getWidth()-cw)/2,cy=(getHeight()-ch)/2;
            if(!ok){
                g2.setColor(AdminUI.SURFACE2); g2.fillRoundRect(cx,cy,cw,ch,18,18);
                g2.setColor(AdminUI.BORDER); g2.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,0,new float[]{7,4},0)); g2.drawRoundRect(cx,cy,cw,ch,18,18);
                g2.setFont(new Font("Segoe UI",Font.ITALIC,12)); g2.setColor(AdminUI.TEXT_SEC);
                String msg="Cliquez sur un abonné pour voir sa carte"; FontMetrics fm=g2.getFontMetrics(); g2.drawString(msg,cx+(cw-fm.stringWidth(msg))/2,cy+ch/2+4);
                g2.dispose(); return;
            }
            Color ac=accent();
            g2.setPaint(new GradientPaint(cx,cy,AdminUI.SURFACE2,cx+cw,cy+ch,AdminUI.alpha(ac,30))); g2.fillRoundRect(cx,cy,cw,ch,18,18);
            RadialGradientPaint rg=new RadialGradientPaint(new Point2D.Float(cx+cw*0.75f,cy+ch*0.3f),80,new float[]{0f,1f},new Color[]{AdminUI.alpha(ac,60),new Color(0,0,0,0)});
            g2.setPaint(rg); g2.fillOval(cx+(int)(cw*0.5f)-80,cy-40,200,200);
            g2.setColor(AdminUI.BORDER); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(cx,cy,cw,ch,18,18);

            g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(AdminUI.GOLD); g2.drawString("SMART PARKING  ·  PRESTIGE",cx+18,cy+20);
            g2.setColor(ac); g2.fillRoundRect(cx+cw-90,cy+9,78,20,10,10);
            g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(AdminUI.BG); FontMetrics fm=g2.getFontMetrics(); g2.drawString(formule,cx+cw-90+(78-fm.stringWidth(formule))/2,cy+22);

            g2.setFont(new Font("Courier New",Font.BOLD,18)); g2.setColor(AdminUI.TEXT); g2.drawString(mat,cx+18,cy+100);
            g2.setFont(new Font("Segoe UI",Font.BOLD,14)); g2.setColor(AdminUI.TEXT); g2.drawString(nom.toUpperCase(),cx+18,cy+130);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(AdminUI.TEXT_SEC); g2.drawString("EXPIRE LE",cx+18,cy+150);
            g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(AdminUI.TEXT); g2.drawString(fin,cx+18,cy+165);
            g2.setFont(new Font("Segoe UI",Font.BOLD,18)); g2.setColor(AdminUI.GOLD); fm=g2.getFontMetrics();
            String ms=String.format("%.0f DH",prix); g2.drawString(ms,cx+cw-fm.stringWidth(ms)-14,cy+165);
            boolean actif=!statut.startsWith("EXPIRÉ"); Color sc=actif?AdminUI.GREEN:AdminUI.RED;
            g2.setColor(AdminUI.alpha(sc,180)); g2.fillRoundRect(cx+cw-72,cy+ch-27,62,18,8,8);
            g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(AdminUI.BG); fm=g2.getFontMetrics();
            String st2=actif?"ACTIF":"EXPIRÉ"; g2.drawString(st2,cx+cw-72+(62-fm.stringWidth(st2))/2,cy+ch-14);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(AdminUI.TEXT_SEC); g2.drawString(mode,cx+18,cy+ch-14);
            g2.dispose();
        }
        private Color accent(){if(formule.contains("1 AN"))return AdminUI.PURPLE;if(formule.contains("3"))return AdminUI.GOLD;if(formule.contains("2"))return AdminUI.BLUE;return AdminUI.GREEN;}
    }
}