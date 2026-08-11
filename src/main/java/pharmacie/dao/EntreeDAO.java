package pharmacie.dao;

import pharmacie.model.Entree;
import pharmacie.util.DatabaseConnection;
import pharmacie.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table ENTREE.
 *
 * Les lectures joignent la table MEDICAMENT afin d'afficher la désignation du
 * médicament plutôt que son seul identifiant.
 */
public class EntreeDAO {

    private static final String SELECT_BASE =
            "SELECT e.numEntree, e.numMedoc, e.stockEntree, e.dateEntree, m.Design "
                    + "FROM entree e JOIN medicament m ON m.numMedoc = e.numMedoc ";

    /** Liste toutes les entrées, de la plus récente à la plus ancienne. */
    public List<Entree> findAll() throws SQLException {
        String sql = SELECT_BASE + "ORDER BY e.dateEntree DESC, e.numEntree DESC";
        try (PreparedStatement stmt = connexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return lireListe(rs);
        }
    }

    /** Recherche une entrée par son identifiant. */
    public Entree findById(String numEntree) throws SQLException {
        String sql = SELECT_BASE + "WHERE e.numEntree = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numEntree);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        }
    }

    /** Insère une entrée de stock. */
    public void insert(Entree entree) throws SQLException {
        String sql = "INSERT INTO entree (numEntree, numMedoc, stockEntree, dateEntree) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, entree.getNumEntree());
            stmt.setString(2, entree.getNumMedoc());
            stmt.setInt(3, entree.getStockEntree());
            stmt.setDate(4, DateUtil.toSqlDate(entree.getDateEntree()));
            stmt.executeUpdate();
        }
    }

    /** Met à jour une entrée existante. */
    public void update(Entree entree) throws SQLException {
        String sql = "UPDATE entree SET numMedoc = ?, stockEntree = ?, dateEntree = ? WHERE numEntree = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, entree.getNumMedoc());
            stmt.setInt(2, entree.getStockEntree());
            stmt.setDate(3, DateUtil.toSqlDate(entree.getDateEntree()));
            stmt.setString(4, entree.getNumEntree());
            stmt.executeUpdate();
        }
    }

    /** Supprime une entrée. */
    public void delete(String numEntree) throws SQLException {
        String sql = "DELETE FROM entree WHERE numEntree = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numEntree);
            stmt.executeUpdate();
        }
    }

    /** Vrai si l'identifiant d'entrée est déjà utilisé. */
    public boolean exists(String numEntree) throws SQLException {
        String sql = "SELECT 1 FROM entree WHERE numEntree = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numEntree);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Propose le prochain identifiant d'entrée au format {@code Ennn}.
     *
     * Le calcul se base sur le plus grand identifiant existant respectant ce
     * format ; les identifiants saisis manuellement sous une autre forme sont
     * simplement ignorés.
     */
    public String nextNumEntree() throws SQLException {
        String sql = "SELECT MAX(CAST(SUBSTRING(numEntree, 2) AS UNSIGNED)) FROM entree "
                + "WHERE numEntree REGEXP '^E[0-9]+$'";
        try (PreparedStatement stmt = connexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int max = rs.next() ? rs.getInt(1) : 0;
            return String.format("E%03d", max + 1);
        }
    }

    // ------------------------------------------------------------------
    //  Utilitaires internes
    // ------------------------------------------------------------------

    private Connection connexion() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    private List<Entree> lireListe(ResultSet rs) throws SQLException {
        List<Entree> entrees = new ArrayList<>();
        while (rs.next()) {
            entrees.add(lire(rs));
        }
        return entrees;
    }

    private Entree lire(ResultSet rs) throws SQLException {
        Entree entree = new Entree(
                rs.getString("numEntree"),
                rs.getString("numMedoc"),
                rs.getInt("stockEntree"),
                DateUtil.toLocalDate(rs.getDate("dateEntree")));
        entree.setDesignMedicament(rs.getString("Design"));
        return entree;
    }
}
