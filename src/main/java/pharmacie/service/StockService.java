package pharmacie.service;

import pharmacie.dao.EntreeDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.model.Entree;
import pharmacie.model.Medicament;
import pharmacie.util.BusinessException;
import pharmacie.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Règles métier du menu STOCK : médicaments et entrées de stock.
 *
 * Le stock d'un médicament n'est jamais saisi directement :
 * <ul>
 *   <li>à la création d'un médicament, il vaut 0 ;</li>
 *   <li>une entrée l'augmente de la quantité entrée ;</li>
 *   <li>un achat le diminue (voir {@link AchatService}).</li>
 * </ul>
 *
 * Toute opération touchant à la fois ENTREE et MEDICAMENT est effectuée dans
 * une transaction : le stock et l'historique des entrées restent cohérents même
 * si une erreur survient en cours de route.
 */
public class StockService {

    private final MedicamentDAO medicamentDAO = new MedicamentDAO();
    private final EntreeDAO entreeDAO = new EntreeDAO();

    // ==================================================================
    //  MEDICAMENTS
    // ==================================================================

    /** Liste tous les médicaments. */
    public List<Medicament> listerMedicaments() throws BusinessException {
        try {
            return medicamentDAO.findAll();
        } catch (SQLException e) {
            throw erreurTechnique("la lecture des médicaments", e);
        }
    }

    /** Recherche partielle sur la désignation ; retourne tout si le texte est vide. */
    public List<Medicament> rechercherMedicaments(String texte) throws BusinessException {
        try {
            if (texte == null || texte.isBlank()) return medicamentDAO.findAll();
            return medicamentDAO.searchByDesign(texte.trim());
        } catch (SQLException e) {
            throw erreurTechnique("la recherche de médicaments", e);
        }
    }

    /** Retourne un médicament par son identifiant, ou {@code null}. */
    public Medicament trouverMedicament(String numMedoc) throws BusinessException {
        try {
            return medicamentDAO.findById(numMedoc);
        } catch (SQLException e) {
            throw erreurTechnique("la lecture du médicament", e);
        }
    }

    /** Liste les médicaments dont le stock est inférieur au seuil d'alerte. */
    public List<Medicament> listerStockFaible() throws BusinessException {
        try {
            return medicamentDAO.findStockFaible(Medicament.SEUIL_STOCK_FAIBLE);
        } catch (SQLException e) {
            throw erreurTechnique("la lecture des stocks faibles", e);
        }
    }

    /**
     * Crée un médicament avec un stock forcé à 0.
     *
     * @throws BusinessException si un champ est invalide ou si l'identifiant existe déjà
     */
    public void creerMedicament(String numMedoc, String design, String prixTexte) throws BusinessException {
        String num = obligatoire(numMedoc, "Le numéro du médicament est obligatoire.");
        String designation = obligatoire(design, "La désignation est obligatoire.");
        int prix = prixValide(prixTexte);

        try {
            if (medicamentDAO.exists(num)) {
                throw new BusinessException("Le numéro « " + num + " » est déjà utilisé par un autre médicament.");
            }
            // Règle 19.1 du cahier des charges : le stock initial est toujours 0.
            medicamentDAO.insert(new Medicament(num, designation, prix, 0));
        } catch (SQLException e) {
            throw erreurTechnique("l'enregistrement du médicament", e);
        }
    }

    /**
     * Modifie la désignation et le prix d'un médicament.
     *
     * Le stock n'est pas modifiable ici : il découle des entrées et des achats.
     */
    public void modifierMedicament(String numMedoc, String design, String prixTexte) throws BusinessException {
        String num = obligatoire(numMedoc, "Sélectionnez le médicament à modifier.");
        String designation = obligatoire(design, "La désignation est obligatoire.");
        int prix = prixValide(prixTexte);

        try {
            if (medicamentDAO.findById(num) == null) {
                throw new BusinessException("Ce médicament n'existe plus.");
            }
            Medicament medicament = new Medicament(num, designation, prix, 0);
            medicamentDAO.update(medicament);
        } catch (SQLException e) {
            throw erreurTechnique("la modification du médicament", e);
        }
    }

    /**
     * Supprime un médicament.
     *
     * La suppression est refusée s'il est référencé par une entrée ou un achat :
     * l'historique des mouvements doit rester intact.
     */
    public void supprimerMedicament(String numMedoc) throws BusinessException {
        String num = obligatoire(numMedoc, "Sélectionnez le médicament à supprimer.");
        try {
            medicamentDAO.delete(num);
        } catch (SQLException e) {
            if (estViolationContrainte(e)) {
                throw new BusinessException(
                        "Suppression impossible : ce médicament est utilisé par des entrées "
                                + "ou des achats déjà enregistrés.");
            }
            throw erreurTechnique("la suppression du médicament", e);
        }
    }

    // ==================================================================
    //  ENTREES DE STOCK
    // ==================================================================

    /** Liste toutes les entrées de stock. */
    public List<Entree> listerEntrees() throws BusinessException {
        try {
            return entreeDAO.findAll();
        } catch (SQLException e) {
            throw erreurTechnique("la lecture des entrées", e);
        }
    }

    /** Propose le prochain numéro d'entrée disponible. */
    public String prochainNumeroEntree() throws BusinessException {
        try {
            return entreeDAO.nextNumEntree();
        } catch (SQLException e) {
            throw erreurTechnique("le calcul du numéro d'entrée", e);
        }
    }

    /**
     * Enregistre une entrée de stock et augmente le stock du médicament.
     *
     * {@code stock = stock + stockEntree}
     */
    public void creerEntree(String numEntree, String numMedoc, String quantiteTexte, LocalDate date)
            throws BusinessException {
        String num = obligatoire(numEntree, "Le numéro d'entrée est obligatoire.");
        String medoc = obligatoire(numMedoc, "Sélectionnez un médicament.");
        int quantite = quantiteValide(quantiteTexte);
        LocalDate dateEntree = dateValide(date);

        Connection connexion = null;
        try {
            if (entreeDAO.exists(num)) {
                throw new BusinessException("Le numéro d'entrée « " + num + " » est déjà utilisé.");
            }

            connexion = DatabaseConnection.getConnection();
            connexion.setAutoCommit(false);
            try {
                entreeDAO.insert(new Entree(num, medoc, quantite, dateEntree));
                if (!medicamentDAO.ajusterStock(medoc, quantite)) {
                    throw new BusinessException("Le médicament sélectionné n'existe plus.");
                }
                connexion.commit();
            } catch (SQLException | BusinessException e) {
                connexion.rollback();
                throw e;
            } finally {
                connexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw erreurTechnique("l'enregistrement de l'entrée", e);
        }
    }

    /**
     * Modifie une entrée existante et répercute l'écart sur le stock.
     *
     * Le stock est corrigé de la différence entre l'ancienne et la nouvelle
     * quantité ; si le médicament change, l'ancien est débité et le nouveau crédité.
     */
    public void modifierEntree(String numEntree, String numMedoc, String quantiteTexte, LocalDate date)
            throws BusinessException {
        String num = obligatoire(numEntree, "Sélectionnez l'entrée à modifier.");
        String medoc = obligatoire(numMedoc, "Sélectionnez un médicament.");
        int quantite = quantiteValide(quantiteTexte);
        LocalDate dateEntree = dateValide(date);

        Connection connexion = null;
        try {
            Entree ancienne = entreeDAO.findById(num);
            if (ancienne == null) {
                throw new BusinessException("Cette entrée n'existe plus.");
            }

            connexion = DatabaseConnection.getConnection();
            connexion.setAutoCommit(false);
            try {
                if (ancienne.getNumMedoc().equals(medoc)) {
                    int ecart = quantite - ancienne.getStockEntree();
                    if (ecart != 0 && !medicamentDAO.ajusterStock(medoc, ecart)) {
                        throw new BusinessException(
                                "Modification impossible : le stock disponible ne permet pas de réduire "
                                        + "cette entrée (des achats ont déjà été effectués).");
                    }
                } else {
                    if (!medicamentDAO.ajusterStock(ancienne.getNumMedoc(), -ancienne.getStockEntree())) {
                        throw new BusinessException(
                                "Modification impossible : le stock de « " + ancienne.getDesignMedicament()
                                        + " » ne permet pas d'annuler cette entrée.");
                    }
                    if (!medicamentDAO.ajusterStock(medoc, quantite)) {
                        throw new BusinessException("Le médicament sélectionné n'existe plus.");
                    }
                }

                Entree modifiee = new Entree(num, medoc, quantite, dateEntree);
                entreeDAO.update(modifiee);
                connexion.commit();
            } catch (SQLException | BusinessException e) {
                connexion.rollback();
                throw e;
            } finally {
                connexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw erreurTechnique("la modification de l'entrée", e);
        }
    }

    /** Supprime une entrée et retire du stock la quantité correspondante. */
    public void supprimerEntree(String numEntree) throws BusinessException {
        String num = obligatoire(numEntree, "Sélectionnez l'entrée à supprimer.");

        Connection connexion = null;
        try {
            Entree entree = entreeDAO.findById(num);
            if (entree == null) {
                throw new BusinessException("Cette entrée n'existe plus.");
            }

            connexion = DatabaseConnection.getConnection();
            connexion.setAutoCommit(false);
            try {
                if (!medicamentDAO.ajusterStock(entree.getNumMedoc(), -entree.getStockEntree())) {
                    throw new BusinessException(
                            "Suppression impossible : le stock actuel de « " + entree.getDesignMedicament()
                                    + " » est inférieur à la quantité de cette entrée "
                                    + "(des achats ont déjà consommé ce stock).");
                }
                entreeDAO.delete(num);
                connexion.commit();
            } catch (SQLException | BusinessException e) {
                connexion.rollback();
                throw e;
            } finally {
                connexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw erreurTechnique("la suppression de l'entrée", e);
        }
    }

    // ==================================================================
    //  Validations communes
    // ==================================================================

    /** Vérifie qu'un champ texte est renseigné et retourne sa valeur nettoyée. */
    static String obligatoire(String valeur, String message) throws BusinessException {
        if (valeur == null || valeur.isBlank()) {
            throw new BusinessException(message);
        }
        return valeur.trim();
    }

    /** Vérifie qu'un prix est un entier positif ou nul. */
    static int prixValide(String texte) throws BusinessException {
        String valeur = obligatoire(texte, "Le prix unitaire est obligatoire.");
        int prix;
        try {
            prix = Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw new BusinessException("Le prix unitaire doit être un nombre entier.");
        }
        if (prix < 0) {
            throw new BusinessException("Le prix unitaire ne peut pas être négatif.");
        }
        return prix;
    }

    /** Vérifie qu'une quantité est un entier strictement positif. */
    static int quantiteValide(String texte) throws BusinessException {
        String valeur = obligatoire(texte, "La quantité est obligatoire.");
        int quantite;
        try {
            quantite = Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw new BusinessException("La quantité doit être un nombre entier.");
        }
        if (quantite <= 0) {
            throw new BusinessException("La quantité doit être strictement positive.");
        }
        return quantite;
    }

    /** Vérifie qu'une date est renseignée et valide. */
    static LocalDate dateValide(LocalDate date) throws BusinessException {
        if (date == null) {
            throw new BusinessException("La date est obligatoire et doit respecter le format jj/mm/aaaa.");
        }
        return date;
    }

    /** Vrai si l'erreur SQL traduit une violation de clé étrangère. */
    static boolean estViolationContrainte(SQLException e) {
        return "23000".equals(e.getSQLState());
    }

    /** Convertit une erreur technique en message utilisateur compréhensible. */
    static BusinessException erreurTechnique(String operation, SQLException e) {
        return new BusinessException(
                "Une erreur est survenue lors de " + operation + ".\n"
                        + "Vérifiez que la base de données est accessible.\n\nDétail : " + e.getMessage(), e);
    }
}
