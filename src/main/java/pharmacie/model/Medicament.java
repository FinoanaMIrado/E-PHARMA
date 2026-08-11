package pharmacie.model;

/**
 * Un médicament de la pharmacie (table MEDICAMENT).
 *
 * Règle métier : à la création, le stock vaut toujours 0. Il n'évolue ensuite
 * que par les entrées de stock (augmentation) et les achats (diminution).
 */
public class Medicament {

    /** Seuil en dessous duquel un stock est considéré comme faible. */
    public static final int SEUIL_STOCK_FAIBLE = 5;

    private String numMedoc;
    private String design;
    private int prixUnitaire;
    private int stock;

    public Medicament() {
    }

    public Medicament(String numMedoc, String design, int prixUnitaire, int stock) {
        this.numMedoc = numMedoc;
        this.design = design;
        this.prixUnitaire = prixUnitaire;
        this.stock = stock;
    }

    public String getNumMedoc() {
        return numMedoc;
    }

    public void setNumMedoc(String numMedoc) {
        this.numMedoc = numMedoc;
    }

    public String getDesign() {
        return design;
    }

    public void setDesign(String design) {
        this.design = design;
    }

    public int getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(int prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    /** Vrai si le stock est strictement inférieur au seuil d'alerte. */
    public boolean isStockFaible() {
        return stock < SEUIL_STOCK_FAIBLE;
    }

    /** Vrai si le médicament n'est plus disponible. */
    public boolean isRupture() {
        return stock <= 0;
    }

    /** Libellé de l'état du stock, affiché dans les tableaux d'alerte. */
    public String getEtatStock() {
        if (isRupture()) return "Rupture";
        if (isStockFaible()) return "Stock faible";
        return "Normal";
    }

    /** Affichage utilisé dans les listes déroulantes de sélection. */
    @Override
    public String toString() {
        return numMedoc + " - " + design;
    }
}
