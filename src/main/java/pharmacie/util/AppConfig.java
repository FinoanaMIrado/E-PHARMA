package pharmacie.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralise les paramètres de l'application (connexion à la base de données…).
 *
 * Les valeurs sont lues dans {@code database.properties} ; chaque propriété
 * peut être surchargée au lancement via une propriété système (ex. {@code -Ddb.password=xxx}).
 */
public final class AppConfig {

    private static final String PROPERTIES_FILE = "/database.properties";
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Fichier de configuration introuvable : " + PROPERTIES_FILE);
            }
            props.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Impossible de lire la configuration : " + PROPERTIES_FILE + " -> " + e.getMessage());
        }
    }

    private AppConfig() {
    }

    /** Lit une propriété, en donnant la priorité à une éventuelle propriété système. */
    public static String get(String key) {
        return System.getProperty(key, props.getProperty(key, ""));
    }

    /** L'hôte du serveur MySQL/MariaDB. */
    public static String getDbHost() {
        return get("db.host");
    }

    /** Le port du serveur MySQL/MariaDB. */
    public static String getDbPort() {
        return get("db.port");
    }

    /** Le nom de la base de données. */
    public static String getDbName() {
        return get("db.name");
    }

    /** L'utilisateur de la base de données. */
    public static String getDbUser() {
        return get("db.user");
    }

    /** Le mot de passe de la base de données. */
    public static String getDbPassword() {
        return get("db.password");
    }

    /** Paramètres complémentaires passés au pilote JDBC (URL-encodés). */
    public static String getDbParams() {
        return get("db.params");
    }
}
