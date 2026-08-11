package pharmacie.ui;

import pharmacie.dao.AchatDAO;
import pharmacie.model.Medicament;
import pharmacie.service.BilanService;
import pharmacie.ui.components.BoutonAction;
import pharmacie.ui.components.Carte;
import pharmacie.ui.components.CarteIndicateur;
import pharmacie.ui.components.Dialogues;
import pharmacie.ui.components.UIFactory;
import pharmacie.util.BusinessException;
import pharmacie.util.MontantUtil;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Tableau de bord : vue d'ensemble de l'état de la pharmacie.
 *
 * Présente les quatre indicateurs clés, le classement des médicaments les plus
 * vendus et la liste des stocks nécessitant un réapprovisionnement.
 */
public class DashboardPanel extends JPanel {

    private final BilanService bilanService = new BilanService();
    private final MainFrame fenetrePrincipale;

    private final CarteIndicateur carteMedicaments =
            new CarteIndicateur("Médicaments", "0", "medicament", Theme.BLEU);
    private final CarteIndicateur carteStockFaible =
            new CarteIndicateur("Stock faible", "0", "alert", Theme.ORANGE);
    private final CarteIndicateur carteRecette =
            new CarteIndicateur("Recette totale", "0", "money", Theme.VERT);
    private final CarteIndicateur carteAchats =
            new CarteIndicateur("Achats", "0", "achat", new java.awt.Color(0x7C, 0x3A, 0xAF));

    private final DefaultTableModel modeleTopVentes = new DefaultTableModel(
            new String[]{"Rang", "Médicament", "Quantité vendue", "Montant"}, 0) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };

    private final DefaultTableModel modeleAlertes = new DefaultTableModel(
            new String[]{"Médicament", "Stock", "État"}, 0) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };

    private final JTable tableTopVentes = new JTable(modeleTopVentes);
    private final JTable tableAlertes = new JTable(modeleAlertes);

    public DashboardPanel(MainFrame fenetrePrincipale) {
        this.fenetrePrincipale = fenetrePrincipale;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        add(construireEntete(), BorderLayout.NORTH);
        add(construireCorps(), BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    //  Construction
    // ------------------------------------------------------------------

    private JPanel construireEntete() {
        BoutonAction actualiser = BoutonAction.secondaire("Actualiser", "refresh");
        actualiser.addActionListener(e -> rafraichir());

        BoutonAction nouvelleVente = BoutonAction.principal("Nouvelle vente", "achat");
        nouvelleVente.addActionListener(e -> fenetrePrincipale.afficherVue(MainFrame.VUE_ACHAT));

        return UIFactory.enteteEcran("Tableau de bord",
                "Vue d'ensemble de l'activité de la pharmacie",
                actualiser, nouvelleVente);
    }

    private JPanel construireCorps() {
        JPanel corps = new JPanel(new BorderLayout(0, 16));
        corps.setOpaque(false);

        corps.add(construireCartes(), BorderLayout.NORTH);
        corps.add(construireTableaux(), BorderLayout.CENTER);
        return corps;
    }

    private JPanel construireCartes() {
        JPanel cartes = new JPanel(new GridLayout(1, 4, 16, 0));
        cartes.setOpaque(false);
        cartes.add(carteMedicaments);
        cartes.add(carteStockFaible);
        cartes.add(carteRecette);
        cartes.add(carteAchats);
        return cartes;
    }

    private JPanel construireTableaux() {
        JPanel tableaux = new JPanel(new GridLayout(1, 2, 16, 0));
        tableaux.setOpaque(false);

        tableaux.add(construireCarteTable(
                "Médicaments les plus vendus",
                "Les 5 meilleures ventes, toutes périodes confondues",
                tableTopVentes));

        tableaux.add(construireCarteTable(
                "Alertes de stock",
                "Médicaments dont le stock est inférieur à " + Medicament.SEUIL_STOCK_FAIBLE,
                tableAlertes));

        return tableaux;
    }

    /** Assemble une carte contenant un titre, une description et un tableau. */
    private Carte construireCarteTable(String titre, String description, JTable table) {
        UIFactory.styliserTable(table);

        Carte carte = new Carte(new BorderLayout(0, 12));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        JPanel entete = new JPanel(new BorderLayout(0, 3));
        entete.setOpaque(false);
        entete.add(UIFactory.sousTitre(titre), BorderLayout.NORTH);
        entete.add(UIFactory.aide(description), BorderLayout.CENTER);

        carte.add(entete, BorderLayout.NORTH);
        carte.add(UIFactory.defilement(table), BorderLayout.CENTER);
        return carte;
    }

    // ------------------------------------------------------------------
    //  Données
    // ------------------------------------------------------------------

    /** Recharge les indicateurs et les tableaux depuis la base. */
    public void rafraichir() {
        try {
            chargerIndicateurs();
            chargerTopVentes();
            chargerAlertes();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void chargerIndicateurs() throws BusinessException {
        BilanService.Indicateurs indicateurs = bilanService.chargerIndicateurs();
        carteMedicaments.setValeur(String.valueOf(indicateurs.nombreMedicaments()));
        carteStockFaible.setValeur(String.valueOf(indicateurs.nombreStockFaible()));
        carteRecette.setValeur(MontantUtil.formatAvecDevise(indicateurs.recetteTotale()));
        carteAchats.setValeur(String.valueOf(indicateurs.nombreAchats()));
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

        UIFactory.largeurColonne(tableTopVentes, 0, 45);
        UIFactory.alignerAuCentre(tableTopVentes, 0);
        UIFactory.alignerADroite(tableTopVentes, 2);
        UIFactory.alignerADroite(tableTopVentes, 3);
    }

    private void chargerAlertes() throws BusinessException {
        modeleAlertes.setRowCount(0);
        List<Medicament> medicaments = bilanService.stockFaible();

        for (Medicament medicament : medicaments) {
            modeleAlertes.addRow(new Object[]{
                    medicament.getDesign(),
                    medicament.getStock(),
                    medicament.getEtatStock()});
        }

        UIFactory.alignerAuCentre(tableAlertes, 1);
        tableAlertes.getColumnModel().getColumn(2)
                .setCellRenderer(new UIFactory.RenduEtatStock());
        UIFactory.largeurColonne(tableAlertes, 1, 70);
        UIFactory.largeurColonne(tableAlertes, 2, 110);

        tableAlertes.setToolTipText(medicaments.isEmpty()
                ? "Aucun médicament en stock faible."
                : medicaments.size() + " médicament(s) à réapprovisionner.");
    }
}
