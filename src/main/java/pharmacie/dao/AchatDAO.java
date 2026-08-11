package pharmacie.dao;

import pharmacie.model.Achat;
import pharmacie.util.DatabaseConnection;
import pharmacie.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accès à la table ACHAT.
 *
 * Une vente portant sur plusieurs médicaments est stockée sous forme de
 * plusieurs lignes partageant le même {@code numAchat}.
 */
public class AchatDAO {

    private static final String SELECT_BASE =
            "SELECT a.numAchat, a.numMedoc, a.nomClient, a.nbr, a.dateAchat, a.prix_unitaire, m.Design "
                    + "FROM achat a JOIN medicament m ON m.numMedoc = a.numMedoc ";

    /** Liste toutes les lignes d'achat, de la plus récente à la plus ancienne. */
    public List<Achat> findAll() throws SQLException {
        String sql = SELECT_BASE + "ORDER BY a.dateAchat DESC, a.numAchat DESC, m.Design";
        try (PreparedStatement stmt = connexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return lireListe(rs);
        }
    }

    /** Recherche les achats d'un client (recherche partielle sur le nom). */
    public List<Achat> searchByClient(String nomClient) throws SQLException {
        String sql = SELECT_BASE + "WHERE a.nomClient LIKE ? ESCAPE '!' "
                + "ORDER BY a.dateAchat DESC, a.numAchat DESC, m.Design";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, "%" + echapperLike(nomClient) + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                return lireListe(rs);
            }
        }
    }

    /** Retourne toutes les lignes d'un achat donné (contenu de la facture). */
    public List<Achat> findByNumAchat(String numAchat) throws SQLException {
        String sql = SELECT_BASE + "WHERE a.numAchat = ? ORDER BY m.Design";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numAchat);
            try (ResultSet rs = stmt.executeQuery()) {
                return lireListe(rs);
            }
        }
    }

    /** Retourne une ligne d'achat précise, identifiée par l'achat et le médicament. */
    public Achat findLigne(String numAchat, String numMedoc) throws SQLException {
        String sql = SELECT_BASE + "WHERE a.numAchat = ? AND a.numMedoc = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numAchat);
            stmt.setString(2, numMedoc);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        }
    }

    /** Insère une ligne d'achat. */
    public void insert(Achat achat) throws SQLException {
        String sql = "INSERT INTO achat (numAchat, numMedoc, nomClient, nbr, dateAchat, prix_unitaire) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, achat.getNumAchat());
            stmt.setString(2, achat.getNumMedoc());
            stmt.setString(3, achat.getNomClient());
            stmt.setInt(4, achat.getNbr());
            stmt.setDate(5, DateUtil.toSqlDate(achat.getDateAchat()));
            stmt.setInt(6, achat.getPrixUnitaire());
            stmt.executeUpdate();
        }
    }

    /** Met à jour la quantité, le client et la date d'une ligne d'achat. */
    public void update(Achat achat) throws SQLException {
        String sql = "UPDATE achat SET nomClient = ?, nbr = ?, dateAchat = ? "
                + "WHERE numAchat = ? AND numMedoc = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, achat.getNomClient());
            stmt.setInt(2, achat.getNbr());
            stmt.setDate(3, DateUtil.toSqlDate(achat.getDateAchat()));
            stmt.setString(4, achat.getNumAchat());
            stmt.setString(5, achat.getNumMedoc());
            stmt.executeUpdate();
        }
    }

    /** Supprime une ligne d'achat. */
    public void delete(String numAchat, String numMedoc) throws SQLException {
        String sql = "DELETE FROM achat WHERE numAchat = ? AND numMedoc = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numAchat);
            stmt.setString(2, numMedoc);
            stmt.executeUpdate();
        }
    }

    /** Vrai si le numéro d'achat est déjà utilisé. */
    public boolean existsNumAchat(String numAchat) throws SQLException {
        String sql = "SELECT 1 FROM achat WHERE numAchat = ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setString(1, numAchat);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Nombre d'achats distincts (un achat = un panier, quel que soit le nombre de lignes). */
    public int countAchats() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT numAchat) FROM achat";
        try (PreparedStatement stmt = connexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Recette totale : somme de {@code prix_unitaire x nbr} sur toutes les lignes. */
    public long recetteTotale() throws SQLException {
        String sql = "SELECT COALESCE(SUM(nbr * prix_unitaire), 0) FROM achat";
        try (PreparedStatement stmt = connexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    /**
     * Médicaments les plus vendus, classés par quantité totale vendue.
     *
     * @param limite nombre de lignes retournées (5 pour le « top 5 »)
     */
    public List<VenteParMedicament> topVentes(int limite) throws SQLException {
        String sql = "SELECT a.numMedoc, m.Design, SUM(a.nbr) AS quantite, "
                + "SUM(a.nbr * a.prix_unitaire) AS montant "
                + "FROM achat a JOIN medicament m ON m.numMedoc = a.numMedoc "
                + "GROUP BY a.numMedoc, m.Design "
                + "ORDER BY quantite DESC, m.Design LIMIT ?";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setInt(1, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                List<VenteParMedicament> ventes = new ArrayList<>();
                while (rs.next()) {
                    ventes.add(new VenteParMedicament(
                            rs.getString("numMedoc"),
                            rs.getString("Design"),
                            rs.getInt("quantite"),
                            rs.getLong("montant")));
                }
                return ventes;
            }
        }
    }

    /**
     * Recettes mensuelles depuis une date donnée.
     *
     * Seuls les mois comportant au moins une vente sont retournés ; c'est au
     * service de compléter les mois sans vente par une valeur nulle afin que
     * l'histogramme affiche bien cinq colonnes.
     */
    public Map<YearMonth, Long> recettesParMois(LocalDate depuis) throws SQLException {
        String sql = "SELECT YEAR(dateAchat) AS annee, MONTH(dateAchat) AS mois, "
                + "COALESCE(SUM(nbr * prix_unitaire), 0) AS montant "
                + "FROM achat WHERE dateAchat >= ? "
                + "GROUP BY YEAR(dateAchat), MONTH(dateAchat) "
                + "ORDER BY annee, mois";
        try (PreparedStatement stmt = connexion().prepareStatement(sql)) {
            stmt.setDate(1, DateUtil.toSqlDate(depuis));
            try (ResultSet rs = stmt.executeQuery()) {
                Map<YearMonth, Long> recettes = new LinkedHashMap<>();
                while (rs.next()) {
                    recettes.put(YearMonth.of(rs.getInt("annee"), rs.getInt("mois")),
                            rs.getLong("montant"));
                }
                return recettes;
            }
        }
    }

    /** Résultat agrégé des ventes d'un médicament. */
    public record VenteParMedicament(String numMedoc, String design, int quantiteVendue, long montantTotal) {
    }

    // ------------------------------------------------------------------
    //  Utilitaires internes
    // ------------------------------------------------------------------

    private Connection connexion() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    private List<Achat> lireListe(ResultSet rs) throws SQLException {
        List<Achat> achats = new ArrayList<>();
        while (rs.next()) {
            achats.add(lire(rs));
        }
        return achats;
    }

    private Achat lire(ResultSet rs) throws SQLException {
        Achat achat = new Achat(
                rs.getString("numAchat"),
                rs.getString("numMedoc"),
                rs.getString("nomClient"),
                rs.getInt("nbr"),
                DateUtil.toLocalDate(rs.getDate("dateAchat")),
                rs.getInt("prix_unitaire"));
        achat.setDesignMedicament(rs.getString("Design"));
        return achat;
    }

    private String echapperLike(String texte) {
        if (texte == null) return "";
        return texte.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
