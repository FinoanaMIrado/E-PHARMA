package pharmacie.ui.components;

import pharmacie.ui.Theme;

import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Panneau blanc à coins arrondis servant de conteneur de section.
 *
 * Reproduit l'aspect « carte » attendu d'une application de gestion moderne :
 * fond blanc, bordure discrète et coins arrondis, sur le fond gris de la fenêtre.
 */
public class Carte extends JPanel {

    private static final int RAYON = 10;

    public Carte() {
        this(new BorderLayout());
    }

    public Carte(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBackground(Theme.BLANC);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RAYON, RAYON);

        g2.setColor(Theme.BORDURE);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RAYON, RAYON);

        g2.dispose();
        super.paintComponent(g);
    }

    /** Bordure intérieure transparente, à combiner avec le fond arrondi de la carte. */
    public static AbstractBorder marge(int haut, int gauche, int bas, int droite) {
        return new AbstractBorder() {
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(haut, gauche, bas, droite);
            }

            @Override
            public Insets getBorderInsets(Component c, Insets insets) {
                insets.set(haut, gauche, bas, droite);
                return insets;
            }

            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                // Aucune bordure dessinée : seul l'espacement compte.
            }
        };
    }

    /** Applique une couleur de fond personnalisée (carte colorée du tableau de bord). */
    public Carte avecFond(Color couleur) {
        setBackground(couleur);
        return this;
    }
}
