package pharmacie.model;

/**
 * Un utilisateur autorisé à se connecter à l'application (table UTILISATEUR).
 *
 * Le mot de passe n'est jamais conservé en clair dans cet objet : seul le
 * hachage BCrypt lu en base y figure, et il n'est pas exposé à l'interface.
 */
public class Utilisateur {

    private int id;
    private String login;
    private String nomComplet;
    private String role;

    public Utilisateur() {
    }

    public Utilisateur(int id, String login, String nomComplet, String role) {
        this.id = id;
        this.login = login;
        this.nomComplet = nomComplet;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return nomComplet;
    }
}
