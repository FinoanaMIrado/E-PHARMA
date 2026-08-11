package pharmacie.ui;

import pharmacie.model.Facture;
import pharmacie.model.LigneVente;
import pharmacie.model.Medicament;
import pharmacie.service.AchatService;
import pharmacie.service.FacturePdfService;
import pharmacie.service.StockService;
import pharmacie.ui.components.BoutonAction;
import pharmacie.ui.components.Carte;
import pharmacie.ui.components.Dialogues;
import pharmacie.ui.components.UIFactory;
import pharmacie.util.BusinessException;
import pharmacie.util.DateUtil;
import pharmacie.util.MontantUtil;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ACHAT → NOUVELLE VENTE.
 *
 * Permet de constituer un panier de plusieurs médicaments puis de valider la
 * vente. Le stock est contrôlé à l'ajout au panier puis revérifié au moment de
 * la validation, et la facture PDF peut être générée dans la foulée.
 */
public class NouvelleVentePanel extends JPanel {

    private final AchatService achatService = new AchatService();
    private final StockService stockService = new StockService();
    private final FacturePdfService facturePdfService = new FacturePdfService();

    /** Action exécutée après la validation d'une vente. */
    private final Runnable apresVente;

    private final JTextField champNumAchat = UIFactory.champLectureSeule(12);
    private final JTextField champClient = UIFactory.champTexte(22);
    private final JTextField champDate = UIFactory.champTexte(12);

    private final JComboBox<Medicament> listeMedicaments = new JComboBox<>();
    private final JTextField champQuantite = UIFactory.champTexte(8);
    private final JLabel labelStockDisponible = UIFactory.aide(" ");

    private final DefaultTableModel modelePanier = new DefaultTableModel(
            new String[]{"Médicament", "Prix unitaire", "Quantité", "Total"}, 0) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };

    private final JTable tablePanier = new JTable(modelePanier);
    private final JLabel labelTotal = UIFactory.valeurMiseEnAvant("0 Ar", Theme.VERT, 24);

    /** Lignes du panier en cours de saisie, non encore enregistrées. */
    private final List<LigneVente> panier = new ArrayList<>();

    /** Dernière facture validée, proposée à l'export PDF. */
    private Facture derniereFacture;

    private BoutonAction boutonFacture;

    public NouvelleVentePanel(Runnable apresVente) {
        this.apresVente = apresVente;

        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        add(construireHaut(), BorderLayout.NORTH);
        add(construirePanier(), BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    //  Construction
    // ------------------------------------------------------------------

    private JPanel construireHaut() {
        JPanel haut = UIFactory.panneauTransparent(new BorderLayout(0, 14));
        haut.add(construireCarteClient(), BorderLayout.NORTH);
        haut.add(construireCarteSelection(), BorderLayout.CENTER);
        return haut;
    }

    /** Carte d'en-tête : numéro d'achat, client et date. */
    private Carte construireCarteClient() {
        Carte carte = new Carte(new BorderLayout(0, 14));
        carte.setBorder(Carte.marge(16, 16, 16, 16));
        carte.add(UIFactory.sousTitre("Informations de la vente"), BorderLayout.NORTH);

        JPanel champs = UIFactory.panneauTransparent(new GridBagLayout());
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.anchor = GridBagConstraints.WEST;
        contraintes.insets = new Insets(0, 0, 0, 18);

        ajouterBloc(champs, contraintes, 0, "N° ACHAT", champNumAchat);
        ajouterBloc(champs, contraintes, 1, "NOM DU CLIENT *", champClient);
        ajouterBloc(champs, contraintes, 2, "DATE * (jj/mm/aaaa)", champDate);

        contraintes.gridx = 3;
        contraintes.weightx = 1;
        champs.add(UIFactory.panneauTransparent(new BorderLayout()), contraintes);

        carte.add(champs, BorderLayout.CENTER);
        return carte;
    }

    /** Carte de sélection : médicament, quantité et ajout au panier. */
    private Carte construireCarteSelection() {
        Carte carte = new Carte(new BorderLayout(0, 14));
        carte.setBorder(Carte.marge(16, 16, 16, 16));
        carte.add(UIFactory.sousTitre("Ajouter un médicament au panier"), BorderLayout.NORTH);

        UIFactory.styliserChamp(listeMedicaments);
        listeMedicaments.setPreferredSize(new Dimension(300, 34));
        listeMedicaments.setBackground(Theme.BLANC);
        listeMedicaments.addActionListener(e -> afficherStockDisponible());

        JPanel champs = UIFactory.panneauTransparent(new GridBagLayout());
        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.anchor = GridBagConstraints.WEST;
        contraintes.insets = new Insets(0, 0, 0, 18);

        ajouterBloc(champs, contraintes, 0, "MÉDICAMENT *", listeMedicaments);
        ajouterBloc(champs, contraintes, 1, "QUANTITÉ *", champQuantite);

        BoutonAction boutonAjouter = BoutonAction.principal("Ajouter au panier", "add");
        boutonAjouter.addActionListener(e -> ajouterAuPanier());

        JPanel blocBouton = UIFactory.panneauTransparent(new BorderLayout(0, 6));
        blocBouton.add(UIFactory.libelle(" "), BorderLayout.NORTH);
        blocBouton.add(boutonAjouter, BorderLayout.CENTER);
        contraintes.gridx = 2;
        contraintes.gridy = 0;
        contraintes.weightx = 0;
        champs.add(blocBouton, contraintes);

        JPanel blocStock = UIFactory.panneauTransparent(new BorderLayout(0, 6));
        blocStock.add(UIFactory.libelle(" "), BorderLayout.NORTH);
        blocStock.add(labelStockDisponible, BorderLayout.CENTER);
        contraintes.gridx = 3;
        contraintes.weightx = 1;
        champs.add(blocStock, contraintes);

        carte.add(champs, BorderLayout.CENTER);
        return carte;
    }

    private void ajouterBloc(JPanel parent, GridBagConstraints contraintes,
                             int colonne, String libelle, Component champ) {
        JPanel bloc = UIFactory.panneauTransparent(new BorderLayout(0, 6));
        bloc.add(UIFactory.libelle(libelle), BorderLayout.NORTH);
        bloc.add(champ, BorderLayout.CENTER);

        contraintes.gridx = colonne;
        contraintes.gridy = 0;
        contraintes.weightx = 0;
        parent.add(bloc, contraintes);
    }

    /** Carte du panier : tableau des lignes, total et actions de validation. */
    private Carte construirePanier() {
        Carte carte = new Carte(new BorderLayout(0, 12));
        carte.setBorder(Carte.marge(16, 16, 16, 16));

        JPanel entete = UIFactory.panneauTransparent(new BorderLayout(0, 3));
        entete.add(UIFactory.sousTitre("Panier"), BorderLayout.NORTH);
        entete.add(UIFactory.aide("Le stock est vérifié avant chaque ajout et à la validation"),
                BorderLayout.CENTER);
        carte.add(entete, BorderLayout.NORTH);

        UIFactory.styliserTable(tablePanier);
        tablePanier.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        carte.add(UIFactory.defilement(tablePanier), BorderLayout.CENTER);

        carte.add(construireBasPanier(), BorderLayout.SOUTH);
        return carte;
    }

    private JPanel construireBasPanier() {
        BoutonAction boutonSupprimerLigne = BoutonAction.secondaire("Supprimer la ligne", "delete");
        boutonSupprimerLigne.addActionListener(e -> supprimerLigneSelectionnee());

        BoutonAction boutonViderPanier = BoutonAction.secondaire("Vider le panier", "clear");
        boutonViderPanier.addActionListener(e -> viderPanier());

        BoutonAction boutonValider = BoutonAction.principal("Valider l'achat", "check");
        boutonValider.addActionListener(e -> validerAchat());

        boutonFacture = BoutonAction.info("Générer facture PDF", "pdf");
        boutonFacture.addActionListener(e -> genererFacture());
        boutonFacture.setEnabled(false);

        JPanel gauche = UIFactory.barreBoutons(boutonSupprimerLigne, boutonViderPanier);

        JPanel total = UIFactory.panneauTransparent(new BorderLayout(12, 0));
        JLabel libelleTotal = UIFactory.libelle("TOTAL GÉNÉRAL");
        total.add(libelleTotal, BorderLayout.NORTH);
        total.add(labelTotal, BorderLayout.CENTER);

        JPanel droite = UIFactory.panneauTransparent(new BorderLayout(18, 0));
        droite.add(total, BorderLayout.WEST);
        droite.add(UIFactory.barreBoutonsDroite(boutonValider, boutonFacture), BorderLayout.EAST);

        JPanel bas = UIFactory.panneauTransparent(new BorderLayout(12, 0));
        bas.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bas.add(gauche, BorderLayout.WEST);
        bas.add(droite, BorderLayout.EAST);
        return bas;
    }

    // ------------------------------------------------------------------
    //  Panier
    // ------------------------------------------------------------------

    /** Ajoute le médicament sélectionné au panier après contrôle du stock. */
    private void ajouterAuPanier() {
        Medicament medicament = (Medicament) listeMedicaments.getSelectedItem();
        if (medicament == null) {
            Dialogues.avertissement(this, "Sélectionnez un médicament.");
            return;
        }

        int quantite;
        try {
            quantite = Integer.parseInt(champQuantite.getText().trim());
        } catch (NumberFormatException e) {
            Dialogues.avertissement(this, "La quantité doit être un nombre entier.");
            return;
        }
        if (quantite <= 0) {
            Dialogues.avertissement(this, "La quantité doit être strictement positive.");
            return;
        }

        try {
            LigneVente existante = ligneDuPanier(medicament.getNumMedoc());
            int dejaAuPanier = existante == null ? 0 : existante.getQuantite();

            // Contrôle du stock avant l'ajout : l'utilisateur est prévenu immédiatement.
            achatService.verifierDisponibilite(medicament, quantite, dejaAuPanier);

            if (existante != null) {
                existante.ajouterQuantite(quantite);
            } else {
                panier.add(new LigneVente(medicament.getNumMedoc(), medicament.getDesign(),
                        medicament.getPrixUnitaire(), quantite));
            }

            champQuantite.setText("");
            afficherPanier();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    private LigneVente ligneDuPanier(String numMedoc) {
        return panier.stream()
                .filter(ligne -> ligne.getNumMedoc().equals(numMedoc))
                .findFirst()
                .orElse(null);
    }

    private void supprimerLigneSelectionnee() {
        int ligne = tablePanier.getSelectedRow();
        if (ligne < 0) {
            Dialogues.avertissement(this, "Sélectionnez la ligne à supprimer dans le panier.");
            return;
        }
        panier.remove(tablePanier.convertRowIndexToModel(ligne));
        afficherPanier();
    }

    private void viderPanier() {
        if (panier.isEmpty()) return;
        if (!Dialogues.confirmer(this, "Voulez-vous vraiment vider le panier ?", "Vider le panier")) {
            return;
        }
        panier.clear();
        afficherPanier();
    }

    private void afficherPanier() {
        modelePanier.setRowCount(0);
        for (LigneVente ligne : panier) {
            modelePanier.addRow(new Object[]{
                    ligne.getDesign(),
                    MontantUtil.formatAvecDevise(ligne.getPrixUnitaire()),
                    ligne.getQuantite(),
                    MontantUtil.formatAvecDevise(ligne.getTotal())});
        }

        UIFactory.largeurColonne(tablePanier, 0, 320);
        UIFactory.alignerADroite(tablePanier, 1);
        UIFactory.alignerAuCentre(tablePanier, 2);
        UIFactory.largeurColonne(tablePanier, 2, 90);
        UIFactory.alignerADroite(tablePanier, 3);

        long total = panier.stream().mapToLong(LigneVente::getTotal).sum();
        labelTotal.setText(MontantUtil.formatAvecDevise(total));
    }

    // ------------------------------------------------------------------
    //  Validation et facture
    // ------------------------------------------------------------------

    /** Valide la vente : enregistre l'achat et décrémente les stocks. */
    private void validerAchat() {
        if (panier.isEmpty()) {
            Dialogues.avertissement(this, "Le panier est vide : ajoutez au moins un médicament.");
            return;
        }

        try {
            Facture facture = achatService.validerVente(
                    champNumAchat.getText(), champClient.getText(),
                    DateUtil.parse(champDate.getText()), panier);

            derniereFacture = facture;
            boutonFacture.setEnabled(true);

            panier.clear();
            afficherPanier();
            rafraichir();
            apresVente.run();

            boolean genererMaintenant = Dialogues.proposer(this,
                    "Achat enregistré avec succès.\n"
                            + "Total : " + MontantUtil.formatAvecDevise(facture.getTotalGeneral()) + "\n\n"
                            + "Voulez-vous générer la facture PDF maintenant ?",
                    "Achat validé", "pdf");

            if (genererMaintenant) {
                genererFacture();
            }
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    /** Enregistre la facture de la dernière vente au format PDF. */
    private void genererFacture() {
        if (derniereFacture == null) {
            Dialogues.avertissement(this, "Validez d'abord un achat pour générer sa facture.");
            return;
        }

        JFileChooser selecteur = new JFileChooser();
        selecteur.setDialogTitle("Enregistrer la facture PDF");
        selecteur.setSelectedFile(new File(facturePdfService.nomFichierPropose(derniereFacture)));
        selecteur.setFileFilter(new FileNameExtensionFilter("Document PDF (*.pdf)", "pdf"));

        if (selecteur.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fichier = selecteur.getSelectedFile();
        if (!fichier.getName().toLowerCase().endsWith(".pdf")) {
            fichier = new File(fichier.getParentFile(), fichier.getName() + ".pdf");
        }

        try {
            facturePdfService.genererPdf(derniereFacture, fichier);
            proposerOuverture(fichier);
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    /** Propose d'ouvrir la facture générée, si le système le permet. */
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

    // ------------------------------------------------------------------
    //  Chargement
    // ------------------------------------------------------------------

    /** Recharge la liste des médicaments et prépare le formulaire de vente. */
    public void rafraichir() {
        try {
            Medicament selectionPrecedente = (Medicament) listeMedicaments.getSelectedItem();
            List<Medicament> medicaments = stockService.listerMedicaments();
            listeMedicaments.setModel(new DefaultComboBoxModel<>(medicaments.toArray(new Medicament[0])));

            if (selectionPrecedente != null) {
                for (int i = 0; i < listeMedicaments.getItemCount(); i++) {
                    if (listeMedicaments.getItemAt(i).getNumMedoc().equals(selectionPrecedente.getNumMedoc())) {
                        listeMedicaments.setSelectedIndex(i);
                        break;
                    }
                }
            }

            champNumAchat.setText(achatService.prochainNumeroAchat());
            if (champDate.getText().isBlank()) {
                champDate.setText(DateUtil.format(LocalDate.now()));
            }
            afficherStockDisponible();
        } catch (BusinessException e) {
            Dialogues.erreur(this, e.getMessage());
        }
    }

    /** Affiche le stock disponible du médicament sélectionné, en tenant compte du panier. */
    private void afficherStockDisponible() {
        Medicament medicament = (Medicament) listeMedicaments.getSelectedItem();
        if (medicament == null) {
            labelStockDisponible.setText(" ");
            return;
        }

        LigneVente ligne = ligneDuPanier(medicament.getNumMedoc());
        int restant = medicament.getStock() - (ligne == null ? 0 : ligne.getQuantite());

        labelStockDisponible.setText("Stock disponible : " + restant);
        labelStockDisponible.setForeground(restant < 5 ? Theme.ORANGE : Theme.TEXTE_DOUX);
    }
}
