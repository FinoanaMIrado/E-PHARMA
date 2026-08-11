package pharmacie.service;

import pharmacie.dao.AchatDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.model.Medicament;
import pharmacie.util.BusinessException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static pharmacie.service.StockService.erreurTechnique;

/**
 * Indicateurs du tableau de bord et du menu BILAN.
 *
 * Tous les calculs sont délégués à la base de données (agrégats SQL) plutôt
 * qu'effectués en mémoire : les écrans restent réactifs même avec un historique
 * de ventes important.
 */
public class BilanService {

    /** Nombre de mois affichés dans l'histogramme des recettes. */
    public static final int NB_MOIS_HISTOGRAMME = 5;

    /** Nombre de médicaments affichés dans le classement des ventes. */
    public static final int TAILLE_TOP_VENTES = 5;

    private final AchatDAO achatDAO = new AchatDAO();
    private final MedicamentDAO medicamentDAO = new MedicamentDAO();

    /** Les quatre indicateurs des cartes du tableau de bord. */
    public record Indicateurs(int nombreMedicaments, int nombreStockFaible,
                              long recetteTotale, int nombreAchats) {
    }

    /** Recette d'un mois, pour l'histogramme. */
    public record RecetteMensuelle(YearMonth mois, long montant) {
    }

    /** Charge les indicateurs affichés sur les cartes du tableau de bord. */
    public Indicateurs chargerIndicateurs() throws BusinessException {
        try {
            return new Indicateurs(
                    medicamentDAO.count(),
                    medicamentDAO.countStockFaible(Medicament.SEUIL_STOCK_FAIBLE),
                    achatDAO.recetteTotale(),
                    achatDAO.countAchats());
        } catch (SQLException e) {
            throw erreurTechnique("le calcul des indicateurs", e);
        }
    }

    /** Recette totale accumulée par la pharmacie. */
    public long recetteTotale() throws BusinessException {
        try {
            return achatDAO.recetteTotale();
        } catch (SQLException e) {
            throw erreurTechnique("le calcul de la recette totale", e);
        }
    }

    /** Les cinq médicaments les plus vendus. */
    public List<AchatDAO.VenteParMedicament> topVentes() throws BusinessException {
        try {
            return achatDAO.topVentes(TAILLE_TOP_VENTES);
        } catch (SQLException e) {
            throw erreurTechnique("le calcul des meilleures ventes", e);
        }
    }

    /** Médicaments dont le stock est strictement inférieur à 5. */
    public List<Medicament> stockFaible() throws BusinessException {
        try {
            return medicamentDAO.findStockFaible(Medicament.SEUIL_STOCK_FAIBLE);
        } catch (SQLException e) {
            throw erreurTechnique("la lecture des stocks faibles", e);
        }
    }

    /**
     * Recettes des cinq derniers mois, mois courant inclus.
     *
     * Les mois sans aucune vente sont inclus avec un montant nul afin que
     * l'histogramme affiche toujours cinq colonnes contiguës.
     */
    public List<RecetteMensuelle> recettesDerniersMois() throws BusinessException {
        try {
            YearMonth moisCourant = YearMonth.from(LocalDate.now());
            YearMonth premierMois = moisCourant.minusMonths(NB_MOIS_HISTOGRAMME - 1L);

            Map<YearMonth, Long> recettes = achatDAO.recettesParMois(premierMois.atDay(1));

            List<RecetteMensuelle> resultat = new ArrayList<>(NB_MOIS_HISTOGRAMME);
            for (int i = 0; i < NB_MOIS_HISTOGRAMME; i++) {
                YearMonth mois = premierMois.plusMonths(i);
                resultat.add(new RecetteMensuelle(mois, recettes.getOrDefault(mois, 0L)));
            }
            return resultat;
        } catch (SQLException e) {
            throw erreurTechnique("le calcul des recettes mensuelles", e);
        }
    }
}
