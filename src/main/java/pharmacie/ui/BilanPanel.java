package pharmacie.ui;

import pharmacie.dao.AchatDAO;
import pharmacie.model.Medicament;
import pharmacie.service.BilanService;
import pharmacie.ui.components.BoutonAction;
import pharmacie.ui.components.Carte;
import pharmacie.ui.components.Dialogues;
import pharmacie.ui.components.HistogrammeRecettes;
import pharmacie.ui.components.UIFactory;
import pharmacie.util.BusinessException;
import pharmacie.util.IconLoader;
import pharmacie.util.MontantUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

/**
 * Écran BILAN : recette totale, meilleures ventes, stocks faibles et
 * histogramme des recettes des cinq derniers mois.
 */
public class BilanPanel extends JPanel {

    private final BilanService bilanService = new BilanService();

    private final JLabel labelRecette = UIFactory.valeurMiseEnAvant("0 Ar", Theme.VERT, 34);
    private final JLabel labelNombreAchats = UIFactory.aide("Achats enregistrés : 0");
    private final JLabel labelQuantiteVendue = UIFactory.aide("Médicaments référencés : 0");

    private final DefaultTableModel modeleTopVentes = new DefaultTableModel(
            new String[]{"Rang", "Médicament", "Quantité vendue", "Montant"}, 0) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };

    private final DefaultTableModel modeleStockFaible = new DefaultTableModel(
            new String[]{"N°", "Médicament", "Stock", "État"}, 0) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };

    private final JTable tableTopVentes = new JTable(modeleTopVentes);
    private final JTable tableStockFaible = new JTable(modeleStockFaible);
    private final HistogrammeRecettes histogramme = new HistogrammeRecettes();

    public BilanPanel() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        add(construireEntete(), BorderLayout.NORTH);

        // Le bilan est plus haut qu'un petit écran : un défilement vertical
        // garantit que l'histogramme et les tableaux restent tous accessibles.
        JScrollPane defilement = new JScrollPane(construireCorps());
        defilement.setBorder(BorderFactory.createEmptyBorder());
        defilement.setOpaque(false);
        defilement.getViewport().setOpaque(false);
        defilement.getVerticalScrollBar().setUnitIncrement(16);
        defilement.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(defilement, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    //  Construction
    // ------------------------------------------------------------------

    private JPanel construireEntete() {
        BoutonAction actualiser = BoutonAction.secondaire("Actualiser", "refresh");
        actualiser.addActionListener(e -> rafraichir());

        return UIFactory.enteteEcran("Bilan",
                "Recette, meilleures ventes, stocks faibles et évolution mensuelle",
                actualiser);
    }

    private JPanel construireCorps() {
        JPanel corps = UIFactory.panneauTransparent(new BorderLayout(0, 16));
        corps.add(construireHaut(), BorderLayout.NORTH);
        corps.add(construireBas(), BorderLayout.CENTER);
        return corps;
    }

    /** Partie haute : recette totale et histogramme. */
    private JPanel construireHaut() {
        JPanel haut = UIFactory.panneauTransparent(new BorderLayout(16, 0));
        haut.setPreferredSize(new Dimension(0, 300));
        haut.add(construireCarteRecette(), BorderLayout.WEST);
        haut.add(construireCarteHistogramme(), BorderLayout.CENTER);
        return haut;
    }

    private Carte construireCarteRecette() {
        Carte carte = new Carte();
        carte.setBorder(Carte.marge(20, 20, 20, 20));
        carte.setLayout(new BoxLayout(carte, BoxLayout.Y_AXIS));
        carte.setPreferredSize(new Dimension(280, 0));

        JLabel icone = new JLabel(IconLoader.get("money", 30));
        icone.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titre = UIFactory.libelle("RECETTE TOTALE");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);

        labelRecette.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel aide = UIFactory.aide("Somme des ventes enregistrées");
        aide.setAlignmentX(Component.LEFT_ALIGNMENT);

        labelNombreAchats.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelQuantiteVendue.setAlignmentX(Component.LEFT_ALIGNMENT);

        carte.add(icone);
        carte.add(Box.createVerticalStrut(14));
        carte.add(titre);
        carte.add(Box.createVerticalStrut(8));
        carte.add(labelRecette);
        carte.add(Box.createVerticalStrut(6));
        carte.add(aide);
        carte.add(Box.createVerticalStrut(20));
        carte.add(separateur());
        carte.add(Box.createVerticalStrut(16));
        carte.add(labelNombreAchats);
        carte.add(Box.createVerticalStrut(10));
        carte.add(labelQuantiteVendue);
        carte.add(Box.createVerticalGlue());

        return carte;
    }

    /** Trait horizontal séparant la recette des indicateurs complémentaires. */
    private JPanel separateur() {
        JPanel trait = new JPanel();
        trait.setBackground(Theme.BORDURE);
        trait.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        trait.setAlignmentX(Component.LEFT_ALIGNMENT);
        return trait;
    }

    private Carte construireCarteHistogramme() {
        Carte carte = new Carte(new BorderLayout(0, 10));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        JPanel entete = UIFactory.panneauTransparent(new BorderLayout(0, 3));
        entete.add(UIFactory.sousTitre("Recettes des "
                + BilanService.NB_MOIS_HISTOGRAMME + " derniers mois"), BorderLayout.NORTH);
        entete.add(UIFactory.aide("Montant total des ventes par mois"), BorderLayout.CENTER);

        carte.add(entete, BorderLayout.NORTH);
        carte.add(histogramme, BorderLayout.CENTER);
        return carte;
    }

    /** Partie basse : top 5 et stocks faibles côte à côte. */
    private JPanel construireBas() {
        JPanel bas = new JPanel(new GridLayout(1, 2, 16, 0));
        bas.setOpaque(false);
        bas.setPreferredSize(new Dimension(0, 260));

        bas.add(construireCarteTable(tableTopVentes,
                "Top " + BilanService.TAILLE_TOP_VENTES + " des médicaments vendus",
                "Classement par quantité totale vendue"));

        bas.add(construireCarteTable(tableStockFaible,
                "Médicaments en stock faible",
                "Stock strictement inférieur à " + Medicament.SEUIL_STOCK_FAIBLE));

        return bas;
    }

    private Carte construireCarteTable(JTable table, String titre, String description) {
        UIFactory.styliserTable(table);

        Carte carte = new Carte(new BorderLayout(0, 12));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        JPanel entete = UIFactory.panneauTransparent(new BorderLayout(0, 3));
        entete.add(UIFactory.sousTitre(titre), BorderLayout.NORTH);
        entete.add(UIFactory.aide(description), BorderLayout.CENTER);

        carte.add(entete, BorderLayout.NORTH);
        carte.add(UIFactory.defilement(table), BorderLayout.CENTER);
        return carte;
    }

    // ------------------------------------------------------------------
    //  Données
    // ------------------------------------------------------------------

    /** Recharge l'ensemble des indicateurs du bilan. */
    public void rafraichir() {
        try {
            BilanService.Indicateurs indicateurs = bilanService.chargerIndicateurs();
            labelRecette.setText(MontantUtil.formatAvecDevise(indicateurs.recetteTotale()));
            labelNombreAchats.setText("Achats enregistrés : " + indicateurs.nombreAchats());
            labelQuantiteVendue.setText("Médicaments référencés : " + indicateurs.nombreMedicaments());

            chargerTopVentes();
            chargerStockFaible();
            histogramme.setRecettes(bilanService.recettesDerniersMois());
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void chargerTopVentes() throws BusinessException {
        modeleTopVentes.setRowCount(0);
        List<AchatDAO.VenteParMedicament> ventes = bilanService.topVentes();

        int rang = 1;
        for (AchatDAO.VenteParMedicament vente : ventes) {
            modeleTopVentes.addRow(new Object[]{
                    rang++,
                    vente.design(),
                    vente.quantiteVendue(),
                    MontantUtil.formatAvecDevise(vente.montantTotal())});
        }

        UIFactory.largeurColonne(tableTopVentes, 0, 50);
        UIFactory.alignerAuCentre(tableTopVentes, 0);
        UIFactory.alignerADroite(tableTopVentes, 2);
        UIFactory.alignerADroite(tableTopVentes, 3);
    }

    private void chargerStockFaible() throws BusinessException {
        modeleStockFaible.setRowCount(0);
        List<Medicament> medicaments = bilanService.stockFaible();

        for (Medicament medicament : medicaments) {
            modeleStockFaible.addRow(new Object[]{
                    medicament.getNumMedoc(),
                    medicament.getDesign(),
                    medicament.getStock(),
                    medicament.getEtatStock()});
        }

        UIFactory.largeurColonne(tableStockFaible, 0, 70);
        UIFactory.alignerAuCentre(tableStockFaible, 2);
        UIFactory.largeurColonne(tableStockFaible, 2, 70);
        UIFactory.largeurColonne(tableStockFaible, 3, 110);
        tableStockFaible.getColumnModel().getColumn(3)
                .setCellRenderer(new UIFactory.RenduEtatStock());
    }
}
