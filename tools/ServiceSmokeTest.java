package pharmacie.service;

import pharmacie.model.LigneVente;
import pharmacie.util.BusinessException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Test d'intégration des services métier, exécuté sans interface graphique.
 *
 * Utilise le connecteur JDBC : lancez-le avec le répertoire des classes et la
 * classe du connecteur en classpath. Il vérifie les règles du cahier des charges
 * directement contre une base MySQL accessible.
 */
public class ServiceSmokeTest {

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        StockService stockService = new StockService();
        AchatService achatService = new AchatService();
        BilanService bilanService = new BilanService();

        // 1. La liste des médicaments est lisible
        List<?> medicaments = stockService.listerMedicaments();
        System.out.println("OK  medicaments listes : " + medicaments.size());
        if (medicaments.isEmpty()) throw new AssertionError("Aucun medicament en base");

        // 2. Le stock est coherent : jamais negatif, et au moins un stock faible (< 5)
        boolean auMoinsUnStockFaible = false;
        for (Object objet : medicaments) {
            pharmacie.model.Medicament m = (pharmacie.model.Medicament) objet;
            if (m.getStock() < 0) throw new AssertionError("Stock negatif pour " + m.getNumMedoc());
            if (m.getStock() < 5) auMoinsUnStockFaible = true;
        }
        if (!auMoinsUnStockFaible) throw new AssertionError("Aucun stock faible (< 5) dans les donnees de test");
        System.out.println("OK  stocks coherents (aucun negatif, stocks faibles presents)");

        // 3. Recherche partielle sur la designation
        List<?> recherche = stockService.rechercherMedicaments("para");
        System.out.println("OK  recherche 'para' -> " + recherche.size() + " resultat(s)");
        if (recherche.isEmpty()) throw new AssertionError("Recherche 'para' sans resultat");

        // 4. Bilan : recette totale, top 5, recettes mensuelles
        long recette = bilanService.recetteTotale();
        System.out.println("OK  recette totale : " + recette);
        if (recette <= 0) throw new AssertionError("Recette totale nulle");

        List<?> top = bilanService.topVentes();
        if (top.size() > 5) throw new AssertionError("Top ventes > 5 lignes");
        System.out.println("OK  top ventes : " + top.size() + " ligne(s)");

        List<?> mensuel = bilanService.recettesDerniersMois();
        if (mensuel.size() != 5) throw new AssertionError("Histogramme != 5 mois");
        System.out.println("OK  recettes mensuelles : 5 mois");

        // 5. Validation d'une vente : stock deja ajoute + stock decremente
        pharmacie.model.Medicament cible =
                (pharmacie.model.Medicament) medicaments.stream()
                        .filter(obj -> ((pharmacie.model.Medicament) obj).getStock() >= 10)
                        .findFirst().orElseThrow(() -> new AssertionError("Pas de medicament stock>=10"));
        int stockAvant = cible.getStock();
        String num = achatService.prochainNumeroAchat();
        System.out.println("OK  prochain numero achat : " + num);

        achatService.verifierDisponibilite(cible, 2, 0);
        achatService.validerVente(num, "TEST INTEGRATION", LocalDate.now(),
                List.of(new LigneVente(cible.getNumMedoc(), cible.getDesign(),
                        cible.getPrixUnitaire(), 2)));

        int stockApres = stockService.trouverMedicament(cible.getNumMedoc()).getStock();
        if (stockApres != stockAvant - 2) {
            throw new AssertionError("Stock apres vente = " + stockApres + ", attendu " + (stockAvant - 2));
        }
        System.out.println("OK  vente de 2 : stock " + stockAvant + " -> " + stockApres);

        // 6. Stock insuffisant : la vente est bloquee
        pharmacie.model.Medicament faible =
                (pharmacie.model.Medicament) medicaments.stream()
                        .filter(obj -> ((pharmacie.model.Medicament) obj).getStock() < 5)
                        .findFirst().orElseThrow();
        try {
            achatService.validerVente(achatService.prochainNumeroAchat(), "TEST", LocalDate.now(),
                    List.of(new LigneVente(faible.getNumMedoc(), faible.getDesign(),
                            faible.getPrixUnitaire(), 99)));
            throw new AssertionError("Vente avec stock insuffisant acceptee a tort");
        } catch (BusinessException attendue) {
            System.out.println("OK  stock insuffisant bloque : " + attendue.getMessage().split("\n")[0]);
        }

        // 7. Facture PDF
        File pdf = File.createTempFile("test_facture", ".pdf");
        pdf.deleteOnExit();
        achatService.chargerFacture(num);
        new FacturePdfService().genererPdf(achatService.chargerFacture(num), pdf);
        if (pdf.length() < 500) throw new AssertionError("PDF trop petit : " + pdf.length());
        System.out.println("OK  facture PDF generee : " + pdf.getAbsolutePath() + " (" + pdf.length() + " octets)");

        System.out.println();
        System.out.println("=== TOUS LES TESTS D'INTEGRATION REUSSIS ===");
        System.out.println("(la base de test contient desormais la vente '" + num + "' de TEST INTEGRATION)");
    }
}
