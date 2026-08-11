package pharmacie.ui;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Charte graphique de l'application : couleurs, polices et réglages globaux.
 *
 * Le look and feel « Metal » (multiplateforme) est utilisé volontairement plutôt
 * que celui du système : il respecte les couleurs de fond appliquées aux
 * composants, ce qui rend l'apparence identique sur toutes les machines.
 */
public final class Theme {

    // ---- Couleurs de marque ----
    /** Vert principal, couleur d'accentuation de l'application. */
    public static final Color VERT = new Color(0x15, 0x80, 0x61);
    public static final Color VERT_FONCE = new Color(0x0F, 0x60, 0x49);
    public static final Color VERT_CLAIR = new Color(0xE6, 0xF4, 0xEF);

    // ---- Menu latéral ----
    public static final Color MENU_FOND = new Color(0x1B, 0x2A, 0x3A);
    public static final Color MENU_FOND_SURVOL = new Color(0x27, 0x3B, 0x50);
    public static final Color MENU_TEXTE = new Color(0xC7, 0xD2, 0xDD);
    public static final Color MENU_SEPARATEUR = new Color(0x2E, 0x42, 0x59);

    // ---- Fonds et textes ----
    public static final Color FOND = new Color(0xF1, 0xF5, 0xF9);
    public static final Color BLANC = Color.WHITE;
    public static final Color BORDURE = new Color(0xDD, 0xE3, 0xEA);
    public static final Color TEXTE = new Color(0x1F, 0x2A, 0x37);
    public static final Color TEXTE_DOUX = new Color(0x64, 0x74, 0x85);

    // ---- Couleurs d'état ----
    public static final Color ROUGE = new Color(0xC0, 0x39, 0x3D);
    public static final Color ROUGE_FONCE = new Color(0xA0, 0x2B, 0x2F);
    public static final Color ORANGE = new Color(0xD9, 0x7A, 0x1E);
    public static final Color BLEU = new Color(0x24, 0x6B, 0xB5);
    public static final Color LIGNE_ALTERNEE = new Color(0xF8, 0xFA, 0xFC);
    public static final Color SELECTION = new Color(0xD8, 0xEA, 0xE4);

    // ---- Polices ----
    private static final String FAMILLE = choisirFamille();

    public static final Font POLICE_NORMALE = new Font(FAMILLE, Font.PLAIN, 13);
    public static final Font POLICE_GRASSE = new Font(FAMILLE, Font.BOLD, 13);
    public static final Font POLICE_PETITE = new Font(FAMILLE, Font.PLAIN, 11);
    public static final Font POLICE_LIBELLE = new Font(FAMILLE, Font.BOLD, 11);
    public static final Font POLICE_TITRE = new Font(FAMILLE, Font.BOLD, 19);
    public static final Font POLICE_SOUS_TITRE = new Font(FAMILLE, Font.BOLD, 15);
    public static final Font POLICE_CHIFFRE = new Font(FAMILLE, Font.BOLD, 26);
    public static final Font POLICE_LOGO = new Font(FAMILLE, Font.BOLD, 16);

    private Theme() {
    }

    /**
     * Retient la première police disponible parmi une liste de polices lisibles.
     *
     * Évite de dépendre d'une police absente du système : à défaut, la famille
     * logique {@code SansSerif} est toujours disponible.
     */
    private static String choisirFamille() {
        Set<String> disponibles = Arrays.stream(
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .collect(Collectors.toSet());

        for (String candidate : new String[]{"Segoe UI", "Inter", "Roboto", "Noto Sans",
                "DejaVu Sans", "Liberation Sans", "Ubuntu", "Arial"}) {
            if (disponibles.contains(candidate)) return candidate;
        }
        return Font.SANS_SERIF;
    }

    /** Applique le look and feel et les réglages globaux. À appeler avant toute création de fenêtre. */
    public static void installer() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // Le look and feel par défaut sera utilisé : l'application reste fonctionnelle.
            System.err.println("Look and feel non appliqué : " + e.getMessage());
        }

        FontUIResource police = new FontUIResource(POLICE_NORMALE);
        for (String cle : new String[]{"Label.font", "Button.font", "TextField.font",
                "PasswordField.font", "ComboBox.font", "Table.font", "TableHeader.font",
                "TabbedPane.font", "OptionPane.messageFont", "OptionPane.buttonFont",
                "Spinner.font", "TextArea.font", "CheckBox.font", "ToolTip.font"}) {
            UIManager.put(cle, police);
        }

        UIManager.put("Panel.background", FOND);
        UIManager.put("OptionPane.background", BLANC);
        UIManager.put("OptionPane.messageForeground", TEXTE);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXTE);
        UIManager.put("Table.gridColor", BORDURE);
        UIManager.put("ToolTip.background", BLANC);

        // Libellés français des boîtes de dialogue standard
        UIManager.put("OptionPane.yesButtonText", "Oui");
        UIManager.put("OptionPane.noButtonText", "Non");
        UIManager.put("OptionPane.cancelButtonText", "Annuler");
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("FileChooser.saveButtonText", "Enregistrer");
        UIManager.put("FileChooser.cancelButtonText", "Annuler");
        UIManager.put("FileChooser.fileNameLabelText", "Nom du fichier");
        UIManager.put("FileChooser.saveInLabelText", "Enregistrer dans");
    }
}
