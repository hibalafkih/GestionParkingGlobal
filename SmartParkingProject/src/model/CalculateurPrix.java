package model;

public interface CalculateurPrix {
    double TARIF_HORAIRE = 5.0; // 5 DH/heure [selon votre sujet]

    // Méthode obligatoire à implémenter
    double calculer(long minutes);
}