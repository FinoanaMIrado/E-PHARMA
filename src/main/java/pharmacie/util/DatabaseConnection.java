package pharmacie.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestion de la connexion JDBC vers MySQL / MariaDB.
 *
 * La connexion est créée à la demande et mise en cache : toutes les classes de
 * l'application partagent ainsi une seule connexion pendant la session, ce qui
 * est le comportement attendu pour une application desktop mono-utilisateur.
 * Si la connexion est perdue (serveur redémarré…), elle est recréée au prochain appel.
 */
public final class DatabaseConnection {

    private static Connection connection;

    private DatabaseConnection() {
    }

    /** Retourne la connexion JDBC, en la créant si nécessaire. */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:mysql://"
                    + AppConfig.getDbHost() + ":" + AppConfig.getDbPort()
                    + "/" + AppConfig.getDbName()
                    + "?" + AppConfig.getDbParams();
            connection = DriverManager.getConnection(url, AppConfig.getDbUser(), AppConfig.getDbPassword());
        }
        return connection;
    }

    /** Teste la connectivité : utilisé par l'écran de connexion pour signaler une erreur claire. */
    public static boolean isReachable() {
        try {
            return getConnection().isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Ferme la connexion en fin de session. */
    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Rien à faire : la session se termine.
            }
            connection = null;
        }
    }
}
