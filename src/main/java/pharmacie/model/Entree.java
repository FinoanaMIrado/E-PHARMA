package pharmacie.model;

import java.time.LocalDate;

/**
 * Une entrée de stock (table ENTREE).
 *
 * La validation d'une entrée augmente le stock du médicament concerné :
 * {@code stock = stock + stockEntree}.
 */
public class Entree {

    private String numEntree;
    private String numMedoc;
    private int stockEntree;
    private LocalDate dateEntree;

    /** Désignation du médicament, renseignée par jointure pour l'affichage. */
    private String designMedicament;

    public Entree() {
    }

    public Entree(String numEntree, String numMedoc, int stockEntree, LocalDate dateEntree) {
        this.numEntree = numEntree;
        this.numMedoc = numMedoc;
        this.stockEntree = stockEntree;
        this.dateEntree = dateEntree;
    }

    public String getNumEntree() {
        return numEntree;
    }

    public void setNumEntree(String numEntree) {
        this.numEntree = numEntree;
    }

    public String getNumMedoc() {
        return numMedoc;
    }

    public void setNumMedoc(String numMedoc) {
        this.numMedoc = numMedoc;
    }

    public int getStockEntree() {
        return stockEntree;
    }

    public void setStockEntree(int stockEntree) {
        this.stockEntree = stockEntree;
    }

    public LocalDate getDateEntree() {
        return dateEntree;
    }

    public void setDateEntree(LocalDate dateEntree) {
        this.dateEntree = dateEntree;
    }

    public String getDesignMedicament() {
        return designMedicament;
    }

    public void setDesignMedicament(String designMedicament) {
        this.designMedicament = designMedicament;
    }
}
