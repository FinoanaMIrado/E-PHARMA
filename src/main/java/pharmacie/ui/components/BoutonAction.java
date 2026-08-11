package pharmacie.ui.components;

import pharmacie.ui.Theme;
import pharmacie.util.IconLoader;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Bouton plat aux angles arrondis, avec icône graphique et effet de survol.
 *
 * Les icônes proviennent toujours de fichiers PNG (voir {@link IconLoader}) :
 * aucun emoji ni caractère Unicode décoratif n'est utilisé dans l'interface.
 */
public class BoutonAction extends JButton {

    private final Color couleurFond;
    private final Color couleurSurvol;
    private boolean survol;

    /**
     * Crée un bouton.
     *
     * @param texte      libellé affiché
     * @param nomIcone   nom du fichier d'icône, sans extension ({@code null} si aucune icône)
     * @param fond       couleur de fond
     * @param texteClair vrai si le texte et l'icône doivent être blancs
     */
    public BoutonAction(String texte, String nomIcone, Color fond, boolean texteClair) {
        super(texte);
        this.couleurFond = fond;
        this.couleurSurvol = assombrir(fond);

        if (nomIcone != null) {
            Icon icone = texteClair
                    ? IconLoader.getWhite(nomIcone, IconLoader.TAILLE_BOUTON)
                    : IconLoader.get(nomIcone, IconLoader.TAILLE_BOUTON);
            if (icone != null) {
                setIcon(icone);
                setIconTextGap(8);
            }
        }

        setForeground(texteClair ? Color.WHITE : Theme.TEXTE);
        setFont(Theme.POLICE_GRASSE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                survol = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                survol = false;
                repaint();
            }
        });
    }

    /** Bouton d'action principale : fond vert, texte blanc. */
    public static BoutonAction principal(String texte, String icone) {
        return new BoutonAction(texte, icone, Theme.VERT, true);
    }

    /** Bouton secondaire : fond blanc bordé, texte sombre. */
    public static BoutonAction secondaire(String texte, String icone) {
        BoutonAction bouton = new BoutonAction(texte, icone, Theme.BLANC, false);
        bouton.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        return bouton;
    }

    /** Bouton destructif : fond rouge, texte blanc. */
    public static BoutonAction danger(String texte, String icone) {
        return new BoutonAction(texte, icone, Theme.ROUGE, true);
    }

    /** Bouton d'information : fond bleu, texte blanc. */
    public static BoutonAction info(String texte, String icone) {
        return new BoutonAction(texte, icone, Theme.BLEU, true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fond = !isEnabled() ? estompee(couleurFond)
                : survol ? couleurSurvol
                : couleurFond;

        g2.setColor(fond);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        // Les boutons clairs reçoivent une bordure pour rester visibles sur fond blanc.
        if (couleurFond.equals(Theme.BLANC)) {
            g2.setColor(Theme.BORDURE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension taille = super.getPreferredSize();
        taille.height = Math.max(taille.height, 36);
        return taille;
    }

    private static Color assombrir(Color couleur) {
        if (couleur.equals(Theme.BLANC)) return new Color(0xF1, 0xF5, 0xF9);
        return new Color(
                Math.max(0, (int) (couleur.getRed() * 0.88)),
                Math.max(0, (int) (couleur.getGreen() * 0.88)),
                Math.max(0, (int) (couleur.getBlue() * 0.88)));
    }

    private static Color estompee(Color couleur) {
        return new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), 110);
    }
}
