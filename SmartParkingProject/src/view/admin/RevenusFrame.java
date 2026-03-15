package view.admin;

import database.Database;
import util.GlassTheme;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.io.*;

/**
 * RevenusFrame — Redesign Glassmorphism Vert.
 * Palette unifiée avec LoginFrame / AdminDashboard.
 */
public class RevenusFrame extends JDialog {

    private Frame parent;
    private JTabbedPane tabs;

    // Onglet 1
    private JLabel kpiJour, kpiSemaine, kpiMois, kpiAnnee, kpiVsHier, kpiVsMoisPrec;
    private GraphPanel graphPanel;

    // Onglet 2
    private JTable tableDetail;
    private DefaultTableModel modelDetail;
    private JComboBox<String> comboPeriode;

    // Onglet 3
    private GraphBarPanel graphVehicules;
    private JTable tableVehicules;
    private DefaultTableModel modelVehicules;

    public RevenusFrame(Frame parent) {
        super(parent, "Tableau de Bord — Revenus", true);
        this.parent = parent;
        setSize(1100, 700);
        setLocationRelativeTo(parent);
        GlassTheme.applyBackground(this, GlassTheme.GOLD, GlassTheme.GREEN);
        buildUI();
        chargerToutesDonnees();
    }

    private void buildUI() {
        JPanel root = (JPanel) getContentPane();
        root.setLayout(new BorderLayout(0, 0));
        root.add(buildHeader(), BorderLayout.NORTH);

        // Onglets stylisés glassmorphism
        tabs = new JTabbedPane() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(GlassTheme.GLASS_C); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tabs.setOpaque(false);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.setBackground(GlassTheme.BG1);
        tabs.setForeground(GlassTheme.TEXT);
        tabs.addTab("  Vue Générale  ",      buildTabVueGenerale());
        tabs.addTab("  Détail Période  ",    buildTabDetail());
        tabs.addTab("  Par Véhicule  ",      buildTabVehicules());
        tabs.addTab("  Comparaison  ",       buildTabComparaison());
        root.add(tabs, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JLabel clock = new JLabel();
        clock.setFont(new Font("Courier New", Font.BOLD, 13));
        clock.setForeground(GlassTheme.alpha(GlassTheme.GOLD, 220));

        // CORRECTION ICI : Ajout de javax.swing. devant Timer
        new javax.swing.Timer(1000, e -> clock.setText(new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss").format(new java.util.Date()))).start();

        clock.setText(new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss").format(new java.util.Date()));
        return GlassTheme.buildHeader(
                "Tableau de Bord Revenus",
                "Jour  ·  Semaine  ·  Mois  ·  Annee  ·  Comparaison",
                GlassTheme.GOLD, clock);
    }

    // ── Onglet 1 : Vue Générale ───────────────────────────────────────────────
    private JPanel buildTabVueGenerale() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false); p.setBorder(new EmptyBorder(16, 16, 10, 16));

        // KPI row
        JPanel kpiRow = new JPanel(new GridLayout(1, 6, 10, 0));
        kpiRow.setOpaque(false); kpiRow.setPreferredSize(new Dimension(0, 88));
        kpiJour       = GlassTheme.kpiCard("Aujourd'hui",    "-- DH", GlassTheme.GOLD);
        kpiSemaine    = GlassTheme.kpiCard("Cette Semaine",  "-- DH", GlassTheme.BLUE);
        kpiMois       = GlassTheme.kpiCard("Ce Mois",        "-- DH", GlassTheme.GREEN);
        kpiAnnee      = GlassTheme.kpiCard("Cette Année",    "-- DH", GlassTheme.PURPLE);
        kpiVsHier     = GlassTheme.kpiCard("vs Hier",        "--",    GlassTheme.TEAL);
        kpiVsMoisPrec = GlassTheme.kpiCard("vs Mois Préc.",  "--",    GlassTheme.ORANGE);
        for (JLabel kpi : new JLabel[]{kpiJour,kpiSemaine,kpiMois,kpiAnnee,kpiVsHier,kpiVsMoisPrec}) {
            JPanel w=new JPanel(new BorderLayout()); w.setOpaque(false); w.add(kpi); kpiRow.add(w);
        }
        p.add(kpiRow, BorderLayout.NORTH);

        // Graphique 30 jours
        JPanel gw = new JPanel(new BorderLayout(0, 8)); gw.setOpaque(false);
        gw.add(GlassTheme.sectionLabel("Revenus des 30 derniers jours", GlassTheme.GOLD), BorderLayout.NORTH);
        graphPanel = new GraphPanel();
        gw.add(graphPanel, BorderLayout.CENTER);
        p.add(gw, BorderLayout.CENTER);
        return p;
    }

    // ── Onglet 2 : Détail ─────────────────────────────────────────────────────
    private JPanel buildTabDetail() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setOpaque(false); p.setBorder(new EmptyBorder(14, 14, 0, 14));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8)); bar.setOpaque(false);
        JLabel lbl = new JLabel("Période :"); lbl.setFont(new Font("Segoe UI",Font.PLAIN,12)); lbl.setForeground(GlassTheme.TEXT_SEC);
        comboPeriode = GlassTheme.createCombo(new String[]{"Aujourd'hui","7 derniers jours","30 derniers jours","Ce mois","3 derniers mois","Cette année"});
        comboPeriode.setPreferredSize(new Dimension(200, 32));
        JButton btnFiltrer = GlassTheme.createButton("Appliquer", GlassTheme.GOLD, 110, 32);
        btnFiltrer.addActionListener(e -> chargerDetail(comboPeriode.getSelectedIndex()));
        bar.add(lbl); bar.add(comboPeriode); bar.add(btnFiltrer);
        p.add(bar, BorderLayout.NORTH);

        modelDetail = new DefaultTableModel(new String[]{"Date","Nb Véhicules","Revenus (DH)","Moy. / Véhicule","Abonnés (gratuit)"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        tableDetail = new JTable(modelDetail);
        GlassTheme.styleTable(tableDetail, GlassTheme.GOLD);
        tableDetail.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
                super.getTableCellRendererComponent(t,v,s,f,r,c);
                setBackground(s?GlassTheme.alpha(GlassTheme.GOLD,45):r%2==0?GlassTheme.alpha(GlassTheme.BG2,180):GlassTheme.alpha(GlassTheme.SURFACE,100));
                setHorizontalAlignment(RIGHT); setFont(new Font("Segoe UI",Font.BOLD,12)); setForeground(GlassTheme.GOLD);
                setBorder(new EmptyBorder(0,10,0,10)); return this;
            }
        });
        p.add(GlassTheme.scrollTable(tableDetail), BorderLayout.CENTER);

        JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10)); btns.setOpaque(false);
        JButton btnCSV=GlassTheme.createButton("Exporter CSV",GlassTheme.GREEN,150,34);
        JButton btnPDF=GlassTheme.createButton("Imprimer PDF",GlassTheme.PURPLE,150,34);
        btnCSV.addActionListener(e->exporterCSV()); btnPDF.addActionListener(e->imprimerPDF());
        btns.add(btnCSV); btns.add(btnPDF);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    // ── Onglet 3 : Par véhicule ───────────────────────────────────────────────
    private JPanel buildTabVehicules() {
        JPanel p=new JPanel(new GridLayout(1,2,14,0));
        p.setOpaque(false); p.setBorder(new EmptyBorder(14,14,10,14));

        JPanel left=new JPanel(new BorderLayout(0,8)); left.setOpaque(false);
        left.add(GlassTheme.sectionLabel("Revenus par Type de Véhicule (30 jours)",GlassTheme.GOLD),BorderLayout.NORTH);
        graphVehicules=new GraphBarPanel(); left.add(graphVehicules,BorderLayout.CENTER);

        JPanel right=new JPanel(new BorderLayout(0,8)); right.setOpaque(false);
        right.add(GlassTheme.sectionLabel("Détail par Type",GlassTheme.GOLD),BorderLayout.NORTH);
        modelVehicules=new DefaultTableModel(new String[]{"Type","Passages","Revenus (DH)","%","Durée Moy."},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        tableVehicules=new JTable(modelVehicules); GlassTheme.styleTable(tableVehicules,GlassTheme.GOLD); tableVehicules.setRowHeight(44);
        tableVehicules.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
                super.getTableCellRendererComponent(t,v,s,f,r,c);
                String type=v!=null?v.toString():"";
                Color col=type.equals("Voiture")?GlassTheme.BLUE:type.equals("Moto")?GlassTheme.GREEN:type.equals("Camion")?GlassTheme.ORANGE:GlassTheme.GOLD;
                setForeground(col); setFont(new Font("Segoe UI",Font.BOLD,13));
                setBackground(s?GlassTheme.alpha(GlassTheme.GOLD,45):r%2==0?GlassTheme.alpha(GlassTheme.BG2,180):GlassTheme.alpha(GlassTheme.SURFACE,100));
                setHorizontalAlignment(CENTER); setBorder(new EmptyBorder(0,10,0,10)); return this;
            }
        });
        right.add(GlassTheme.scrollTable(tableVehicules),BorderLayout.CENTER);
        p.add(left); p.add(right);
        return p;
    }

    // ── Onglet 4 : Comparaison ────────────────────────────────────────────────
    private JPanel buildTabComparaison() {
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setOpaque(false); p.setBorder(new EmptyBorder(16,16,10,16));

        JPanel cards=new JPanel(new GridLayout(2,3,12,12)); cards.setOpaque(false); cards.setPreferredSize(new Dimension(0,220));
        String[][] comparaisons={{"Aujourd'hui vs Hier","jour"},{"Cette Semaine vs Préc.","semaine"},{"Ce Mois vs Mois Préc.","mois"},{"Nb Véhicules / Jour","vehicules"},{"Revenu Moyen / Véhicule","moyen"},{"Meilleur Jour du Mois","meilleur"}};
        for(String[] comp:comparaisons) cards.add(compCard(comp[0],comp[1]));
        p.add(cards,BorderLayout.NORTH);

        JPanel bot=new JPanel(new BorderLayout(0,8)); bot.setOpaque(false);
        bot.add(GlassTheme.sectionLabel("Comparaison des 6 derniers mois",GlassTheme.GOLD),BorderLayout.NORTH);
        DefaultTableModel mComp=new DefaultTableModel(new String[]{"Mois","Revenus","Nb Véhicules","Moy./Véhicule","Croissance"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        JTable tComp=new JTable(mComp); GlassTheme.styleTable(tComp,GlassTheme.GOLD); tComp.setRowHeight(38);
        tComp.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
                super.getTableCellRendererComponent(t,v,s,f,r,c);
                String val=v!=null?v.toString():"";
                Color col=val.startsWith("+")?GlassTheme.GREEN:val.startsWith("-")?GlassTheme.RED:GlassTheme.TEXT_SEC;
                setForeground(col); setFont(new Font("Segoe UI",Font.BOLD,12)); setHorizontalAlignment(CENTER);
                setBackground(s?GlassTheme.alpha(GlassTheme.GOLD,45):r%2==0?GlassTheme.alpha(GlassTheme.BG2,180):GlassTheme.alpha(GlassTheme.SURFACE,100));
                setBorder(new EmptyBorder(0,10,0,10)); return this;
            }
        });
        chargerComparaisonMensuelle(mComp);
        bot.add(GlassTheme.scrollTable(tComp),BorderLayout.CENTER);
        p.add(bot,BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFooter() {
        JPanel f=GlassTheme.buildFooter();
        JButton btnActu=GlassTheme.createButton("Actualiser",GlassTheme.TEAL,130,36);
        JButton btnClose=GlassTheme.createButton("Fermer",GlassTheme.TEXT_SEC,100,36);
        btnActu.addActionListener(e->chargerToutesDonnees()); btnClose.addActionListener(e->dispose());
        f.add(btnActu); f.add(btnClose); return f;
    }

    // =========================================================================
    // DONNÉES
    // =========================================================================
    private void chargerToutesDonnees() {
        chargerKPI(); chargerGraphique30Jours(); chargerDetail(0); chargerVehicules();
    }

    private void chargerKPI() {
        try(Connection c=Database.getConnection()){
            double jour=rev(c,"DATE(date_sortie)=CURDATE()");
            double hier=rev(c,"DATE(date_sortie)=DATE_SUB(CURDATE(),INTERVAL 1 DAY)");
            double semaine=rev(c,"YEARWEEK(date_sortie,1)=YEARWEEK(NOW(),1)");
            double mois=rev(c,"YEAR(date_sortie)=YEAR(NOW()) AND MONTH(date_sortie)=MONTH(NOW())");
            double annee=rev(c,"YEAR(date_sortie)=YEAR(NOW())");
            double moisPrec=rev(c,"YEAR(date_sortie)=YEAR(DATE_SUB(NOW(),INTERVAL 1 MONTH)) AND MONTH(date_sortie)=MONTH(DATE_SUB(NOW(),INTERVAL 1 MONTH))");
            kpiJour.setText(String.format("%.2f DH",jour));
            kpiSemaine.setText(String.format("%.2f DH",semaine));
            kpiMois.setText(String.format("%.2f DH",mois));
            kpiAnnee.setText(String.format("%.2f DH",annee));
            if(hier>0){double d=((jour-hier)/hier)*100;kpiVsHier.setText(String.format("%+.1f%%",d));}
            else kpiVsHier.setText(jour>0?"+100%":"--");
            if(moisPrec>0){double d=((mois-moisPrec)/moisPrec)*100;kpiVsMoisPrec.setText(String.format("%+.1f%%",d));}
            else kpiVsMoisPrec.setText(mois>0?"+100%":"--");
        }catch(SQLException e){e.printStackTrace();}
    }

    private double rev(Connection c,String where) throws SQLException {
        ResultSet rs=c.createStatement().executeQuery("SELECT COALESCE(SUM(montant_paye),0) FROM historique_paiements WHERE "+where);
        return rs.next()?rs.getDouble(1):0;
    }

    private void chargerGraphique30Jours() {
        List<double[]> data=new ArrayList<>();
        try(Connection c=Database.getConnection();ResultSet rs=c.createStatement().executeQuery(
                "SELECT DATE(date_sortie) as j, SUM(montant_paye) as t FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY) GROUP BY DATE(date_sortie) ORDER BY j ASC")){
            while(rs.next()) data.add(new double[]{rs.getDate("j").getTime(),rs.getDouble("t")});
        }catch(SQLException e){e.printStackTrace();}
        graphPanel.setData(data);
    }

    private void chargerDetail(int idx) {
        modelDetail.setRowCount(0);
        String groupBy,where;
        switch(idx){
            case 0:where="DATE(date_sortie)=CURDATE()";groupBy="HOUR(date_sortie)";break;
            case 1:where="date_sortie>=DATE_SUB(NOW(),INTERVAL 7 DAY)";groupBy="DATE(date_sortie)";break;
            case 2:where="date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY)";groupBy="DATE(date_sortie)";break;
            case 3:where="YEAR(date_sortie)=YEAR(NOW()) AND MONTH(date_sortie)=MONTH(NOW())";groupBy="DATE(date_sortie)";break;
            case 4:where="date_sortie>=DATE_SUB(NOW(),INTERVAL 90 DAY)";groupBy="DATE(date_sortie)";break;
            default:where="YEAR(date_sortie)=YEAR(NOW())";groupBy="MONTH(date_sortie)";
        }
        try(Connection c=Database.getConnection();ResultSet rs=c.createStatement().executeQuery(
                "SELECT "+groupBy+" as p, COUNT(*) as nb, SUM(montant_paye) as rev, AVG(montant_paye) as moy FROM historique_paiements WHERE "+where+" GROUP BY "+groupBy+" ORDER BY "+groupBy+" DESC")){
            while(rs.next()) modelDetail.addRow(new Object[]{rs.getString("p"),rs.getInt("nb"),String.format("%.2f DH",rs.getDouble("rev")),String.format("%.2f DH",rs.getDouble("moy")),"--"});
        }catch(SQLException e){e.printStackTrace();}
    }

    private void chargerVehicules() {
        modelVehicules.setRowCount(0);
        List<String> labels=new ArrayList<>(); List<Double> values=new ArrayList<>(); List<Color> colors=new ArrayList<>();
        double totalRev=0; List<double[]> rows=new ArrayList<>();
        try(Connection c=Database.getConnection();ResultSet rs=c.createStatement().executeQuery(
                "SELECT type_vehicule, COUNT(*) as nb, SUM(montant_paye) as rev, AVG(duree_minutes) as duree FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY) GROUP BY type_vehicule ORDER BY rev DESC")){
            while(rs.next()){
                double r=rs.getDouble("rev"); totalRev+=r;
                rows.add(new double[]{rs.getDouble("nb"),r,rs.getDouble("duree")});
                labels.add(rs.getString("type_vehicule")); values.add(r);
                switch(rs.getString("type_vehicule")){case "Voiture":colors.add(GlassTheme.BLUE);break;case "Moto":colors.add(GlassTheme.GREEN);break;case "Camion":colors.add(GlassTheme.ORANGE);break;default:colors.add(GlassTheme.GOLD);}
            }
        }catch(SQLException e){e.printStackTrace();}
        for(int i=0;i<rows.size();i++){double[] r=rows.get(i);double pct=totalRev>0?(r[1]/totalRev)*100:0;modelVehicules.addRow(new Object[]{labels.get(i),(int)r[0],String.format("%.2f DH",r[1]),String.format("%.1f%%",pct),String.format("%.0f min",r[2])});}
        graphVehicules.setData(labels,values,colors);
    }

    private void chargerComparaisonMensuelle(DefaultTableModel m) {
        try(Connection c=Database.getConnection();ResultSet rs=c.createStatement().executeQuery(
                "SELECT DATE_FORMAT(date_sortie,'%Y-%m') as mois, SUM(montant_paye) as rev, COUNT(*) as nb, AVG(montant_paye) as moy FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 6 MONTH) GROUP BY DATE_FORMAT(date_sortie,'%Y-%m') ORDER BY mois DESC")){
            double prevRev=-1;
            while(rs.next()){double rev=rs.getDouble("rev");String cr="--";if(prevRev>0){double d=((prevRev-rev)/rev)*100;cr=String.format("%+.1f%%",d);}m.addRow(new Object[]{rs.getString("mois"),String.format("%.2f DH",rev),rs.getInt("nb"),String.format("%.2f DH",rs.getDouble("moy")),cr});prevRev=rev;}
        }catch(SQLException e){e.printStackTrace();}
    }

    // ── Carte comparaison ─────────────────────────────────────────────────────
    private JPanel compCard(String titre, String type) {
        JPanel card=new JPanel(null){
            String val1="--",val2="--"; double pct=0;
            {SwingUtilities.invokeLater(()->{
                try(Connection c=Database.getConnection()){
                    switch(type){
                        case "jour": val1=String.format("%.2f DH",rev(c,"DATE(date_sortie)=CURDATE()")); double h=rev(c,"DATE(date_sortie)=DATE_SUB(CURDATE(),INTERVAL 1 DAY)"); val2=String.format("%.2f DH",h); if(h>0) pct=((rev(c,"DATE(date_sortie)=CURDATE()")-h)/h)*100; break;
                        case "semaine": val1=String.format("%.2f DH",rev(c,"YEARWEEK(date_sortie,1)=YEARWEEK(NOW(),1)")); double sw=rev(c,"YEARWEEK(date_sortie,1)=YEARWEEK(NOW(),1)-1"); val2=String.format("%.2f DH",sw); if(sw>0) pct=((rev(c,"YEARWEEK(date_sortie,1)=YEARWEEK(NOW(),1)")-sw)/sw)*100; break;
                        case "mois": double mo=rev(c,"YEAR(date_sortie)=YEAR(NOW()) AND MONTH(date_sortie)=MONTH(NOW())"); double mp=rev(c,"YEAR(date_sortie)=YEAR(DATE_SUB(NOW(),INTERVAL 1 MONTH)) AND MONTH(date_sortie)=MONTH(DATE_SUB(NOW(),INTERVAL 1 MONTH))"); val1=String.format("%.2f DH",mo); val2=String.format("%.2f DH",mp); if(mp>0) pct=((mo-mp)/mp)*100; break;
                        case "vehicules": ResultSet r=c.createStatement().executeQuery("SELECT COALESCE(AVG(nb),0) FROM (SELECT DATE(date_sortie),COUNT(*) as nb FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY) GROUP BY DATE(date_sortie)) t"); val1=r.next()?String.format("%.1f veh/j",r.getDouble(1)):"--"; val2="30 jours"; break;
                        case "moyen": ResultSet r2=c.createStatement().executeQuery("SELECT COALESCE(AVG(montant_paye),0) FROM historique_paiements WHERE date_sortie>=DATE_SUB(NOW(),INTERVAL 30 DAY)"); val1=r2.next()?String.format("%.2f DH",r2.getDouble(1)):"--"; val2="30 jours"; break;
                        case "meilleur": ResultSet r3=c.createStatement().executeQuery("SELECT DATE(date_sortie) as j, SUM(montant_paye) as t FROM historique_paiements WHERE MONTH(date_sortie)=MONTH(NOW()) GROUP BY j ORDER BY t DESC LIMIT 1"); if(r3.next()){val1=new SimpleDateFormat("dd/MM").format(r3.getDate("j")); val2=String.format("%.2f DH",r3.getDouble("t"));} break;
                    }
                }catch(Exception ex){ex.printStackTrace();}
                repaint();
            });}
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GlassTheme.GLASS_C); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setPaint(new GradientPaint(0,0,GlassTheme.GLASSBR,0,getHeight()/2f,new Color(0,0,0,0))); g2.fillRoundRect(0,0,getWidth(),getHeight()/2,14,14);
                g2.setPaint(new GradientPaint(0,0,GlassTheme.GOLD,getWidth(),0,GlassTheme.alpha(GlassTheme.GOLD,60))); g2.fillRoundRect(0,0,getWidth(),3,3,3);
                g2.setColor(GlassTheme.BORDER_C); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(GlassTheme.TEXT_SEC);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(titre,(getWidth()-fm.stringWidth(titre))/2,22);
                g2.setFont(new Font("Segoe UI",Font.BOLD,15)); g2.setColor(GlassTheme.GOLD); fm=g2.getFontMetrics(); g2.drawString(val1,(getWidth()-fm.stringWidth(val1))/2,50);
                if(!val2.equals("--")){g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(GlassTheme.TEXT_SEC); fm=g2.getFontMetrics(); g2.drawString("Préc: "+val2,(getWidth()-fm.stringWidth("Préc: "+val2))/2,66);}
                if(pct!=0){String ps=String.format("%+.1f%%",pct); g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(pct>=0?GlassTheme.GREEN:GlassTheme.RED); fm=g2.getFontMetrics(); g2.drawString(ps,(getWidth()-fm.stringWidth(ps))/2,84);}
                g2.dispose();
            }
        };
        card.setOpaque(false); return card;
    }

    // ── Export ────────────────────────────────────────────────────────────────
    private void exporterCSV() {
        if(modelDetail.getRowCount()==0){NotificationManager.show(parent,"Aucune donnée à exporter.",NotificationManager.Type.WARNING);return;}
        JFileChooser fc=new JFileChooser(); fc.setSelectedFile(new File("revenus_parking.csv"));
        if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        try(PrintWriter pw=new PrintWriter(new FileWriter(fc.getSelectedFile()))){
            pw.println("Date,Nb Vehicules,Revenus (DH),Moy. par Vehicule,Abonnes");
            for(int i=0;i<modelDetail.getRowCount();i++){StringBuilder row=new StringBuilder();for(int j=0;j<modelDetail.getColumnCount();j++){if(j>0) row.append(",");row.append(modelDetail.getValueAt(i,j));}pw.println(row);}
            NotificationManager.show(parent,"Export CSV réussi !",NotificationManager.Type.SUCCESS);
        }catch(IOException e){NotificationManager.show(parent,"Erreur export.",NotificationManager.Type.ERROR);}
    }

    private void imprimerPDF() {
        PrinterJob job=PrinterJob.getPrinterJob();
        job.setPrintable((g,pf,pi)->{
            if(pi>0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2=(Graphics2D)g; g2.translate(pf.getImageableX(),pf.getImageableY());
            g2.setFont(new Font("Segoe UI",Font.BOLD,16)); g2.setColor(Color.BLACK); g2.drawString("SMART PARKING — RAPPORT REVENUS",60,30);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.drawString("Généré le : "+new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()),60,50);
            g2.drawLine(60,55,(int)pf.getImageableWidth()-60,55);
            String[] cols={"Date","Nb Véhicules","Revenus","Moy./Véhicule"}; int[] x={60,160,270,380}; int y=75;
            g2.setFont(new Font("Segoe UI",Font.BOLD,10)); for(int i=0;i<cols.length;i++) g2.drawString(cols[i],x[i],y);
            g2.drawLine(60,y+4,(int)pf.getImageableWidth()-60,y+4);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); y+=18;
            for(int i=0;i<Math.min(modelDetail.getRowCount(),35);i++){for(int j=0;j<4&&j<modelDetail.getColumnCount();j++) g2.drawString(String.valueOf(modelDetail.getValueAt(i,j)),x[j],y); y+=14; if(y>pf.getImageableHeight()-40) break;}
            return Printable.PAGE_EXISTS;
        });
        if(job.printDialog()){try{job.print();NotificationManager.show(parent,"Rapport envoyé à l'imprimante.",NotificationManager.Type.SUCCESS);}catch(PrinterException e){NotificationManager.show(parent,"Erreur impression.",NotificationManager.Type.ERROR);}}
    }

    // =========================================================================
    // GRAPHIQUES
    // =========================================================================
    class GraphPanel extends JPanel {
        List<double[]> data=new ArrayList<>();
        void setData(List<double[]> d){this.data=d;repaint();}
        {setOpaque(false);setBorder(BorderFactory.createLineBorder(GlassTheme.BORDER_C));}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            // Fond glass
            g2.setColor(GlassTheme.GLASS_C); g2.fillRect(0,0,getWidth(),getHeight());
            if(data==null||data.isEmpty()){g2.setColor(GlassTheme.TEXT_SEC);g2.setFont(new Font("Segoe UI",Font.ITALIC,12));g2.drawString("Aucune donnée disponible",getWidth()/2-80,getHeight()/2);g2.dispose();return;}
            int pad=50,w=getWidth()-pad*2,h=getHeight()-pad*2;
            double maxV=data.stream().mapToDouble(d->d[1]).max().orElse(1); if(maxV==0) maxV=1;
            // Grille
            g2.setStroke(new BasicStroke(0.5f));
            for(int i=0;i<=4;i++){int y=pad+h-i*(h/4); g2.setColor(GlassTheme.alpha(GlassTheme.BORDER_C,60)); g2.drawLine(pad,y,pad+w,y); g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(GlassTheme.TEXT_SEC); g2.drawString(String.format("%.0f",maxV*i/4),4,y+4);}
            int n=data.size(); int[] xs=new int[n],ys=new int[n];
            for(int i=0;i<n;i++){xs[i]=pad+i*w/(n>1?n-1:1); ys[i]=pad+h-(int)(data.get(i)[1]/maxV*h);}
            // Aire dégradée sous courbe
            int[] polyX=new int[n+2],polyY=new int[n+2];
            System.arraycopy(xs,0,polyX,0,n); System.arraycopy(ys,0,polyY,0,n);
            polyX[n]=pad+w; polyY[n]=pad+h; polyX[n+1]=pad; polyY[n+1]=pad+h;
            g2.setPaint(new GradientPaint(0,pad,GlassTheme.alpha(GlassTheme.GOLD,60),0,pad+h,GlassTheme.alpha(GlassTheme.GOLD,5)));
            g2.fillPolygon(polyX,polyY,n+2);
            // Courbe
            g2.setColor(GlassTheme.GOLD); g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            for(int i=0;i<n-1;i++) g2.drawLine(xs[i],ys[i],xs[i+1],ys[i+1]);
            // Points
            for(int i=0;i<n;i++){g2.setColor(GlassTheme.GOLD2); g2.fillOval(xs[i]-4,ys[i]-4,8,8); g2.setColor(GlassTheme.BG2); g2.fillOval(xs[i]-2,ys[i]-2,4,4);}
            // Labels X
            g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(GlassTheme.TEXT_SEC);
            for(int i=0;i<n;i+=Math.max(1,n/8)){String lbl=new SimpleDateFormat("dd/MM").format(new java.util.Date((long)data.get(i)[0])); g2.drawString(lbl,xs[i]-12,pad+h+14);}
            g2.dispose();
        }
    }

    class GraphBarPanel extends JPanel {
        List<String> labels=new ArrayList<>(); List<Double> values=new ArrayList<>(); List<Color> colors=new ArrayList<>();
        void setData(List<String> l,List<Double> v,List<Color> c){labels=l;values=v;colors=c;repaint();}
        {setOpaque(false);setBorder(BorderFactory.createLineBorder(GlassTheme.BORDER_C));}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(GlassTheme.GLASS_C); g2.fillRect(0,0,getWidth(),getHeight());
            if(values==null||values.isEmpty()){g2.setColor(GlassTheme.TEXT_SEC);g2.setFont(new Font("Segoe UI",Font.ITALIC,12));g2.drawString("Aucune donnée",getWidth()/2-50,getHeight()/2);g2.dispose();return;}
            int pad=50,w=getWidth()-pad*2,h=getHeight()-pad*2;
            double max=values.stream().mapToDouble(Double::doubleValue).max().orElse(1); if(max==0) max=1;
            int n=values.size(),bw=w/Math.max(n,1)-12;
            for(int i=0;i<n;i++){
                int bh=(int)(values.get(i)/max*h),x=pad+i*(w/n)+6,y=pad+h-bh;
                Color c=i<colors.size()?colors.get(i):GlassTheme.GOLD;
                g2.setPaint(new GradientPaint(x,y,c,x,y+bh,GlassTheme.alpha(c,80))); g2.fillRoundRect(x,y,bw,bh,8,8);
                g2.setColor(GlassTheme.alpha(c,160)); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(x,y,bw,bh,8,8);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(GlassTheme.TEXT_SEC); String lbl=i<labels.size()?labels.get(i):""; FontMetrics fm=g2.getFontMetrics(); g2.drawString(lbl,x+(bw-fm.stringWidth(lbl))/2,pad+h+14);
                g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(c); String val=String.format("%.0f",values.get(i)); fm=g2.getFontMetrics(); g2.drawString(val,x+(bw-fm.stringWidth(val))/2,y-4);
            }
            g2.setColor(GlassTheme.alpha(GlassTheme.BORDER_C,60)); g2.setStroke(new BasicStroke(0.5f));
            for(int i=0;i<=4;i++){int y=pad+h-i*(h/4);g2.drawLine(pad,y,pad+w,y);}
            g2.dispose();
        }
    }
}