package pharmacie.dao;

import pharmacie.model.Utilisateur;
import pharmacie.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Accès à la table UTILISATEUR.
 *
 * Le hachage du mot de passe ne sort de cette classe que pour être vérifié par
 * le service d'authentification ; il n'est jamais transporté dans le modèle
 * {@link Utilisateur} exposé à l'interface.
 */
public class UtilisateurDAO {

    /** Représente les données d'identification lues en base. */
    public record Identifiants(Utilisateur utilisateur, String hashMotDePasse) {
    }

    /**
     * Recherche un utilisateur par son login.
     *
     * @return les identifiants, ou {@code null} si le login est inconnu
     */
    public Identifiants findByLogin(String login) throws SQLException {
        String sql = "SELECT id, login, motDePasse, nomComplet, role FROM utilisateur WHERE login = ?";
        try (PreparedStatement stmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                Utilisateur utilisateur = new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("nomComplet"),
                        rs.getString("role"));
                return new Identifiants(utilisateur, rs.getString("motDePasse"));
            }
        }
    }
}
