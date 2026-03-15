package database;

import model.TypeVehicule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SCHÉMA RÉEL (phpMyAdmin) :
 *  places              : id_place | numero_place | type_place | est_disponible
 *  vehicules           : id_vehicule | immatriculation | type_vehicule | heure_entree | id_place
 *  historique_paiements: id_paiement | immatriculation | duree_minutes | montant_paye | date_sortie
 *  abonnements         : id_abonnement | matricule | nom_client | date_debut | date_fin |
 *                        montant | mode_paiement | statut_paiement | num_transaction
 */
public class ParkingDAO {

    // =========================================================================
    // 1. ENTRÉE
    // =========================================================================
    public boolean enregistrerEntree(String matricule, int idPlace, String typeVehicule) {
        String sqlV = "INSERT INTO vehicules (immatriculation, id_place, type_vehicule, heure_entree) VALUES (?, ?, ?, NOW())";
        String sqlP = "UPDATE places SET est_disponible = 0 WHERE id_place = ?";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psV = conn.prepareStatement(sqlV);
                 PreparedStatement psP = conn.prepareStatement(sqlP)) {
                psV.setString(1, matricule.toUpperCase());
                psV.setInt(2, idPlace);
                psV.setString(3, typeVehicule != null ? typeVehicule : "Voiture");
                psV.executeUpdate();
                psP.setInt(1, idPlace);
                psP.executeUpdate();
                conn.commit();
                return true;
            } catch (SQLException e) { conn.rollback(); e.printStackTrace(); }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // =========================================================================
    // 2. SORTIE
    // =========================================================================
    public double enregistrerSortie(String matricule) {
        if (aAbonnementActif(matricule)) return traiterSortieAbonne(matricule);
        String sql = "SELECT id_place, type_vehicule, " +
                "TIMESTAMPDIFF(MINUTE, heure_entree, NOW()) AS duree " +
                "FROM vehicules WHERE immatriculation = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricule.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int    idPlace = rs.getInt("id_place");
                long   duree   = rs.getLong("duree");
                String typeV   = rs.getString("type_vehicule");
                double montant = calculerMontant(duree, typeV);
                try (PreparedStatement p = conn.prepareStatement(
                        "UPDATE places SET est_disponible = 1 WHERE id_place = ?")) {
                    p.setInt(1, idPlace); p.executeUpdate();
                }
                try (PreparedStatement p = conn.prepareStatement(
                        "INSERT INTO historique_paiements (immatriculation, duree_minutes, montant_paye) VALUES (?,?,?)")) {
                    p.setString(1, matricule.toUpperCase()); p.setLong(2, duree); p.setDouble(3, montant); p.executeUpdate();
                }
                try (PreparedStatement p = conn.prepareStatement(
                        "DELETE FROM vehicules WHERE immatriculation = ?")) {
                    p.setString(1, matricule.toUpperCase()); p.executeUpdate();
                }
                return montant;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1.0;
    }

    private double traiterSortieAbonne(String matricule) {
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_place FROM vehicules WHERE immatriculation = ?")) {
                ps.setString(1, matricule.toUpperCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt("id_place");
                    try (PreparedStatement p = conn.prepareStatement(
                            "UPDATE places SET est_disponible = 1 WHERE id_place = ?")) {
                        p.setInt(1, id); p.executeUpdate();
                    }
                }
            }
            try (PreparedStatement p = conn.prepareStatement(
                    "DELETE FROM vehicules WHERE immatriculation = ?")) {
                p.setString(1, matricule.toUpperCase()); p.executeUpdate();
            }
            return 0.0;
        } catch (SQLException e) { e.printStackTrace(); return -1.0; }
    }

    // =========================================================================
    // 3. ÉTAT DU PARKING
    // =========================================================================
    public int[] getEtatParking() {
        String sql = "SELECT SUM(CASE WHEN est_disponible=1 THEN 1 ELSE 0 END) AS libres, " +
                "SUM(CASE WHEN est_disponible=0 THEN 1 ELSE 0 END) AS occupees FROM places";
        try (Connection conn = Database.getConnection();
             Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return new int[]{ rs.getInt("libres"), rs.getInt("occupees") };
        } catch (SQLException e) { e.printStackTrace(); }
        return new int[]{0, 0};
    }

    public ResultSet getToutesLesPlaces() {
        try {
            Connection conn = Database.getConnection();
            String sql = "SELECT p.id_place, p.numero_place, p.est_disponible, p.type_place, " +
                    "v.immatriculation, v.type_vehicule " +
                    "FROM places p LEFT JOIN vehicules v ON p.id_place = v.id_place " +
                    "ORDER BY CAST(p.numero_place AS UNSIGNED) ASC";
            return conn.createStatement().executeQuery(sql);
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }
    public String getNumeroPlaceParId(int idPlace) {
        String sql = "SELECT `numero place` FROM places WHERE id_place = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlace);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return "N/A";
    }

    // =========================================================================
    // 4. HISTORIQUE
    // =========================================================================
    public ResultSet getHistorique() {
        try {
            Connection conn = Database.getConnection();
            String sql = "SELECT id_paiement, immatriculation, duree_minutes, montant_paye, date_sortie " +
                    "FROM historique_paiements ORDER BY date_sortie DESC";
            return conn.createStatement().executeQuery(sql);
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    // =========================================================================
    // 5. STATISTIQUES
    // =========================================================================
    public double getRevenusParJour() {
        String sql = "SELECT COALESCE(SUM(montant_paye),0) FROM historique_paiements WHERE DATE(date_sortie)=CURDATE()";
        try (Connection conn = Database.getConnection();
             Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public int getNombreVehiculesDuJour() {
        String sql = "SELECT COUNT(*) FROM historique_paiements WHERE DATE(date_sortie)=CURDATE()";
        try (Connection conn = Database.getConnection();
             Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public List<double[]> getRevenus7Jours() {
        List<double[]> data = new ArrayList<>();
        String sql = "SELECT DATE(date_sortie) AS jour, SUM(montant_paye) AS total " +
                "FROM historique_paiements GROUP BY DATE(date_sortie) ORDER BY DATE(date_sortie) ASC LIMIT 7";
        try (Connection conn = Database.getConnection();
             Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                data.add(new double[]{ rs.getDate("jour").getTime(), rs.getDouble("total") });
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    public int[] getDistributionHoraire() {
        int[] heures = new int[24];
        String sql = "SELECT HOUR(date_sortie) AS h, COUNT(*) AS nb FROM historique_paiements GROUP BY HOUR(date_sortie)";
        try (Connection conn = Database.getConnection();
             Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) heures[rs.getInt("h")] = rs.getInt("nb");
        } catch (SQLException e) { e.printStackTrace(); }
        return heures;
    }

    // =========================================================================
    // 6. ABONNEMENTS
    // =========================================================================
    public ResultSet getTousAbonnements() {
        try {
            Connection conn = Database.getConnection();
            String sql = "SELECT id_abonnement, matricule, nom_client, date_debut, date_fin, " +
                    "montant, mode_paiement, statut_paiement, num_transaction, " +
                    "DATEDIFF(date_fin, NOW()) AS jours_restants " +
                    "FROM abonnements ORDER BY date_fin DESC";
            return conn.createStatement().executeQuery(sql);
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    public boolean ajouterAbonnement(String matricule, String nom, int jours,
                                     double montant, String mode, String statut, String transac) {
        String sql = "INSERT INTO abonnements (matricule, nom_client, date_debut, date_fin, " +
                "montant, mode_paiement, statut_paiement, num_transaction) " +
                "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricule.toUpperCase());
            ps.setString(2, nom);
            ps.setInt(3, jours);
            ps.setDouble(4, montant);
            ps.setString(5, mode);
            ps.setString(6, statut);
            ps.setString(7, transac);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean aAbonnementActif(String matricule) {
        String sql = "SELECT COUNT(*) FROM abonnements WHERE matricule=? AND date_fin>=NOW()";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricule.toUpperCase());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean supprimerAbonnement(int idAbonnement) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM abonnements WHERE id_abonnement = ?")) {
            ps.setInt(1, idAbonnement);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // =========================================================================
    // 7. RECHERCHE VÉHICULE
    // =========================================================================
    public String rechercherVehicule(String matricule) {
        String sql = "SELECT p.numero_place, v.heure_entree " +
                "FROM vehicules v JOIN places p ON v.id_place = p.id_place " +
                "WHERE v.immatriculation = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricule.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return "✅ Véhicule trouvé en Place " + rs.getString("numero_place") +
                        " (Entrée : " + rs.getTimestamp("heure_entree") + ")";
        } catch (SQLException e) { e.printStackTrace(); }
        return "❌ Aucun véhicule trouvé avec le matricule " + matricule;
    }

    // =========================================================================
    // 8. CALCUL MONTANT
    // =========================================================================
    public double calculerMontant(long dureeMinutes, String typeVehiculeStr) {
        if (dureeMinutes <= 15) return 0.0; // 15 min gratuites

        double tarifHoraire;
        try {
            // Récupération du tarif de base selon le type (MOTO, VOITURE, CAMION)
            model.TypeVehicule type = model.TypeVehicule.valueOf(typeVehiculeStr.toUpperCase());
            tarifHoraire = type.getTarifHoraire();
        } catch (Exception e) {
            tarifHoraire = 5.0; // Tarif par défaut (Voiture)
        }

        // Calcul simple : (minutes / 60) * tarif
        double montant = (dureeMinutes / 60.0) * tarifHoraire;

        // Arrondir à 2 décimales
        return Math.round(montant * 100.0) / 100.0;
    }
}