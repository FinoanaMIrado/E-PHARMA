package pharmacie.ui;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import pharmacie.model.Facture;
import pharmacie.model.LigneVente;
import pharmacie.service.AchatService;
import pharmacie.service.FacturePdfService;
import pharmacie.service.StockService;
import pharmacie.model.Medicament;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Verifie que la facture PDF est generee et contient bien toutes les
 * informations exigees par le cahier des charges (section 22).
 */
public class TestFacturePdf {

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        AchatService achatService = new AchatService();
        StockService stockService = new StockService();
        FacturePdfService pdfService = new FacturePdfService();

        List<Medicament> dispo = stockService.listerMedicaments().stream()
                .filter(m -> m.getStock() >= 2).limit(3).toList();
        if (dispo.size() < 3) {
            throw new IllegalStateException("Pas assez de stock pour le test");
        }

        String numAchat = achatService.prochainNumeroAchat();
        String client = "ANDRIAMANANA Soa";
        achatService.validerVente(numAchat, client, LocalDate.now(), List.of(
                new LigneVente(dispo.get(0).getNumMedoc(), dispo.get(0).getDesign(), dispo.get(0).getPrixUnitaire(), 2),
                new LigneVente(dispo.get(1).getNumMedoc(), dispo.get(1).getDesign(), dispo.get(1).getPrixUnitaire(), 1),
                new LigneVente(dispo.get(2).getNumMedoc(), dispo.get(2).getDesign(), dispo.get(2).getPrixUnitaire(), 2)));

        Facture facture = achatService.chargerFacture(numAchat);
        File sortie = new File("/tmp/facture_test.pdf");
        pdfService.genererPdf(facture, sortie);

        if (!sortie.exists() || sortie.length() == 0) {
            throw new AssertionError("Le fichier PDF n'a pas ete cree");
        }
        System.out.println("PDF genere : " + sortie.getAbsolutePath()
                + " (" + sortie.length() + " octets)");

        try (PDDocument doc = PDDocument.load(sortie)) {
            String texte = new PDFTextStripper().getText(doc);
            System.out.println("Pages : " + doc.getNumberOfPages());
            System.out.println("--------- CONTENU EXTRAIT ---------");
            System.out.println(texte.trim());
            System.out.println("-----------------------------------");

            verifier(texte.contains(client), "nom du client");
            verifier(texte.contains(numAchat), "numero d'achat");
            verifier(texte.contains(dispo.get(0).getDesign()), "designation du medicament");
            verifier(texte.contains(String.valueOf(LocalDate.now().getYear())), "date");
            verifier(texte.toLowerCase().contains("total"), "total general");
            verifier(texte.contains("Ar"), "devise");
        }

        // Nettoyage des lignes d'achat de test
        for (Medicament m : dispo) {
            achatService.supprimerLigneAchat(numAchat, m.getNumMedoc());
        }
        System.out.println("=== FACTURE PDF CONFORME ===");
        System.exit(0);
    }

    private static void verifier(boolean ok, String champ) {
        if (!ok) {
            throw new AssertionError("Champ absent de la facture : " + champ);
        }
        System.out.println("OK  facture contient : " + champ);
    }
}
