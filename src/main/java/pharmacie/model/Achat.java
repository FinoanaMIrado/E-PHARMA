package pharmacie.model;

import java.time.LocalDate;

/**
 * Une ligne d'achat (table ACHAT).
 *
 * Un même achat (identifié par {@code numAchat}) peut contenir plusieurs
 * médicaments : chaque médicament du panier donne lieu à une ligne distincte
 * partageant le même numéro d'achat, le même client et la même date.
 *
 * Le prix unitaire est figé au moment de la vente afin qu'une modification
 * ultérieure du tarif ne fausse pas les factures et les recettes déjà établies.
 */
public class Achat {

    private String numAchat;
    private String numMedoc;
    private String nomClient;
    private int nbr;
    private LocalDate dateAchat;
    private int prixUnitaire;

    /** Désignation du médicament, renseignée par jointure pour l'affichage. */
    private String designMedicament;

    public Achat() {
    }

    public Achat(String numAchat, String numMedoc, String nomClient,
                 int nbr, LocalDate dateAchat, int prixUnitaire) {
        this.numAchat = numAchat;
        this.numMedoc = numMedoc;
        this.nomClient = nomClient;
        this.nbr = nbr;
        this.dateAchat = dateAchat;
        this.prixUnitaire = prixUnitaire;
    }

    public String getNumAchat() {
        return numAchat;
    }

    public void setNumAchat(String numAchat) {
        this.numAchat = numAchat;
    }

    public String getNumMedoc() {
        return numMedoc;
    }

    public void setNumMedoc(String numMedoc) {
        this.numMedoc = numMedoc;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    public int getNbr() {
        return nbr;
    }

    public void setNbr(int nbr) {
        this.nbr = nbr;
    }

    public LocalDate getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(LocalDate dateAchat) {
        this.dateAchat = dateAchat;
    }

    public int getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(int prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public String getDesignMedicament() {
        return designMedicament;
    }

    public void setDesignMedicament(String designMedicament) {
        this.designMedicament = designMedicament;
    }

    /** Total de la ligne : prix unitaire x quantité. */
    public int getTotalLigne() {
        return prixUnitaire * nbr;
    }
}
