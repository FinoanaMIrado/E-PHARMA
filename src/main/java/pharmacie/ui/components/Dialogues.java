package pharmacie.ui.components;

import pharmacie.util.IconLoader;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Boîtes de dialogue de l'application.
 *
 * Toutes les icônes affichées proviennent des fichiers PNG du projet : les
 * icônes par défaut de Swing sont volontairement remplacées pour conserver une
 * apparence homogène, sans emoji ni symbole Unicode.
 */
public final class Dialogues {

    private static final int TAILLE_ICONE = 32;

    private Dialogues() {
    }

    /** Message de confirmation d'une opération réussie. */
    public static void succes(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Opération réussie",
                JOptionPane.INFORMATION_MESSAGE, IconLoader.get("check", TAILLE_ICONE));
    }

    /** Message d'erreur : règle métier non respectée ou problème technique. */
    public static void erreur(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Erreur",
                JOptionPane.ERROR_MESSAGE, IconLoader.get("alert", TAILLE_ICONE));
    }

    /** Avertissement : saisie incomplète, stock faible… */
    public static void avertissement(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Attention",
                JOptionPane.WARNING_MESSAGE, IconLoader.get("alert", TAILLE_ICONE));
    }

    /** Message d'information neutre. */
    public static void information(Component parent, String message, String titre) {
        JOptionPane.showMessageDialog(parent, message, titre,
                JOptionPane.INFORMATION_MESSAGE, IconLoader.get("check", TAILLE_ICONE));
    }

    /**
     * Demande une confirmation avant une opération destructive.
     *
     * @return vrai si l'utilisateur a répondu « Oui »
     */
    public static boolean confirmer(Component parent, String message, String titre) {
        int reponse = JOptionPane.showConfirmDialog(parent, message, titre,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                IconLoader.get("alert", TAILLE_ICONE));
        return reponse == JOptionPane.YES_OPTION;
    }

    /**
     * Propose une action facultative après une opération réussie
     * (par exemple : générer la facture après un achat).
     *
     * @return vrai si l'utilisateur accepte
     */
    public static boolean proposer(Component parent, String message, String titre, String nomIcone) {
        int reponse = JOptionPane.showConfirmDialog(parent, message, titre,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                IconLoader.get(nomIcone, TAILLE_ICONE));
        return reponse == JOptionPane.YES_OPTION;
    }
}
