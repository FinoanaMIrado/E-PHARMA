package pharmacie.ui;

import pharmacie.model.Entree;
import pharmacie.model.Medicament;
import pharmacie.service.StockService;
import pharmacie.ui.components.BoutonAction;
import pharmacie.ui.components.Carte;
import pharmacie.ui.components.Dialogues;
import pharmacie.ui.components.UIFactory;
import pharmacie.util.BusinessException;
import pharmacie.util.DateUtil;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;

/**
 * STOCK → ENTRÉES : enregistrement des approvisionnements.
 *
 * La validation d'une entrée augmente immédiatement le stock du médicament
 * concerné ; sa modification ou sa suppression corrige le stock en conséquence.
 */
public class EntreePanel extends JPanel {

    private final StockService stockService = new StockService();

    /** Action exécutée après toute opération modifiant le stock. */
    private final Runnable apresModificationStock;

    private final JTextField champNumero = UIFactory.champTexte(12);
    private final JComboBox<Medicament> listeMedicaments = new JComboBox<>();
    private final JTextField champQuantite = UIFactory.champTexte(10);
    private final JTextField champDate = UIFactory.champTexte(12);

    private final DefaultTableModel modele = new DefaultTableModel(
            new String[]{"N° entrée", "Médicament", "Quantité", "Date"}, 0) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };

    private final JTable table = new JTable(modele);

    private List<Entree> entreesAffichees = List.of();

    private BoutonAction boutonEnregistrer;
    private BoutonAction boutonModifier;
    private BoutonAction boutonSupprimer;

    public EntreePanel(Runnable apresModificationStock) {
        this.apresModificationStock = apresModificationStock;

        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        add(construireFormulaire(), BorderLayout.NORTH);
        add(construireTableau(), BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    //  Construction
    // ------------------------------------------------------------------

    private Carte construireFormulaire() {
        Carte carte = new Carte(new BorderLayout(0, 14));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        carte.add(UIFactory.sousTitre("Nouvelle entrée de stock"), BorderLayout.NORTH);
        carte.add(construireChamps(), BorderLayout.CENTER);
        carte.add(construireBoutons(), BorderLayout.SOUTH);

        return carte;
    }

    private JPanel construireChamps() {
        JPanel champs = UIFactory.panneauTransparent(new GridBagLayout());
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.anchor = GridBagConstraints.WEST;
        contraintes.insets = new Insets(0, 0, 0, 18);

        UIFactory.styliserChamp(listeMedicaments);
        listeMedicaments.setPreferredSize(new Dimension(280, 34));
        listeMedicaments.setBackground(Theme.BLANC);

        ajouterBloc(champs, contraintes, 0, "N° ENTRÉE *", champNumero);
        ajouterBloc(champs, contraintes, 1, "MÉDICAMENT *", listeMedicaments);
        ajouterBloc(champs, contraintes, 2, "QUANTITÉ *", champQuantite);
        ajouterBloc(champs, contraintes, 3, "DATE * (jj/mm/aaaa)", champDate);

        contraintes.gridx = 4;
        contraintes.weightx = 1;
        champs.add(UIFactory.panneauTransparent(new BorderLayout()), contraintes);

        return champs;
    }

    private void ajouterBloc(JPanel parent, GridBagConstraints contraintes,
                             int colonne, String libelle, java.awt.Component champ) {
        JPanel bloc = UIFactory.panneauTransparent(new BorderLayout(0, 6));
        bloc.add(UIFactory.libelle(libelle), BorderLayout.NORTH);
        bloc.add(champ, BorderLayout.CENTER);

        contraintes.gridx = colonne;
        contraintes.gridy = 0;
        contraintes.weightx = 0;
        parent.add(bloc, contraintes);
    }

    private JPanel construireBoutons() {
        boutonEnregistrer = BoutonAction.principal("Enregistrer", "save");
        boutonEnregistrer.addActionListener(e -> enregistrer());

        boutonModifier = BoutonAction.info("Modifier", "edit");
        boutonModifier.addActionListener(e -> modifier());

        boutonSupprimer = BoutonAction.danger("Supprimer", "delete");
        boutonSupprimer.addActionListener(e -> supprimer());

        BoutonAction boutonVider = BoutonAction.secondaire("Vider", "clear");
        boutonVider.addActionListener(e -> viderFormulaire());

        JPanel barre = UIFactory.barreBoutons(boutonEnregistrer, boutonModifier, boutonSupprimer, boutonVider);
        barre.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        return barre;
    }

    private Carte construireTableau() {
        Carte carte = new Carte(new BorderLayout(0, 12));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        JPanel entete = UIFactory.panneauTransparent(new BorderLayout(0, 3));
        entete.add(UIFactory.sousTitre("Historique des entrées"), BorderLayout.NORTH);
        entete.add(UIFactory.aide("Sélectionnez une ligne pour la modifier ou la supprimer"),
                BorderLayout.CENTER);
        carte.add(entete, BorderLayout.NORTH);

        UIFactory.styliserTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) remplirFormulaireDepuisSelection();
        });

        carte.add(UIFactory.defilement(table), BorderLayout.CENTER);
        return carte;
    }

    // ------------------------------------------------------------------
    //  Actions
    // ------------------------------------------------------------------

    private void enregistrer() {
        Medicament medicament = (Medicament) listeMedicaments.getSelectedItem();
        if (medicament == null) {
            Dialogues.avertissement(this, "Sélectionnez un médicament.");
            return;
        }

        try {
            stockService.creerEntree(champNumero.getText(), medicament.getNumMedoc(),
                    champQuantite.getText(), DateUtil.parse(champDate.getText()));
            Dialogues.succes(this, "Entrée enregistrée avec succès.\nLe stock a été mis à jour.");
            viderFormulaire();
            rafraichir();
            apresModificationStock.run();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void modifier() {
        Entree selection = entreeSelectionnee();
        if (selection == null) {
            Dialogues.avertissement(this, "Sélectionnez l'entrée à modifier dans le tableau.");
            return;
        }

        Medicament medicament = (Medicament) listeMedicaments.getSelectedItem();
        if (medicament == null) {
            Dialogues.avertissement(this, "Sélectionnez un médicament.");
            return;
        }

        try {
            stockService.modifierEntree(selection.getNumEntree(), medicament.getNumMedoc(),
                    champQuantite.getText(), DateUtil.parse(champDate.getText()));
            Dialogues.succes(this, "Entrée modifiée avec succès.\nLe stock a été ajusté.");
            viderFormulaire();
            rafraichir();
            apresModificationStock.run();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void supprimer() {
        Entree selection = entreeSelectionnee();
        if (selection == null) {
            Dialogues.avertissement(this, "Sélectionnez l'entrée à supprimer dans le tableau.");
            return;
        }

        boolean confirme = Dialogues.confirmer(this,
                "Voulez-vous vraiment supprimer l'entrée « " + selection.getNumEntree() + " » ?\n"
                        + "La quantité correspondante sera retirée du stock.",
                "Confirmer la suppression");
        if (!confirme) return;

        try {
            stockService.supprimerEntree(selection.getNumEntree());
            Dialogues.succes(this, "Entrée supprimée avec succès.");
            viderFormulaire();
            rafraichir();
            apresModificationStock.run();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    /** Recharge la liste des médicaments et l'historique des entrées. */
    public void rafraichir() {
        try {
            chargerMedicaments();
            afficher(stockService.listerEntrees());

            if (champNumero.getText().isBlank()) {
                preparerNouvelleEntree();
            }
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  Chargement des données
    // ------------------------------------------------------------------

    private void chargerMedicaments() throws BusinessException {
        Medicament selectionPrecedente = (Medicament) listeMedicaments.getSelectedItem();
        List<Medicament> medicaments = stockService.listerMedicaments();

        listeMedicaments.setModel(new DefaultComboBoxModel<>(medicaments.toArray(new Medicament[0])));

        if (selectionPrecedente != null) {
            selectionnerMedicament(selectionPrecedente.getNumMedoc());
        } else if (!medicaments.isEmpty()) {
            listeMedicaments.setSelectedIndex(0);
        }
    }

    private void afficher(List<Entree> entrees) {
        entreesAffichees = entrees;
        modele.setRowCount(0);

        for (Entree entree : entrees) {
            modele.addRow(new Object[]{
                    entree.getNumEntree(),
                    entree.getDesignMedicament(),
                    entree.getStockEntree(),
                    DateUtil.format(entree.getDateEntree())});
        }

        UIFactory.largeurColonne(table, 0, 100);
        UIFactory.largeurColonne(table, 1, 300);
        UIFactory.alignerAuCentre(table, 2);
        UIFactory.largeurColonne(table, 2, 90);
        UIFactory.alignerAuCentre(table, 3);
        UIFactory.largeurColonne(table, 3, 110);
    }

    private Entree entreeSelectionnee() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) return null;
        int index = table.convertRowIndexToModel(ligne);
        if (index < 0 || index >= entreesAffichees.size()) return null;
        return entreesAffichees.get(index);
    }

    private void remplirFormulaireDepuisSelection() {
        Entree entree = entreeSelectionnee();
        if (entree == null) return;

        champNumero.setText(entree.getNumEntree());
        champQuantite.setText(String.valueOf(entree.getStockEntree()));
        champDate.setText(DateUtil.format(entree.getDateEntree()));
        selectionnerMedicament(entree.getNumMedoc());

        champNumero.setEditable(false);
        champNumero.setBackground(Theme.FOND);
        boutonEnregistrer.setEnabled(false);
        boutonModifier.setEnabled(true);
        boutonSupprimer.setEnabled(true);
    }

    private void selectionnerMedicament(String numMedoc) {
        for (int i = 0; i < listeMedicaments.getItemCount(); i++) {
            if (listeMedicaments.getItemAt(i).getNumMedoc().equals(numMedoc)) {
                listeMedicaments.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Prépare le formulaire pour une nouvelle saisie : numéro proposé et date du jour. */
    private void preparerNouvelleEntree() {
        try {
            champNumero.setText(stockService.prochainNumeroEntree());
        } catch (BusinessException e) {
            champNumero.setText("");
        }
        champDate.setText(DateUtil.format(LocalDate.now()));
    }

    private void viderFormulaire() {
        table.clearSelection();

        champNumero.setText("");
        champQuantite.setText("");
        champNumero.setEditable(true);
        champNumero.setBackground(Theme.BLANC);

        boutonEnregistrer.setEnabled(true);
        boutonModifier.setEnabled(false);
        boutonSupprimer.setEnabled(false);

        preparerNouvelleEntree();
    }
}
