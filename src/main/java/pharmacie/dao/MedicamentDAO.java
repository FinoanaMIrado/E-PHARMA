package pharmacie.dao;

import pharmacie.model.Medicament;
import pharmacie.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table MEDICAMENT.
 *
 * Toutes les requêtes utilisent {@link PreparedStatement} : aucune valeur
 * saisie par l'utilisateur n'est concaténée dans le SQL.
 */
public class MedicamentDAO {

    private static final String COLONNES = "numMedoc, Design, prix_unitaire, stock";

    /** Liste tous les médicaments, triés par désignation. */
    public List<Medicament> findAll() throws SQLException {
        String sql = "SELECT " + COLONNES + " FROM medicament ORDER BY Design";
        try (PreparedStatement stmt = connexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return lireListe(rs);
        }
    }

    /** Recherche un médicament par son identifiant. */
    public Medicament findById(String numMedoc) throws SQLException {
        String sql = "SELECT " + COLONNES + " FROM medicament WHERE numMedoc = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numMedoc);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        }
    }

    /**
     * Recherche partielle sur la désignation, via {@code LIKE '%texte%'}.
     *
     * Le texte recherché est passé en paramètre lié : les caractères spéciaux
     * de LIKE ({@code %} et {@code _}) sont échappés au préalable pour qu'une
     * saisie contenant « % » ne retourne pas toute la table.
     */
    public List<Medicament> searchByDesign(String texte) throws SQLException {
        String sql = "SELECT " + COLONNES + " FROM medicament "
                + "WHERE Design LIKE ? ESCAPE '!' ORDER BY Design";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, "%" + echapperLike(texte) + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                return lireListe(rs);
            }
        }
    }

    /** Liste les médicaments dont le stock est strictement inférieur au seuil. */
    public List<Medicament> findStockFaible(int seuil) throws SQLException {
        String sql = "SELECT " + COLONNES + " FROM medicament WHERE stock < ? ORDER BY stock, Design";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setInt(1, seuil);
            try (ResultSet rs = stmt.executeQuery()) {
                return lireListe(rs);
            }
        }
    }

    /** Insère un médicament. Le stock enregistré est toujours celui de l'objet (0 à la création). */
    public void insert(Medicament medicament) throws SQLException {
        String sql = "INSERT INTO medicament (numMedoc, Design, prix_unitaire, stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, medicament.getNumMedoc());
            stmt.setString(2, medicament.getDesign());
            stmt.setInt(3, medicament.getPrixUnitaire());
            stmt.setInt(4, medicament.getStock());
            stmt.executeUpdate();
        }
    }

    /**
     * Met à jour la désignation et le prix d'un médicament.
     *
     * Le stock n'est volontairement pas modifiable ici : il ne peut évoluer que
     * par une entrée de stock ou un achat, conformément aux règles métier.
     */
    public void update(Medicament medicament) throws SQLException {
        String sql = "UPDATE medicament SET Design = ?, prix_unitaire = ? WHERE numMedoc = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, medicament.getDesign());
            stmt.setInt(2, medicament.getPrixUnitaire());
            stmt.setString(3, medicament.getNumMedoc());
            stmt.executeUpdate();
        }
    }

    /** Supprime un médicament. */
    public void delete(String numMedoc) throws SQLException {
        String sql = "DELETE FROM medicament WHERE numMedoc = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numMedoc);
            stmt.executeUpdate();
        }
    }

    /** Vrai si l'identifiant est déjà utilisé. */
    public boolean exists(String numMedoc) throws SQLException {
        String sql = "SELECT 1 FROM medicament WHERE numMedoc = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numMedoc);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Nombre total de médicaments enregistrés. */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM medicament";
        try (PreparedStatement stmt = connexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Nombre de médicaments dont le stock est strictement inférieur au seuil. */
    public int countStockFaible(int seuil) throws SQLException {
        String sql = "SELECT COUNT(*) FROM medicament WHERE stock < ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setInt(1, seuil);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Applique une variation de stock (positive pour une entrée, négative pour un achat).
     *
     * La condition {@code stock + ? >= 0} interdit tout stock négatif directement
     * en base : même en cas d'accès concurrent, la mise à jour échoue plutôt que
     * de produire un stock incohérent.
     *
     * @return vrai si la ligne a été mise à jour, faux si le stock aurait été négatif
     */
    public boolean ajusterStock(String numMedoc, int variation) throws SQLException {
        String sql = "UPDATE medicament SET stock = stock + ? WHERE numMedoc = ? AND stock + ? >= 0";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setInt(1, variation);
            stmt.setString(2, numMedoc);
            stmt.setInt(3, variation);
            return stmt.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------------
    //  Utilitaires internes
    // ------------------------------------------------------------------

    private Connection connexion() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    private List<Medicament> lireListe(ResultSet rs) throws SQLException {
        List<Medicament> medicaments = new ArrayList<>();
        while (rs.next()) {
            medicaments.add(lire(rs));
        }
        return medicaments;
    }

    private Medicament lire(ResultSet rs) throws SQLException {
        return new Medicament(
                rs.getString("numMedoc"),
                rs.getString("Design"),
                rs.getInt("prix_unitaire"),
                rs.getInt("stock"));
    }

    /** Neutralise les jokers LIKE saisis par l'utilisateur (caractère d'échappement « ! »). */
    private String echapperLike(String texte) {
        if (texte == null) return "";
        return texte.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
