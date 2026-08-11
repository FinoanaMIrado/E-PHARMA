package pharmacie.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Facture d'un achat, telle qu'elle est présentée puis exportée en PDF.
 *
 * Regroupe les lignes partageant un même {@code numAchat} avec leur total.
 */
public class Facture {

    private final String numAchat;
    private final String nomClient;
    private final LocalDate date;
    private final List<Achat> lignes;

    public Facture(String numAchat, String nomClient, LocalDate date, List<Achat> lignes) {
        this.numAchat = numAchat;
        this.nomClient = nomClient;
        this.date = date;
        this.lignes = List.copyOf(lignes);
    }

    public String getNumAchat() {
        return numAchat;
    }

    public String getNomClient() {
        return nomClient;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<Achat> getLignes() {
        return lignes;
    }

    /** Total général : somme des totaux de chaque ligne. */
    public long getTotalGeneral() {
        return lignes.stream().mapToLong(Achat::getTotalLigne).sum();
    }
}
