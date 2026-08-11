package pharmacie.ui;

import pharmacie.model.Medicament;
import pharmacie.service.StockService;
import pharmacie.ui.components.BoutonAction;
import pharmacie.ui.components.Carte;
import pharmacie.ui.components.Dialogues;
import pharmacie.ui.components.UIFactory;
import pharmacie.util.BusinessException;
import pharmacie.util.MontantUtil;

import javax.swing.BorderFactory;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * STOCK → MÉDICAMENTS : CRUD complet et recherche par désignation.
 *
 * Le champ « Stock » est affiché en lecture seule : à la création il vaut
 * toujours 0, et il n'évolue ensuite que par les entrées et les achats.
 */
public class MedicamentPanel extends JPanel {

    private final StockService stockService = new StockService();

    private final JTextField champNumero = UIFactory.champTexte(12);
    private final JTextField champDesignation = UIFactory.champTexte(24);
    private final JTextField champPrix = UIFactory.champTexte(12);
    private final JTextField champStock = UIFactory.champLectureSeule(8);
    private final JTextField champRecherche = UIFactory.champTexte(24);

    private final DefaultTableModel modele = new DefaultTableModel(
            new String[]{"N°", "Désignation", "Prix unitaire", "Stock", "État"}, 0) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };

    private final JTable table = new JTable(modele);

    /** Médicaments actuellement affichés, dans l'ordre du tableau. */
    private List<Medicament> medicamentsAffiches = List.of();

    private BoutonAction boutonAjouter;
    private BoutonAction boutonModifier;
    private BoutonAction boutonSupprimer;

    public MedicamentPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        add(construireFormulaire(), BorderLayout.NORTH);
        add(construireTableau(), BorderLayout.CENTER);

        viderFormulaire();
    }

    // ------------------------------------------------------------------
    //  Construction
    // ------------------------------------------------------------------

    private Carte construireFormulaire() {
        Carte carte = new Carte(new BorderLayout(0, 14));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        carte.add(UIFactory.sousTitre("Fiche médicament"), BorderLayout.NORTH);
        carte.add(construireChamps(), BorderLayout.CENTER);
        carte.add(construireBoutons(), BorderLayout.SOUTH);

        return carte;
    }

    private JPanel construireChamps() {
        JPanel champs = UIFactory.panneauTransparent(new GridBagLayout());
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.anchor = GridBagConstraints.WEST;
        contraintes.insets = new Insets(0, 0, 0, 18);

        ajouterChamp(champs, contraintes, 0, "NUMÉRO *", champNumero);
        ajouterChamp(champs, contraintes, 1, "DÉSIGNATION *", champDesignation);
        ajouterChamp(champs, contraintes, 2, "PRIX UNITAIRE *", champPrix);
        ajouterChamp(champs, contraintes, 3, "STOCK", champStock);

        // Colonne vide en fin de ligne : les champs restent groupés à gauche.
        contraintes.gridx = 4;
        contraintes.weightx = 1;
        champs.add(UIFactory.panneauTransparent(new BorderLayout()), contraintes);

        return champs;
    }

    /** Place un libellé au-dessus de son champ, dans une colonne de la grille. */
    private void ajouterChamp(JPanel parent, GridBagConstraints contraintes,
                              int colonne, String libelle, JTextField champ) {
        JPanel bloc = UIFactory.panneauTransparent(new BorderLayout(0, 6));
        bloc.add(UIFactory.libelle(libelle), BorderLayout.NORTH);
        bloc.add(champ, BorderLayout.CENTER);

        contraintes.gridx = colonne;
        contraintes.gridy = 0;
        contraintes.weightx = 0;
        parent.add(bloc, contraintes);
    }

    private JPanel construireBoutons() {
        boutonAjouter = BoutonAction.principal("Ajouter", "add");
        boutonAjouter.addActionListener(e -> ajouter());

        boutonModifier = BoutonAction.info("Modifier", "edit");
        boutonModifier.addActionListener(e -> modifier());

        boutonSupprimer = BoutonAction.danger("Supprimer", "delete");
        boutonSupprimer.addActionListener(e -> supprimer());

        BoutonAction boutonVider = BoutonAction.secondaire("Vider", "clear");
        boutonVider.addActionListener(e -> viderFormulaire());

        JPanel barre = UIFactory.barreBoutons(boutonAjouter, boutonModifier, boutonSupprimer, boutonVider);
        barre.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        return barre;
    }

    private Carte construireTableau() {
        Carte carte = new Carte(new BorderLayout(0, 12));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        carte.add(construireBarreRecherche(), BorderLayout.NORTH);

        UIFactory.styliserTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) remplirFormulaireDepuisSelection();
        });

        carte.add(UIFactory.defilement(table), BorderLayout.CENTER);
        return carte;
    }

    private JPanel construireBarreRecherche() {
        BoutonAction boutonRechercher = BoutonAction.secondaire("Rechercher", "search");
        boutonRechercher.addActionListener(e -> rechercher());

        BoutonAction boutonTout = BoutonAction.secondaire("Tout afficher", "refresh");
        boutonTout.addActionListener(e -> {
            champRecherche.setText("");
            rafraichir();
        });

        // La touche Entrée déclenche la recherche.
        champRecherche.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) rechercher();
            }
        });

        JPanel gauche = UIFactory.panneauTransparent(new BorderLayout(10, 0));
        gauche.add(UIFactory.libelle("RECHERCHE PAR DÉSIGNATION"), BorderLayout.NORTH);

        JPanel saisie = UIFactory.panneauTransparent(new BorderLayout(8, 0));
        champRecherche.setPreferredSize(new Dimension(280, 34));
        saisie.add(champRecherche, BorderLayout.WEST);
        saisie.add(UIFactory.barreBoutons(boutonRechercher, boutonTout), BorderLayout.CENTER);
        gauche.add(saisie, BorderLayout.CENTER);

        return gauche;
    }

    // ------------------------------------------------------------------
    //  Actions
    // ------------------------------------------------------------------

    private void ajouter() {
        try {
            stockService.creerMedicament(
                    champNumero.getText(), champDesignation.getText(), champPrix.getText());
            Dialogues.succes(this, "Médicament ajouté avec succès.");
            viderFormulaire();
            rafraichir();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void modifier() {
        Medicament selection = medicamentSelectionne();
        if (selection == null) {
            Dialogues.avertissement(this, "Sélectionnez le médicament à modifier dans le tableau.");
            return;
        }

        try {
            stockService.modifierMedicament(
                    selection.getNumMedoc(), champDesignation.getText(), champPrix.getText());
            Dialogues.succes(this, "Médicament modifié avec succès.");
            rafraichir();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void supprimer() {
        Medicament selection = medicamentSelectionne();
        if (selection == null) {
            Dialogues.avertissement(this, "Sélectionnez le médicament à supprimer dans le tableau.");
            return;
        }

        boolean confirme = Dialogues.confirmer(this,
                "Voulez-vous vraiment supprimer le médicament « " + selection.getDesign() + " » ?",
                "Confirmer la suppression");
        if (!confirme) return;

        try {
            stockService.supprimerMedicament(selection.getNumMedoc());
            Dialogues.succes(this, "Médicament supprimé avec succès.");
            viderFormulaire();
            rafraichir();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void rechercher() {
        try {
            afficher(stockService.rechercherMedicaments(champRecherche.getText()));
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    /** Recharge la liste complète, en conservant le filtre de recherche saisi. */
    public void rafraichir() {
        try {
            afficher(stockService.rechercherMedicaments(champRecherche.getText()));
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  Tableau et formulaire
    // ------------------------------------------------------------------

    private void afficher(List<Medicament> medicaments) {
        medicamentsAffiches = medicaments;
        modele.setRowCount(0);

        for (Medicament medicament : medicaments) {
            modele.addRow(new Object[]{
                    medicament.getNumMedoc(),
                    medicament.getDesign(),
                    MontantUtil.formatAvecDevise(medicament.getPrixUnitaire()),
                    medicament.getStock(),
                    medicament.getEtatStock()});
        }

        UIFactory.largeurColonne(table, 0, 80);
        UIFactory.largeurColonne(table, 1, 300);
        UIFactory.alignerADroite(table, 2);
        UIFactory.alignerAuCentre(table, 3);
        UIFactory.largeurColonne(table, 3, 70);
        UIFactory.largeurColonne(table, 4, 110);
        table.getColumnModel().getColumn(4).setCellRenderer(new UIFactory.RenduEtatStock());
    }

    /** Retourne le médicament sélectionné, ou {@code null} si aucune ligne n'est sélectionnée. */
    private Medicament medicamentSelectionne() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) return null;
        int index = table.convertRowIndexToModel(ligne);
        if (index < 0 || index >= medicamentsAffiches.size()) return null;
        return medicamentsAffiches.get(index);
    }

    /** Recopie la ligne sélectionnée dans le formulaire. */
    private void remplirFormulaireDepuisSelection() {
        Medicament medicament = medicamentSelectionne();
        if (medicament == null) return;

        champNumero.setText(medicament.getNumMedoc());
        champDesignation.setText(medicament.getDesign());
        champPrix.setText(String.valueOf(medicament.getPrixUnitaire()));
        champStock.setText(String.valueOf(medicament.getStock()));

        // Le numéro identifie le médicament : il n'est pas modifiable après création.
        champNumero.setEditable(false);
        champNumero.setBackground(Theme.FOND);
        boutonAjouter.setEnabled(false);
        boutonModifier.setEnabled(true);
        boutonSupprimer.setEnabled(true);
    }

    /** Réinitialise le formulaire pour la saisie d'un nouveau médicament. */
    private void viderFormulaire() {
        table.clearSelection();

        champNumero.setText("");
        champDesignation.setText("");
        champPrix.setText("");
        champStock.setText("0");

        champNumero.setEditable(true);
        champNumero.setBackground(Theme.BLANC);
        boutonAjouter.setEnabled(true);
        boutonModifier.setEnabled(false);
        boutonSupprimer.setEnabled(false);

        champNumero.requestFocusInWindow();
    }
}
