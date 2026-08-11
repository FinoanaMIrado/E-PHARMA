package pharmacie.service;

import java.io.File;

/** Génère la facture d'un achat existant dans un fichier, pour relecture visuelle. */
public class FactureDemo {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String numAchat = args.length > 0 ? args[0] : "A001";
        File sortie = new File(args.length > 1 ? args[1] : "/tmp/facture_demo.pdf");
        new FacturePdfService().genererPdf(new AchatService().chargerFacture(numAchat), sortie);
        System.out.println("Facture " + numAchat + " -> " + sortie.getAbsolutePath()
                + " (" + sortie.length() + " octets)");
    }
}
