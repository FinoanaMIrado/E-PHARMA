package pharmacie.util;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Chargement des icônes de l'interface.
 *
 * Toutes les icônes sont de vrais fichiers PNG situés dans
 * {@code src/main/resources/icons} : l'application n'utilise aucun emoji ni
 * caractère Unicode décoratif. Deux variantes existent :
 * la variante sombre (par défaut) et la variante blanche ({@code /icons/white})
 * destinée au menu latéral sur fond foncé.
 *
 * Les icônes chargées sont mises en cache : une même icône n'est décodée et
 * redimensionnée qu'une seule fois.
 */
public final class IconLoader {

    /** Taille des icônes des boutons d'action. */
    public static final int TAILLE_BOUTON = 16;
    /** Taille des icônes du menu latéral. */
    public static final int TAILLE_MENU = 20;
    /** Taille des icônes des cartes du tableau de bord. */
    public static final int TAILLE_CARTE = 28;

    private static final Map<String, ImageIcon> cache = new HashMap<>();

    private IconLoader() {
    }

    /** Charge une icône sombre à la taille demandée. */
    public static ImageIcon get(String nom, int taille) {
        return charger("/icons/" + nom + ".png", taille);
    }

    /** Charge la variante blanche d'une icône, pour les fonds foncés. */
    public static ImageIcon getWhite(String nom, int taille) {
        return charger("/icons/white/" + nom + ".png", taille);
    }

    /**
     * Charge une image d'après son chemin dans les ressources.
     *
     * @return l'icône redimensionnée, ou {@code null} si le fichier est absent.
     *         Un bouton Swing accepte une icône nulle : l'interface reste
     *         utilisable même si une ressource manque.
     */
    private static ImageIcon charger(String chemin, int taille) {
        String cle = chemin + "@" + taille;
        ImageIcon enCache = cache.get(cle);
        if (enCache != null) return enCache;

        URL url = IconLoader.class.getResource(chemin);
        if (url == null) {
            System.err.println("Icône introuvable : " + chemin);
            return null;
        }

        Image image = new ImageIcon(url).getImage()
                .getScaledInstance(taille, taille, Image.SCALE_SMOOTH);
        ImageIcon icone = new ImageIcon(image);
        cache.put(cle, icone);
        return icone;
    }
}
