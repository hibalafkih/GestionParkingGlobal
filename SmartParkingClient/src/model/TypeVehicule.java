package model;

public enum TypeVehicule {
    MOTO(3.0),      // 3 DH / heure
    VOITURE(5.0),   // 5 DH / heure
    CAMION(10.0);   // 10 DH / heure

    private double tarifHoraire;

    TypeVehicule(double tarifHoraire) {
        this.tarifHoraire = tarifHoraire;
    }

    public double getTarifHoraire() {
        return tarifHoraire;
    }
}