package pharmacie.ui;

import pharmacie.model.Achat;
import pharmacie.service.AchatService;
import pharmacie.service.FacturePdfService;
import pharmacie.ui.components.BoutonAction;
import pharmacie.ui.components.Carte;
import pharmacie.ui.components.Dialogues;
import pharmacie.ui.components.UIFactory;
import pharmacie.util.BusinessException;
import pharmacie.util.DateUtil;
import pharmacie.util.MontantUtil;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * ACHAT → HISTORIQUE : consultation, modification et suppression des ventes.
 *
 * Chaque ligne correspond à un médicament acheté. Modifier ou supprimer une
 * ligne ajuste automatiquement le stock du médicament concerné.
 */
public class HistoriqueAchatPanel extends JPanel {

    private final AchatService achatService = new AchatService();
    private final FacturePdfService facturePdfService = new FacturePdfService();

    private final JTextField champRecherche = UIFactory.champTexte(22);
    private final JTextField champClient = UIFactory.champTexte(22);
    private final JTextField champQuantite = UIFactory.champTexte(8);
    private final JTextField champDate = UIFactory.champTexte(12);

    private final DefaultTableModel modele = new DefaultTableModel(
            new String[]{"N° Achat", "Client", "Médicament", "Prix unitaire", "Quantité", "Total", "Date"}, 0) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };

    private final JTable table = new JTable(modele);

    private List<Achat> achatsAffiches = List.of();

    private BoutonAction boutonModifier;
    private BoutonAction boutonSupprimer;
    private BoutonAction boutonFacture;

    public HistoriqueAchatPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        add(construireFormulaire(), BorderLayout.NORTH);
        add(construireTableau(), BorderLayout.CENTER);

        activerBoutons(false);
    }

    // ------------------------------------------------------------------
    //  Construction
    // ------------------------------------------------------------------

    private Carte construireFormulaire() {
        Carte carte = new Carte(new BorderLayout(0, 14));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        JPanel entete = UIFactory.panneauTransparent(new BorderLayout(0, 3));
        entete.add(UIFactory.sousTitre("Ligne d'achat sélectionnée"), BorderLayout.NORTH);
        entete.add(UIFactory.aide("Modifier la quantité ajuste automatiquement le stock du médicament"),
                BorderLayout.CENTER);
        carte.add(entete, BorderLayout.NORTH);

        JPanel champs = UIFactory.panneauTransparent(new GridBagLayout());
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.anchor = GridBagConstraints.WEST;
        contraintes.insets = new Insets(0, 0, 0, 18);

        ajouterBloc(champs, contraintes, 0, "CLIENT *", champClient);
        ajouterBloc(champs, contraintes, 1, "QUANTITÉ *", champQuantite);
        ajouterBloc(champs, contraintes, 2, "DATE * (jj/mm/aaaa)", champDate);

        contraintes.gridx = 3;
        contraintes.weightx = 1;
        champs.add(UIFactory.panneauTransparent(new BorderLayout()), contraintes);

        carte.add(champs, BorderLayout.CENTER);
        carte.add(construireBoutons(), BorderLayout.SOUTH);
        return carte;
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
        boutonModifier = BoutonAction.info("Modifier", "edit");
        boutonModifier.addActionListener(e -> modifier());

        boutonSupprimer = BoutonAction.danger("Supprimer", "delete");
        boutonSupprimer.addActionListener(e -> supprimer());

        boutonFacture = BoutonAction.secondaire("Facture PDF", "pdf");
        boutonFacture.addActionListener(e -> genererFacture());

        JPanel barre = UIFactory.barreBoutons(boutonModifier, boutonSupprimer, boutonFacture);
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

        champRecherche.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) rechercher();
            }
        });

        JPanel barre = UIFactory.panneauTransparent(new BorderLayout(10, 0));
        barre.add(UIFactory.libelle("RECHERCHE PAR CLIENT"), BorderLayout.NORTH);

        JPanel saisie = UIFactory.panneauTransparent(new BorderLayout(8, 0));
        champRecherche.setPreferredSize(new Dimension(280, 34));
        saisie.add(champRecherche, BorderLayout.WEST);
        saisie.add(UIFactory.barreBoutons(boutonRechercher, boutonTout), BorderLayout.CENTER);
        barre.add(saisie, BorderLayout.CENTER);

        return barre;
    }

    // ------------------------------------------------------------------
    //  Actions
    // ------------------------------------------------------------------

    private void modifier() {
        Achat selection = achatSelectionne();
        if (selection == null) {
            Dialogues.avertissement(this, "Sélectionnez la ligne à modifier dans le tableau.");
            return;
        }

        try {
            achatService.modifierLigneAchat(selection.getNumAchat(), selection.getNumMedoc(),
                    champClient.getText(), champQuantite.getText(), DateUtil.parse(champDate.getText()));
            Dialogues.succes(this, "Achat modifié avec succès.\nLe stock a été ajusté.");
            rafraichir();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void supprimer() {
        Achat selection = achatSelectionne();
        if (selection == null) {
            Dialogues.avertissement(this, "Sélectionnez la ligne à supprimer dans le tableau.");
            return;
        }

        boolean confirme = Dialogues.confirmer(this,
                "Voulez-vous vraiment supprimer cette ligne d'achat ?\n"
                        + selection.getDesignMedicament() + " x " + selection.getNbr() + "\n\n"
                        + "La quantité sera restituée au stock.",
                "Confirmer la suppression");
        if (!confirme) return;

        try {
            achatService.supprimerLigneAchat(selection.getNumAchat(), selection.getNumMedoc());
            Dialogues.succes(this, "Ligne d'achat supprimée avec succès.");
            viderFormulaire();
            rafraichir();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    /** Génère la facture PDF de l'achat auquel appartient la ligne sélectionnée. */
    private void genererFacture() {
        Achat selection = achatSelectionne();
        if (selection == null) {
            Dialogues.avertissement(this, "Sélectionnez un achat dans le tableau.");
            return;
        }

        try {
            var facture = achatService.chargerFacture(selection.getNumAchat());

            JFileChooser selecteur = new JFileChooser();
            selecteur.setDialogTitle("Enregistrer la facture PDF");
            selecteur.setSelectedFile(new File(facturePdfService.nomFichierPropose(facture)));
            selecteur.setFileFilter(new FileNameExtensionFilter("Document PDF (*.pdf)", "pdf"));

            if (selecteur.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

            File fichier = selecteur.getSelectedFile();
            if (!fichier.getName().toLowerCase().endsWith(".pdf")) {
                fichier = new File(fichier.getParentFile(), fichier.getName() + ".pdf");
            }

            facturePdfService.genererPdf(facture, fichier);
            proposerOuverture(fichier);
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private void proposerOuverture(File fichier) {
        boolean ouvrable = Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);

        if (!ouvrable) {
            Dialogues.succes(this, "Facture enregistrée :\n" + fichier.getAbsolutePath());
            return;
        }

        boolean ouvrir = Dialogues.proposer(this,
                "Facture enregistrée :\n" + fichier.getAbsolutePath() + "\n\nVoulez-vous l'ouvrir ?",
                "Facture générée", "pdf");

        if (ouvrir) {
            try {
                Desktop.getDesktop().open(fichier);
            } catch (Exception e) {
                Dialogues.avertissement(this,
                        "La facture a été enregistrée mais n'a pas pu être ouverte automatiquement.\n"
                                + fichier.getAbsolutePath());
            }
        }
    }

    private void rechercher() {
        try {
            afficher(achatService.rechercherParClient(champRecherche.getText()));
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    /** Recharge l'historique en conservant le filtre de recherche saisi. */
    public void rafraichir() {
        try {
            afficher(achatService.rechercherParClient(champRecherche.getText()));
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  Tableau et formulaire
    // ------------------------------------------------------------------

    private void afficher(List<Achat> achats) {
        achatsAffiches = achats;
        modele.setRowCount(0);

        for (Achat achat : achats) {
            modele.addRow(new Object[]{
                    achat.getNumAchat(),
                    achat.getNomClient(),
                    achat.getDesignMedicament(),
                    MontantUtil.formatAvecDevise(achat.getPrixUnitaire()),
                    achat.getNbr(),
                    MontantUtil.formatAvecDevise(achat.getTotalLigne()),
                    DateUtil.format(achat.getDateAchat())});
        }

        UIFactory.largeurColonne(table, 0, 90);
        UIFactory.largeurColonne(table, 1, 170);
        UIFactory.largeurColonne(table, 2, 220);
        UIFactory.alignerADroite(table, 3);
        UIFactory.alignerAuCentre(table, 4);
        UIFactory.largeurColonne(table, 4, 80);
        UIFactory.alignerADroite(table, 5);
        UIFactory.alignerAuCentre(table, 6);
        UIFactory.largeurColonne(table, 6, 110);
    }

    private Achat achatSelectionne() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) return null;
        int index = table.convertRowIndexToModel(ligne);
        if (index < 0 || index >= achatsAffiches.size()) return null;
        return achatsAffiches.get(index);
    }

    private void remplirFormulaireDepuisSelection() {
        Achat achat = achatSelectionne();
        if (achat == null) {
            activerBoutons(false);
            return;
        }

        champClient.setText(achat.getNomClient());
        champQuantite.setText(String.valueOf(achat.getNbr()));
        champDate.setText(DateUtil.format(achat.getDateAchat()));
        activerBoutons(true);
    }

    private void viderFormulaire() {
        table.clearSelection();
        champClient.setText("");
        champQuantite.setText("");
        champDate.setText(DateUtil.format(LocalDate.now()));
        activerBoutons(false);
    }

    private void activerBoutons(boolean actif) {
        boutonModifier.setEnabled(actif);
        boutonSupprimer.setEnabled(actif);
        boutonFacture.setEnabled(actif);
    }
}
