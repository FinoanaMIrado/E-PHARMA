package pharmacie.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import pharmacie.dao.UtilisateurDAO;
import pharmacie.model.Utilisateur;
import pharmacie.util.BusinessException;
import pharmacie.util.DatabaseConnection;

import java.sql.SQLException;

/**
 * Authentification des utilisateurs.
 *
 * Les mots de passe sont stockés hachés avec BCrypt : la base ne contient aucun
 * mot de passe en clair, et la vérification se fait par comparaison de hachage.
 *
 * Le message d'erreur affiché ne précise pas si c'est le login ou le mot de
 * passe qui est erroné, afin de ne pas révéler l'existence d'un compte.
 */
public class AuthService {

    private static final String MESSAGE_ECHEC = "Nom d'utilisateur ou mot de passe incorrect.";

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    /** L'utilisateur connecté pour la session en cours. */
    private static Utilisateur utilisateurConnecte;

    /**
     * Vérifie les identifiants et ouvre la session.
     *
     * @return l'utilisateur authentifié
     * @throws BusinessException si les champs sont vides, les identifiants
     *                           invalides ou la base inaccessible
     */
    public Utilisateur authentifier(String login, char[] motDePasse) throws BusinessException {
        if (login == null || login.isBlank()) {
            throw new BusinessException("Saisissez votre nom d'utilisateur.");
        }
        if (motDePasse == null || motDePasse.length == 0) {
            throw new BusinessException("Saisissez votre mot de passe.");
        }

        if (!DatabaseConnection.isReachable()) {
            throw new BusinessException(
                    "Connexion à la base de données impossible.\n\n"
                            + "Vérifiez que le serveur MySQL est démarré et que les paramètres du fichier\n"
                            + "src/main/resources/database.properties sont corrects.");
        }

        try {
            UtilisateurDAO.Identifiants identifiants = utilisateurDAO.findByLogin(login.trim());
            if (identifiants == null) {
                throw new BusinessException(MESSAGE_ECHEC);
            }

            BCrypt.Result resultat = BCrypt.verifyer()
                    .verify(motDePasse, identifiants.hashMotDePasse());
            if (!resultat.verified) {
                throw new BusinessException(MESSAGE_ECHEC);
            }

            utilisateurConnecte = identifiants.utilisateur();
            return utilisateurConnecte;
        } catch (SQLException e) {
            throw StockService.erreurTechnique("la vérification des identifiants", e);
        }
    }

    /** L'utilisateur actuellement connecté, ou {@code null} hors session. */
    public static Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    /** Termine la session en cours. */
    public static void deconnecter() {
        utilisateurConnecte = null;
    }

    /**
     * Calcule le hachage BCrypt d'un mot de passe.
     *
     * Utilitaire destiné à la création de comptes : le hachage produit peut être
     * inséré dans la colonne {@code motDePasse} de la table UTILISATEUR.
     */
    public static String hacher(String motDePasseEnClair) {
        return BCrypt.withDefaults().hashToString(12, motDePasseEnClair.toCharArray());
    }
}
