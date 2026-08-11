package pharmacie.ui.components;

import pharmacie.ui.Theme;
import pharmacie.util.IconLoader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Carte d'indicateur du tableau de bord : un libellé, une valeur et une icône.
 *
 * L'icône est affichée en blanc dans une pastille ronde colorée, ce qui distingue
 * visuellement chaque indicateur sans recourir à un emoji.
 */
public class CarteIndicateur extends Carte {

    private final JLabel valeurLabel;

    /**
     * @param libelle  intitulé de l'indicateur (ex. « Médicaments »)
     * @param valeur   valeur initiale affichée
     * @param nomIcone nom du fichier d'icône, sans extension
     * @param couleur  couleur de la pastille
     */
    public CarteIndicateur(String libelle, String valeur, String nomIcone, Color couleur) {
        super(new BorderLayout(14, 0));
        setBorder(marge(16, 16, 16, 16));

        add(new Pastille(nomIcone, couleur), BorderLayout.WEST);

        JPanel textes = new JPanel();
        textes.setOpaque(false);
        textes.setLayout(new BoxLayout(textes, BoxLayout.Y_AXIS));

        JLabel libelleLabel = new JLabel(libelle.toUpperCase());
        libelleLabel.setFont(Theme.POLICE_LIBELLE);
        libelleLabel.setForeground(Theme.TEXTE_DOUX);
        libelleLabel.setAlignmentX(LEFT_ALIGNMENT);

        valeurLabel = new JLabel(valeur);
        valeurLabel.setFont(Theme.POLICE_CHIFFRE);
        valeurLabel.setForeground(Theme.TEXTE);
        valeurLabel.setAlignmentX(LEFT_ALIGNMENT);

        textes.add(libelleLabel);
        textes.add(Box.createVerticalStrut(4));
        textes.add(valeurLabel);

        add(textes, BorderLayout.CENTER);
    }

    /** Met à jour la valeur affichée. */
    public void setValeur(String valeur) {
        valeurLabel.setText(valeur);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension taille = super.getPreferredSize();
        taille.height = Math.max(taille.height, 92);
        return taille;
    }

    /** Pastille ronde colorée contenant l'icône blanche de l'indicateur. */
    private static class Pastille extends JPanel {

        private final Color couleur;

        Pastille(String nomIcone, Color couleur) {
            this.couleur = couleur;
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(52, 52));

            JLabel icone = new JLabel(IconLoader.getWhite(nomIcone, IconLoader.TAILLE_CARTE));
            icone.setHorizontalAlignment(SwingConstants.CENTER);
            icone.setBorder(BorderFactory.createEmptyBorder());
            add(icone, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(couleur);
            int diametre = Math.min(getWidth(), getHeight());
            g2.fillOval((getWidth() - diametre) / 2, (getHeight() - diametre) / 2, diametre, diametre);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
