package pharmacie.service;

import pharmacie.dao.AchatDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.model.Achat;
import pharmacie.model.Facture;
import pharmacie.model.LigneVente;
import pharmacie.model.Medicament;
import pharmacie.util.BusinessException;
import pharmacie.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static pharmacie.service.StockService.dateValide;
import static pharmacie.service.StockService.erreurTechnique;
import static pharmacie.service.StockService.estViolationContrainte;
import static pharmacie.service.StockService.obligatoire;
import static pharmacie.service.StockService.quantiteValide;

/**
 * Règles métier du menu ACHAT.
 *
 * Un achat peut porter sur plusieurs médicaments : chaque ligne du panier
 * devient une ligne de la table ACHAT partageant le même numéro d'achat.
 *
 * Le contrôle du stock est appliqué à deux niveaux :
 * <ul>
 *   <li>à l'ajout au panier, pour prévenir l'utilisateur immédiatement ;</li>
 *   <li>à la validation, dans une transaction, car le stock a pu changer
 *       entre-temps. La mise à jour SQL refuse tout stock négatif, ce qui rend
 *       la règle « le stock ne devient jamais négatif » infranchissable.</li>
 * </ul>
 */
public class AchatService {

    private final AchatDAO achatDAO = new AchatDAO();
    private final MedicamentDAO medicamentDAO = new MedicamentDAO();

    // ==================================================================
    //  Nouvelle vente
    // ==================================================================

    /**
     * Vérifie qu'une quantité peut être ajoutée au panier pour ce médicament.
     *
     * @param dejaAuPanier quantité déjà présente dans le panier pour ce médicament
     * @throws BusinessException si le stock disponible est insuffisant
     */
    public void verifierDisponibilite(Medicament medicament, int quantite, int dejaAuPanier)
            throws BusinessException {
        if (medicament == null) {
            throw new BusinessException("Sélectionnez un médicament.");
        }
        int total = dejaAuPanier + quantite;
        if (total > medicament.getStock()) {
            throw new BusinessException(
                    "Stock insuffisant pour « " + medicament.getDesign() + " ».\n"
                            + "Stock disponible : " + medicament.getStock() + ".\n"
                            + (dejaAuPanier > 0
                            ? "Déjà dans le panier : " + dejaAuPanier + "."
                            : "Quantité demandée : " + quantite + "."));
        }
    }

    /** Propose le prochain numéro d'achat au format {@code Annn}. */
    public String prochainNumeroAchat() throws BusinessException {
        try {
            // Le numéro est calculé à partir du plus grand identifiant existant.
            for (int suffixe = 1; suffixe < 100000; suffixe++) {
                String candidat = String.format("A%03d", suffixe);
                if (!achatDAO.existsNumAchat(candidat)) {
                    return candidat;
                }
            }
            throw new BusinessException("Impossible de générer un nouveau numéro d'achat.");
        } catch (SQLException e) {
            throw erreurTechnique("le calcul du numéro d'achat", e);
        }
    }

    /**
     * Valide une vente : enregistre chaque ligne du panier et décrémente les stocks.
     *
     * L'ensemble est exécuté dans une transaction : soit toutes les lignes sont
     * enregistrées et tous les stocks mis à jour, soit rien n'est modifié.
     *
     * @return la facture correspondant à la vente enregistrée
     */
    public Facture validerVente(String numAchat, String nomClient, LocalDate date, List<LigneVente> panier)
            throws BusinessException {
        String num = obligatoire(numAchat, "Le numéro d'achat est obligatoire.");
        String client = obligatoire(nomClient, "Le nom du client est obligatoire.");
        LocalDate dateAchat = dateValide(date);

        if (panier == null || panier.isEmpty()) {
            throw new BusinessException("Le panier est vide : ajoutez au moins un médicament.");
        }

        Connection connexion = null;
        try {
            if (achatDAO.existsNumAchat(num)) {
                throw new BusinessException("Le numéro d'achat « " + num + " » est déjà utilisé.");
            }

            connexion = DatabaseConnection.getConnection();
            connexion.setAutoCommit(false);
            try {
                List<Achat> lignesEnregistrees = new ArrayList<>();

                for (LigneVente ligne : panier) {
                    // Le stock est relu dans la transaction : il a pu changer depuis
                    // la constitution du panier.
                    Medicament medicament = medicamentDAO.findById(ligne.getNumMedoc());
                    if (medicament == null) {
                        throw new BusinessException(
                                "Le médicament « " + ligne.getDesign() + " » n'existe plus.");
                    }
                    if (ligne.getQuantite() > medicament.getStock()) {
                        throw new BusinessException(
                                "Stock insuffisant pour « " + medicament.getDesign() + " ».\n"
                                        + "Stock disponible : " + medicament.getStock() + ".\n"
                                        + "Quantité demandée : " + ligne.getQuantite() + ".");
                    }

                    // Règle 19.3 : stock = stock - quantité achetée.
                    if (!medicamentDAO.ajusterStock(ligne.getNumMedoc(), -ligne.getQuantite())) {
                        throw new BusinessException(
                                "Stock insuffisant pour « " + medicament.getDesign() + " ».");
                    }

                    Achat achat = new Achat(num, ligne.getNumMedoc(), client,
                            ligne.getQuantite(), dateAchat, ligne.getPrixUnitaire());
                    achat.setDesignMedicament(ligne.getDesign());
                    achatDAO.insert(achat);
                    lignesEnregistrees.add(achat);
                }

                connexion.commit();
                return new Facture(num, client, dateAchat, lignesEnregistrees);
            } catch (SQLException | BusinessException e) {
                connexion.rollback();
                throw e;
            } finally {
                connexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw erreurTechnique("l'enregistrement de l'achat", e);
        }
    }

    // ==================================================================
    //  Historique
    // ==================================================================

    /** Liste toutes les lignes d'achat. */
    public List<Achat> listerAchats() throws BusinessException {
        try {
            return achatDAO.findAll();
        } catch (SQLException e) {
            throw erreurTechnique("la lecture des achats", e);
        }
    }

    /** Recherche les achats par nom de client ; retourne tout si le texte est vide. */
    public List<Achat> rechercherParClient(String nomClient) throws BusinessException {
        try {
            if (nomClient == null || nomClient.isBlank()) return achatDAO.findAll();
            return achatDAO.searchByClient(nomClient.trim());
        } catch (SQLException e) {
            throw erreurTechnique("la recherche des achats", e);
        }
    }

    /** Construit la facture d'un achat existant. */
    public Facture chargerFacture(String numAchat) throws BusinessException {
        try {
            List<Achat> lignes = achatDAO.findByNumAchat(numAchat);
            if (lignes.isEmpty()) {
                throw new BusinessException("Aucune ligne trouvée pour l'achat « " + numAchat + " ».");
            }
            Achat premiere = lignes.get(0);
            return new Facture(numAchat, premiere.getNomClient(), premiere.getDateAchat(), lignes);
        } catch (SQLException e) {
            throw erreurTechnique("la lecture de la facture", e);
        }
    }

    /**
     * Modifie une ligne d'achat existante et répercute l'écart sur le stock.
     *
     * Augmenter la quantité vendue retire du stock, la diminuer le restitue.
     */
    public void modifierLigneAchat(String numAchat, String numMedoc, String nomClient,
                                   String quantiteTexte, LocalDate date) throws BusinessException {
        String num = obligatoire(numAchat, "Sélectionnez l'achat à modifier.");
        String medoc = obligatoire(numMedoc, "Sélectionnez le médicament.");
        String client = obligatoire(nomClient, "Le nom du client est obligatoire.");
        int quantite = quantiteValide(quantiteTexte);
        LocalDate dateAchat = dateValide(date);

        Connection connexion = null;
        try {
            Achat ancienne = achatDAO.findLigne(num, medoc);
            if (ancienne == null) {
                throw new BusinessException("Cette ligne d'achat n'existe plus.");
            }

            connexion = DatabaseConnection.getConnection();
            connexion.setAutoCommit(false);
            try {
                // Vendre davantage retire du stock : la variation est l'opposé de l'écart.
                int ecart = quantite - ancienne.getNbr();
                if (ecart != 0 && !medicamentDAO.ajusterStock(medoc, -ecart)) {
                    Medicament medicament = medicamentDAO.findById(medoc);
                    int disponible = medicament == null ? 0 : medicament.getStock();
                    throw new BusinessException(
                            "Stock insuffisant pour augmenter cette quantité.\n"
                                    + "Stock disponible : " + disponible + ".");
                }

                Achat modifiee = new Achat(num, medoc, client, quantite, dateAchat, ancienne.getPrixUnitaire());
                achatDAO.update(modifiee);
                connexion.commit();
            } catch (SQLException | BusinessException e) {
                connexion.rollback();
                throw e;
            } finally {
                connexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw erreurTechnique("la modification de l'achat", e);
        }
    }

    /** Supprime une ligne d'achat et restitue la quantité au stock. */
    public void supprimerLigneAchat(String numAchat, String numMedoc) throws BusinessException {
        String num = obligatoire(numAchat, "Sélectionnez l'achat à supprimer.");
        String medoc = obligatoire(numMedoc, "Sélectionnez le médicament.");

        Connection connexion = null;
        try {
            Achat ligne = achatDAO.findLigne(num, medoc);
            if (ligne == null) {
                throw new BusinessException("Cette ligne d'achat n'existe plus.");
            }

            connexion = DatabaseConnection.getConnection();
            connexion.setAutoCommit(false);
            try {
                // Annuler une vente restitue la quantité au stock.
                medicamentDAO.ajusterStock(medoc, ligne.getNbr());
                achatDAO.delete(num, medoc);
                connexion.commit();
            } catch (SQLException e) {
                connexion.rollback();
                throw e;
            } finally {
                connexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            if (estViolationContrainte(e)) {
                throw new BusinessException("Suppression impossible : cette ligne est référencée ailleurs.");
            }
            throw erreurTechnique("la suppression de l'achat", e);
        }
    }
}
