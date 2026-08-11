package pharmacie.ui.components;

import pharmacie.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Fabrique de composants Swing préconfigurés selon la charte graphique.
 *
 * Centralise la mise en forme afin que tous les écrans partagent exactement la
 * même apparence, sans dupliquer les réglages dans chaque panneau.
 */
public final class UIFactory {

    private UIFactory() {
    }

    // ------------------------------------------------------------------
    //  Textes
    // ------------------------------------------------------------------

    /** Titre principal d'un écran. */
    public static JLabel titre(String texte) {
        JLabel label = new JLabel(texte);
        label.setFont(Theme.POLICE_TITRE);
        label.setForeground(Theme.TEXTE);
        return label;
    }

    /** Titre de section, à l'intérieur d'une carte. */
    public static JLabel sousTitre(String texte) {
        JLabel label = new JLabel(texte);
        label.setFont(Theme.POLICE_SOUS_TITRE);
        label.setForeground(Theme.TEXTE);
        return label;
    }

    /** Libellé de champ de formulaire. */
    public static JLabel libelle(String texte) {
        JLabel label = new JLabel(texte);
        label.setFont(Theme.POLICE_LIBELLE);
        label.setForeground(Theme.TEXTE_DOUX);
        return label;
    }

    /** Texte d'explication en gris. */
    public static JLabel aide(String texte) {
        JLabel label = new JLabel(texte);
        label.setFont(Theme.POLICE_PETITE);
        label.setForeground(Theme.TEXTE_DOUX);
        return label;
    }

    // ------------------------------------------------------------------
    //  Saisie
    // ------------------------------------------------------------------

    /** Champ de saisie texte au style de l'application. */
    public static JTextField champTexte(int colonnes) {
        JTextField champ = new JTextField(colonnes);
        styliserChamp(champ);
        return champ;
    }

    /** Applique la bordure et la hauteur standard à un champ de saisie. */
    public static void styliserChamp(JComponent champ) {
        champ.setFont(Theme.POLICE_NORMALE);
        champ.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDURE),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        champ.setBackground(Theme.BLANC);
        Dimension taille = champ.getPreferredSize();
        champ.setPreferredSize(new Dimension(taille.width, Math.max(taille.height, 34)));
    }

    /** Champ affichant une valeur non modifiable (numéro généré, stock calculé…). */
    public static JTextField champLectureSeule(int colonnes) {
        JTextField champ = champTexte(colonnes);
        champ.setEditable(false);
        champ.setBackground(Theme.FOND);
        champ.setForeground(Theme.TEXTE_DOUX);
        return champ;
    }

    // ------------------------------------------------------------------
    //  Conteneurs
    // ------------------------------------------------------------------

    /** Panneau transparent, utilisé pour composer des mises en page. */
    public static JPanel panneauTransparent(java.awt.LayoutManager layout) {
        JPanel panneau = new JPanel(layout);
        panneau.setOpaque(false);
        return panneau;
    }

    /** Barre horizontale de boutons alignés à gauche. */
    public static JPanel barreBoutons(JComponent... boutons) {
        JPanel barre = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barre.setOpaque(false);
        for (JComponent bouton : boutons) {
            barre.add(bouton);
        }
        return barre;
    }

    /** Barre horizontale de boutons alignés à droite. */
    public static JPanel barreBoutonsDroite(JComponent... boutons) {
        JPanel barre = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        barre.setOpaque(false);
        for (JComponent bouton : boutons) {
            barre.add(bouton);
        }
        return barre;
    }

    // ------------------------------------------------------------------
    //  Tableaux
    // ------------------------------------------------------------------

    /**
     * Applique la mise en forme standard à un tableau : en-tête lisible, lignes
     * alternées, sélection d'une seule ligne et hauteur de ligne confortable.
     */
    public static void styliserTable(JTable table) {
        table.setFont(Theme.POLICE_NORMALE);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.BORDURE);
        table.setSelectionBackground(Theme.SELECTION);
        table.setSelectionForeground(Theme.TEXTE);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader entete = table.getTableHeader();
        entete.setFont(Theme.POLICE_LIBELLE);
        entete.setBackground(Theme.FOND);
        entete.setForeground(Theme.TEXTE_DOUX);
        entete.setPreferredSize(new Dimension(entete.getPreferredSize().width, 34));
        entete.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) entete.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        table.setDefaultRenderer(Object.class, new RenduLigneAlternee());
    }

    /** Enveloppe un tableau dans une zone défilante sans bordure superflue. */
    public static JScrollPane defilement(JTable table) {
        JScrollPane defilement = new JScrollPane(table);
        defilement.setBorder(BorderFactory.createLineBorder(Theme.BORDURE));
        defilement.getViewport().setBackground(Theme.BLANC);
        defilement.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return defilement;
    }

    /** Aligne une colonne à droite (montants, quantités). */
    public static void alignerADroite(JTable table, int colonne) {
        table.getColumnModel().getColumn(colonne).setCellRenderer(new RenduLigneAlternee() {
            {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        });
    }

    /** Aligne une colonne au centre. */
    public static void alignerAuCentre(JTable table, int colonne) {
        table.getColumnModel().getColumn(colonne).setCellRenderer(new RenduLigneAlternee() {
            {
                setHorizontalAlignment(SwingConstants.CENTER);
            }
        });
    }

    /** Fixe la largeur préférée d'une colonne. */
    public static void largeurColonne(JTable table, int colonne, int largeur) {
        table.getColumnModel().getColumn(colonne).setPreferredWidth(largeur);
    }

    /**
     * Rendu de cellule alternant la couleur de fond une ligne sur deux.
     *
     * Améliore nettement la lisibilité des tableaux larges.
     */
    public static class RenduLigneAlternee extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object valeur, boolean selectionne,
                                                       boolean focus, int ligne, int colonne) {
            Component composant = super.getTableCellRendererComponent(
                    table, valeur, selectionne, focus, ligne, colonne);
            if (!selectionne) {
                composant.setBackground(ligne % 2 == 0 ? Theme.BLANC : Theme.LIGNE_ALTERNEE);
                composant.setForeground(Theme.TEXTE);
            }
            setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
            return composant;
        }
    }

    /**
     * Rendu colorant la cellule selon l'état du stock.
     *
     * Rouge en rupture, orange en stock faible : l'information reste lisible
     * sans dépendre d'un symbole ou d'un emoji.
     */
    public static class RenduEtatStock extends RenduLigneAlternee {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object valeur, boolean selectionne,
                                                       boolean focus, int ligne, int colonne) {
            Component composant = super.getTableCellRendererComponent(
                    table, valeur, selectionne, focus, ligne, colonne);
            String texte = valeur == null ? "" : valeur.toString();

            Color couleur = switch (texte) {
                case "Rupture" -> Theme.ROUGE;
                case "Stock faible" -> Theme.ORANGE;
                default -> Theme.TEXTE;
            };
            composant.setForeground(selectionne ? Theme.TEXTE : couleur);
            composant.setFont(texte.equals("Normal") ? Theme.POLICE_NORMALE : Theme.POLICE_GRASSE);
            return composant;
        }
    }

    /** En-tête d'écran : titre à gauche, actions éventuelles à droite. */
    public static JPanel enteteEcran(String titre, String description, JComponent... actions) {
        JPanel entete = new JPanel(new BorderLayout(12, 0));
        entete.setOpaque(false);

        JPanel textes = new JPanel();
        textes.setOpaque(false);
        textes.setLayout(new javax.swing.BoxLayout(textes, javax.swing.BoxLayout.Y_AXIS));
        JLabel titreLabel = titre(titre);
        titreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textes.add(titreLabel);

        if (description != null && !description.isBlank()) {
            JLabel descriptionLabel = aide(description);
            descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textes.add(javax.swing.Box.createVerticalStrut(3));
            textes.add(descriptionLabel);
        }

        entete.add(textes, BorderLayout.WEST);
        if (actions.length > 0) {
            entete.add(barreBoutonsDroite(actions), BorderLayout.EAST);
        }
        return entete;
    }

    /** Étiquette colorée servant de valeur mise en avant (recette totale…). */
    public static JLabel valeurMiseEnAvant(String texte, Color couleur, int taillePolice) {
        JLabel label = new JLabel(texte);
        label.setFont(new Font(Theme.POLICE_TITRE.getFamily(), Font.BOLD, taillePolice));
        label.setForeground(couleur);
        return label;
    }
}
