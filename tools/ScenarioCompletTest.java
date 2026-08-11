package pharmacie.ui;

import pharmacie.model.LigneVente;
import pharmacie.model.Medicament;
import pharmacie.model.Utilisateur;
import pharmacie.service.AchatService;
import pharmacie.service.AuthService;
import pharmacie.service.StockService;
import pharmacie.util.BusinessException;

import java.time.LocalDate;
import java.util.List;

/**
 * Scenario fonctionnel complet du cahier des charges (section 27).
 *
 * Rejoue les etapes d'utilisation reelles : connexion, creation d'un medicament
 * a stock 0, entree de stock, vente, controle du stock insuffisant, puis
 * verification des indicateurs. Chaque etape est verifiee par assertion.
 */
public class ScenarioCompletTest {

    private static int etape = 0;

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        AuthService authService = new AuthService();
        StockService stockService = new StockService();
        AchatService achatService = new AchatService();

        String suffixe = String.valueOf(System.currentTimeMillis() % 100000);
        String numMedoc = "T" + suffixe;
        String numEntree = "TE" + suffixe;

        // Etape 1 : connexion avec le compte de demonstration
        Utilisateur utilisateur = authService.authentifier("admin", "admin123".toCharArray());
        verifier(utilisateur != null && "admin".equals(utilisateur.getLogin()),
                "Connexion admin/admin123 reussie (" + utilisateur.getNomComplet() + ")");

        // Etape 1b : un mauvais mot de passe est refuse
        try {
            authService.authentifier("admin", "mauvais".toCharArray());
            throw new AssertionError("Un mot de passe incorrect a ete accepte");
        } catch (BusinessException e) {
            verifier(true, "Mot de passe incorrect refuse : " + e.getMessage());
        }

        // Etape 3 : creation d'un medicament -> stock force a 0
        stockService.creerMedicament(numMedoc, "Test Scenario " + suffixe, "1500");
        Medicament cree = stockService.trouverMedicament(numMedoc);
        verifier(cree != null && cree.getStock() == 0,
                "Medicament cree avec stock = 0 (regle 19.1)");

        // Doublon d'identifiant refuse
        try {
            stockService.creerMedicament(numMedoc, "Doublon", "500");
            throw new AssertionError("Doublon de numMedoc accepte");
        } catch (BusinessException e) {
            verifier(true, "Numero de medicament en doublon refuse");
        }

        // Prix invalide refuse
        try {
            stockService.creerMedicament("X" + suffixe, "Prix invalide", "abc");
            throw new AssertionError("Prix non numerique accepte");
        } catch (BusinessException e) {
            verifier(true, "Prix non numerique refuse : " + e.getMessage());
        }

        // Etape 4 : entree de stock -> stock = 0 + 20
        stockService.creerEntree(numEntree, numMedoc, "20", LocalDate.now());
        verifier(stockService.trouverMedicament(numMedoc).getStock() == 20,
                "Entree de 20 : stock 0 -> 20 (regle 19.2)");

        // Etape 5 : vente de 2 -> stock = 20 - 2
        String numAchat = achatService.prochainNumeroAchat();
        achatService.validerVente(numAchat, "RAKOTO Bernard", LocalDate.now(),
                List.of(new LigneVente(numMedoc, cree.getDesign(), cree.getPrixUnitaire(), 2)));
        verifier(stockService.trouverMedicament(numMedoc).getStock() == 18,
                "Vente de 2 : stock 20 -> 18 (regle 19.3)");

        // Etape 5b : stock insuffisant -> vente bloquee, stock inchange
        try {
            achatService.validerVente(achatService.prochainNumeroAchat(), "Client Test", LocalDate.now(),
                    List.of(new LigneVente(numMedoc, cree.getDesign(), cree.getPrixUnitaire(), 999)));
            throw new AssertionError("Vente avec stock insuffisant acceptee");
        } catch (BusinessException e) {
            verifier(stockService.trouverMedicament(numMedoc).getStock() == 18,
                    "Stock insuffisant bloque, stock inchange (regle 19.4)");
        }

        // Etape 5c : panier multi-lignes dans un meme achat
        List<Medicament> disponibles = stockService.listerMedicaments().stream()
                .filter(m -> m.getStock() >= 3 && !m.getNumMedoc().equals(numMedoc))
                .limit(2).toList();
        if (disponibles.size() == 2) {
            String numMulti = achatService.prochainNumeroAchat();
            int stockA = disponibles.get(0).getStock();
            int stockB = disponibles.get(1).getStock();

            achatService.validerVente(numMulti, "Client Multi", LocalDate.now(), List.of(
                    new LigneVente(disponibles.get(0).getNumMedoc(), disponibles.get(0).getDesign(),
                            disponibles.get(0).getPrixUnitaire(), 3),
                    new LigneVente(disponibles.get(1).getNumMedoc(), disponibles.get(1).getDesign(),
                            disponibles.get(1).getPrixUnitaire(), 2)));

            boolean stocksOk =
                    stockService.trouverMedicament(disponibles.get(0).getNumMedoc()).getStock() == stockA - 3
                            && stockService.trouverMedicament(disponibles.get(1).getNumMedoc()).getStock() == stockB - 2;
            boolean factureOk = achatService.chargerFacture(numMulti).getLignes().size() == 2;
            verifier(stocksOk && factureOk,
                    "Achat multi-medicaments : 2 lignes, les 2 stocks decrementes");
        }

        // Etape 8 : recherche partielle LIKE %...%
        List<Medicament> resultats = stockService.rechercherMedicaments("Test Scenario");
        verifier(resultats.stream().anyMatch(m -> m.getNumMedoc().equals(numMedoc)),
                "Recherche partielle par designation (LIKE %...%)");

        // Suppression refusee tant que le medicament est reference
        try {
            stockService.supprimerMedicament(numMedoc);
            throw new AssertionError("Suppression d'un medicament reference acceptee");
        } catch (BusinessException e) {
            verifier(true, "Suppression refusee car medicament reference (integrite)");
        }

        // Nettoyage : on retire l'achat, l'entree, puis le medicament
        achatService.supprimerLigneAchat(numAchat, numMedoc);
        stockService.supprimerEntree(numEntree);
        stockService.supprimerMedicament(numMedoc);
        verifier(stockService.trouverMedicament(numMedoc) == null,
                "Nettoyage : achat, entree et medicament de test supprimes");

        System.out.println();
        System.out.println("=== SCENARIO COMPLET : " + etape + " ETAPES VALIDEES ===");
        System.exit(0);
    }

    private static void verifier(boolean condition, String description) {
        etape++;
        if (!condition) {
            throw new AssertionError("ECHEC etape " + etape + " : " + description);
        }
        System.out.println("OK  " + etape + ". " + description);
    }
}
