package pharmacie.model;

/**
 * Une ligne du panier en cours de saisie dans l'écran « Nouvelle vente ».
 *
 * Objet de travail purement en mémoire : le panier n'est écrit en base que
 * lorsque la vente est validée, sous forme de lignes de la table ACHAT.
 */
public class LigneVente {

    private final String numMedoc;
    private final String design;
    private final int prixUnitaire;
    private int quantite;

    public LigneVente(String numMedoc, String design, int prixUnitaire, int quantite) {
        this.numMedoc = numMedoc;
        this.design = design;
        this.prixUnitaire = prixUnitaire;
        this.quantite = quantite;
    }

    public String getNumMedoc() {
        return numMedoc;
    }

    public String getDesign() {
        return design;
    }

    public int getPrixUnitaire() {
        return prixUnitaire;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    /** Ajoute une quantité à la ligne existante (même médicament ajouté deux fois). */
    public void ajouterQuantite(int supplement) {
        this.quantite += supplement;
    }

    /** Total de la ligne : prix unitaire x quantité. */
    public int getTotal() {
        return prixUnitaire * quantite;
    }
}
