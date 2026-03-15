package model;

import java.time.LocalDateTime;
import java.time.Duration;

public class Voiture implements CalculateurPrix {
    private String matricule;
    private LocalDateTime heureEntree;

    public Voiture(String matricule, LocalDateTime heureEntree) {
        this.matricule = matricule;
        this.heureEntree = heureEntree;
    }

    @Override
    public double calculer(long minutes) {
        // Calcul simple : 5 DH par heure, prorata par minute
        return (minutes / 60.0) * TARIF_HORAIRE;
    }

    // Getters
    public String getMatricule() { return matricule; }
    public LocalDateTime getHeureEntree() { return heureEntree; }
}