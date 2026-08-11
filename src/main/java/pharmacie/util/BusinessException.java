package pharmacie.util;

/**
 * Erreur de règle métier destinée à être présentée telle quelle à l'utilisateur.
 *
 * Les services lèvent cette exception pour les situations prévues par le cahier
 * des charges (stock insuffisant, numéro déjà utilisé, champ invalide…) ; les
 * écrans Swing se contentent d'afficher son message. Les erreurs techniques
 * (SQLException…) sont, elles, converties en un message générique.
 */
public class BusinessException extends Exception {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
