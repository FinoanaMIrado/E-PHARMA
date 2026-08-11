package pharmacie.ui.components;

import pharmacie.service.BilanService;
import pharmacie.ui.Theme;
import pharmacie.util.DateUtil;
import pharmacie.util.MontantUtil;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Histogramme des recettes mensuelles, dessiné avec {@link Graphics2D}.
 *
 * Conformément à la contrainte « Java Swing uniquement », aucun composant ni
 * bibliothèque graphique externe n'est utilisé : le graphique est peint
 * directement dans ce panneau.
 */
public class HistogrammeRecettes extends JPanel {

    private static final int MARGE_GAUCHE = 78;
    private static final int MARGE_DROITE = 20;
    private static final int MARGE_HAUT = 20;
    private static final int MARGE_BAS = 46;

    private static final int NB_GRADUATIONS = 4;
    private static final int LARGEUR_BARRE_MAX = 74;

    private List<BilanService.RecetteMensuelle> recettes = new ArrayList<>();

    public HistogrammeRecettes() {
        setOpaque(false);
        setPreferredSize(new Dimension(560, 280));
    }

    /** Remplace les données affichées et redessine le graphique. */
    public void setRecettes(List<BilanService.RecetteMensuelle> recettes) {
        this.recettes = recettes == null ? new ArrayList<>() : recettes;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (recettes.isEmpty()) {
            dessinerMessageVide(g2);
            g2.dispose();
            return;
        }

        int largeurZone = getWidth() - MARGE_GAUCHE - MARGE_DROITE;
        int hauteurZone = getHeight() - MARGE_HAUT - MARGE_BAS;
        if (largeurZone <= 0 || hauteurZone <= 0) {
            g2.dispose();
            return;
        }

        long maximum = recettes.stream().mapToLong(BilanService.RecetteMensuelle::montant).max().orElse(0);
        long echelle = calculerEchelle(maximum);

        dessinerGrille(g2, largeurZone, hauteurZone, echelle);
        dessinerBarres(g2, largeurZone, hauteurZone, echelle);

        g2.dispose();
    }

    // ------------------------------------------------------------------
    //  Dessin
    // ------------------------------------------------------------------

    /** Lignes horizontales et valeurs de l'axe vertical. */
    private void dessinerGrille(Graphics2D g2, int largeurZone, int hauteurZone, long echelle) {
        g2.setFont(Theme.POLICE_PETITE);
        FontMetrics metriques = g2.getFontMetrics();

        for (int i = 0; i <= NB_GRADUATIONS; i++) {
            int y = MARGE_HAUT + hauteurZone - (hauteurZone * i / NB_GRADUATIONS);

            g2.setColor(i == 0 ? Theme.BORDURE : new Color(0xEE, 0xF2, 0xF6));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(MARGE_GAUCHE, y, MARGE_GAUCHE + largeurZone, y);

            String valeur = MontantUtil.format(echelle * i / NB_GRADUATIONS);
            g2.setColor(Theme.TEXTE_DOUX);
            g2.drawString(valeur,
                    MARGE_GAUCHE - 8 - metriques.stringWidth(valeur),
                    y + metriques.getAscent() / 2 - 1);
        }
    }

    /** Barres verticales, valeur au sommet et libellé du mois en dessous. */
    private void dessinerBarres(Graphics2D g2, int largeurZone, int hauteurZone, long echelle) {
        int nombre = recettes.size();
        int pas = largeurZone / nombre;
        int largeurBarre = Math.min(LARGEUR_BARRE_MAX, (int) (pas * 0.55));

        FontMetrics metriques = g2.getFontMetrics();

        for (int i = 0; i < nombre; i++) {
            BilanService.RecetteMensuelle recette = recettes.get(i);

            int centre = MARGE_GAUCHE + pas * i + pas / 2;
            int x = centre - largeurBarre / 2;

            int hauteurBarre = echelle == 0 ? 0
                    : (int) (hauteurZone * (double) recette.montant() / echelle);
            int y = MARGE_HAUT + hauteurZone - hauteurBarre;

            // Barre : dégradé simple obtenu par deux tons du vert de la charte.
            if (hauteurBarre > 0) {
                g2.setColor(Theme.VERT);
                g2.fillRoundRect(x, y, largeurBarre, hauteurBarre, 6, 6);
                g2.setColor(Theme.VERT_FONCE);
                g2.fillRect(x, MARGE_HAUT + hauteurZone - 3, largeurBarre, 3);
            } else {
                // Mois sans vente : un trait discret marque quand même la colonne.
                g2.setColor(Theme.BORDURE);
                g2.fillRect(x, MARGE_HAUT + hauteurZone - 2, largeurBarre, 2);
            }

            // Montant au-dessus de la barre
            String montant = MontantUtil.format(recette.montant());
            g2.setFont(Theme.POLICE_LIBELLE);
            g2.setColor(Theme.TEXTE);
            g2.drawString(montant,
                    centre - g2.getFontMetrics().stringWidth(montant) / 2,
                    Math.max(MARGE_HAUT + 10, y - 6));

            // Libellé du mois sous l'axe
            String mois = DateUtil.formatMois(recette.mois());
            g2.setFont(Theme.POLICE_PETITE);
            metriques = g2.getFontMetrics();
            g2.setColor(Theme.TEXTE_DOUX);
            g2.drawString(mois,
                    centre - metriques.stringWidth(mois) / 2,
                    MARGE_HAUT + hauteurZone + metriques.getAscent() + 10);
        }
    }

    private void dessinerMessageVide(Graphics2D g2) {
        String message = "Aucune donnée à afficher";
        g2.setFont(Theme.POLICE_NORMALE);
        g2.setColor(Theme.TEXTE_DOUX);
        FontMetrics metriques = g2.getFontMetrics();
        g2.drawString(message,
                (getWidth() - metriques.stringWidth(message)) / 2,
                getHeight() / 2);
    }

    /**
     * Arrondit le maximum vers le haut pour obtenir une échelle lisible.
     *
     * Sans cet arrondi, les valeurs de l'axe vertical seraient des nombres
     * quelconques difficiles à lire.
     */
    private long calculerEchelle(long maximum) {
        if (maximum <= 0) return NB_GRADUATIONS;

        long puissance = 1;
        while (puissance * 10 <= maximum) {
            puissance *= 10;
        }

        long pas = puissance * NB_GRADUATIONS;
        return ((maximum + pas - 1) / pas) * pas;
    }
}
