package pharmacie.ui;

import pharmacie.service.AuthService;
import pharmacie.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Convertit en hachage BCrypt les mots de passe encore stockes en clair dans la
 * table UTILISATEUR.
 *
 * L'application verifie les mots de passe avec BCrypt : une valeur en clair ne
 * peut donc jamais etre validee, et la connexion echoue meme avec le bon mot de
 * passe. Cet outil corrige la base sans changer les mots de passe eux-memes :
 * l'utilisateur continue de saisir la meme chose qu'avant.
 *
 * Les valeurs deja hachees sont laissees intactes : le script est rejouable
 * sans risque.
 *
 * Utilisation :
 *     java -cp "$CP:/tmp/tools" pharmacie.ui.MigrationMotsDePasse
 */
public class MigrationMotsDePasse {

    /** Un hachage BCrypt commence toujours par $2a$, $2b$ ou $2y$. */
    private static boolean dejaHache(String valeur) {
        return valeur != null && valeur.length() == 60 && valeur.startsWith("$2");
    }

    private record Compte(int id, String login, String motDePasse) {
    }

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        List<Compte> comptes = new ArrayList<>();
        String lecture = "SELECT id, login, motDePasse FROM utilisateur ORDER BY id";
        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(lecture)) {
            while (rs.next()) {
                comptes.add(new Compte(rs.getInt("id"), rs.getString("login"),
                        rs.getString("motDePasse")));
            }
        }

        System.out.println(comptes.size() + " compte(s) trouve(s).");
        System.out.println();

        int migres = 0;
        int ignores = 0;

        String maj = "UPDATE utilisateur SET motDePasse = ? WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getConnection().prepareStatement(maj)) {
            for (Compte compte : comptes) {
                if (dejaHache(compte.motDePasse())) {
                    System.out.println("  IGNORE   " + compte.login()
                            + " : deja hache, laisse intact");
                    ignores++;
                    continue;
                }

                // Les valeurs saisies a la main contiennent parfois un retour
                // chariot ou des espaces : on les retire, sinon le mot de passe
                // hache inclurait ces caracteres invisibles.
                String enClair = compte.motDePasse() == null ? "" : compte.motDePasse().strip();
                if (enClair.isEmpty()) {
                    System.out.println("  ATTENTION " + compte.login()
                            + " : mot de passe vide, compte non migre");
                    ignores++;
                    continue;
                }

                stmt.setString(1, AuthService.hacher(enClair));
                stmt.setInt(2, compte.id());
                stmt.executeUpdate();

                System.out.println("  MIGRE    " + compte.login()
                        + " : mot de passe hache (saisir toujours \"" + enClair + "\")");
                migres++;
            }
        }

        System.out.println();
        System.out.println("Resultat : " + migres + " migre(s), " + ignores + " ignore(s).");

        verifierConnexions(comptes);
        System.exit(0);
    }

    /** Rejoue une authentification reelle pour chaque compte migre. */
    private static void verifierConnexions(List<Compte> comptes) {
        System.out.println();
        System.out.println("--- Verification par authentification reelle ---");

        AuthService authService = new AuthService();
        for (Compte compte : comptes) {
            if (dejaHache(compte.motDePasse())) continue;

            String enClair = compte.motDePasse() == null ? "" : compte.motDePasse().strip();
            if (enClair.isEmpty()) continue;

            try {
                var utilisateur = authService.authentifier(compte.login(), enClair.toCharArray());
                System.out.println("  OK  " + compte.login() + " / " + enClair
                        + "  -> connexion reussie (role " + utilisateur.getRole() + ")");
            } catch (Exception e) {
                System.out.println("  ECHEC " + compte.login() + " : " + e.getMessage());
            }
        }
    }
}
