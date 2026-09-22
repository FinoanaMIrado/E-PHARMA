package pharmacie.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import pharmacie.model.Achat;
import pharmacie.model.Facture;
import pharmacie.util.BusinessException;
import pharmacie.util.DateUtil;
import pharmacie.util.MontantUtil;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Génération de la facture PDF d'un achat (Apache PDFBox).
 *
 * La facture reprend les éléments exigés par le cahier des charges : date, nom
 * du client, puis pour chaque ligne la désignation, le prix unitaire, la
 * quantité et le total, suivis du total général.
 *
 * La mise en page est dessinée directement : les colonnes sont positionnées en
 * points depuis le bord de la page, et les montants sont alignés à droite en
 * mesurant la largeur du texte.
 */
public class FacturePdfService {

    private static final PDFont POLICE = PDType1Font.HELVETICA;
    private static final PDFont POLICE_GRASSE = PDType1Font.HELVETICA_BOLD;

    private static final Color VERT_PRINCIPAL = new Color(21, 128, 97);
    private static final Color GRIS_ENTETE = new Color(240, 243, 245);
    private static final Color GRIS_TEXTE = new Color(90, 100, 110);
    private static final Color GRIS_LIGNE = new Color(226, 232, 236);

    /** Nom affiché en en-tête. Non spécifié par le cahier des charges : valeur par défaut. */
    private static final String NOM_PHARMACIE = "PHARMACIE FIANARANTSOA";

    // Géométrie de la page (en points, origine en bas à gauche)
    private static final float MARGE = 50;
    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();
    private static final float DROITE = LARGEUR_PAGE - MARGE;
    private static final float HAUTEUR_LIGNE = 20;

    // Position horizontale des colonnes du tableau
    private static final float COL_DESIGNATION = MARGE + 8;
    private static final float COL_PRIX_FIN = MARGE + 300;
    private static final float COL_QUANTITE_FIN = MARGE + 380;
    private static final float COL_TOTAL_FIN = DROITE - 8;

    /**
     * Écrit la facture dans le fichier indiqué.
     *
     * @throws BusinessException si le fichier ne peut pas être écrit
     */
    public void genererPdf(Facture facture, File fichier) throws BusinessException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream flux = new PDPageContentStream(document, page)) {
                float y = PDRectangle.A4.getHeight() - MARGE;
                y = dessinerEntete(flux, facture, y);
                y = dessinerTableau(flux, facture.getLignes(), y);
                dessinerTotal(flux, facture.getTotalGeneral(), y);
                dessinerPiedDePage(flux);
            }

            document.save(fichier);
        } catch (IOException e) {
            throw new BusinessException(
                    "Impossible d'écrire la facture PDF.\n\nDétail : " + e.getMessage(), e);
        }
    }

    /** Nom de fichier proposé par défaut pour une facture. */
    public String nomFichierPropose(Facture facture) {
        return "facture_" + facture.getNumAchat() + ".pdf";
    }

    // ------------------------------------------------------------------
    //  Sections de la facture
    // ------------------------------------------------------------------

    private float dessinerEntete(PDPageContentStream flux, Facture facture, float y) throws IOException {
        ecrire(flux, POLICE_GRASSE, 18, VERT_PRINCIPAL, MARGE, y, NOM_PHARMACIE);
        y -= 16;
        ecrire(flux, POLICE, 9, GRIS_TEXTE, MARGE, y, "E-PHARMA");

        y -= 34;
        ecrire(flux, POLICE_GRASSE, 14, Color.BLACK, MARGE, y, "FACTURE");

        y -= 24;
        y = dessinerInfo(flux, y, "Facture n°", facture.getNumAchat());
        y = dessinerInfo(flux, y, "Date", DateUtil.format(facture.getDate()));
        y = dessinerInfo(flux, y, "Client", facture.getNomClient());

        return y - 18;
    }

    private float dessinerInfo(PDPageContentStream flux, float y, String libelle, String valeur)
            throws IOException {
        ecrire(flux, POLICE, 9, GRIS_TEXTE, MARGE, y, libelle);
        ecrire(flux, POLICE_GRASSE, 10, Color.BLACK, MARGE + 90, y, valeur);
        return y - 16;
    }

    private float dessinerTableau(PDPageContentStream flux, List<Achat> lignes, float y) throws IOException {
        // Bandeau d'en-tête
        remplirRectangle(flux, MARGE, y - 6, DROITE - MARGE, HAUTEUR_LIGNE + 2, GRIS_ENTETE);
        ecrire(flux, POLICE_GRASSE, 9, Color.BLACK, COL_DESIGNATION, y, "Désignation");
        ecrireADroite(flux, POLICE_GRASSE, 9, Color.BLACK, COL_PRIX_FIN, y, "Prix unitaire");
        ecrireADroite(flux, POLICE_GRASSE, 9, Color.BLACK, COL_QUANTITE_FIN, y, "Quantité");
        ecrireADroite(flux, POLICE_GRASSE, 9, Color.BLACK, COL_TOTAL_FIN, y, "Total");

        y -= 8;
        tracerLigne(flux, MARGE, y, DROITE, y, VERT_PRINCIPAL, 1f);
        y -= HAUTEUR_LIGNE;

        for (Achat ligne : lignes) {
            ecrire(flux, POLICE, 10, Color.BLACK, COL_DESIGNATION, y,
                    tronquer(ligne.getDesignMedicament(), 38));
            ecrireADroite(flux, POLICE, 10, Color.BLACK, COL_PRIX_FIN, y,
                    MontantUtil.formatAvecDevise(ligne.getPrixUnitaire()));
            ecrireADroite(flux, POLICE, 10, Color.BLACK, COL_QUANTITE_FIN, y,
                    String.valueOf(ligne.getNbr()));
            ecrireADroite(flux, POLICE, 10, Color.BLACK, COL_TOTAL_FIN, y,
                    MontantUtil.formatAvecDevise(ligne.getTotalLigne()));

            y -= 7;
            tracerLigne(flux, MARGE, y, DROITE, y, GRIS_LIGNE, 0.5f);
            y -= HAUTEUR_LIGNE - 7;
        }

        return y;
    }

    private void dessinerTotal(PDPageContentStream flux, long total, float y) throws IOException {
        float hauteur = 28;
        float largeur = 250;
        float x = DROITE - largeur;

        remplirRectangle(flux, x, y - hauteur + 8, largeur, hauteur, GRIS_ENTETE);
        ecrire(flux, POLICE_GRASSE, 11, Color.BLACK, x + 12, y - 8, "TOTAL GÉNÉRAL");
        ecrireADroite(flux, POLICE_GRASSE, 12, VERT_PRINCIPAL, DROITE - 12, y - 8,
                MontantUtil.formatAvecDevise(total));
    }

    private void dessinerPiedDePage(PDPageContentStream flux) throws IOException {
        String texte = "Merci de votre confiance.";
        float largeur = largeurTexte(POLICE, 9, texte);
        ecrire(flux, POLICE, 9, GRIS_TEXTE, (LARGEUR_PAGE - largeur) / 2, MARGE, texte);
    }

    // ------------------------------------------------------------------
    //  Primitives de dessin
    // ------------------------------------------------------------------

    private void ecrire(PDPageContentStream flux, PDFont police, float taille,
                        Color couleur, float x, float y, String texte) throws IOException {
        flux.beginText();
        flux.setFont(police, taille);
        flux.setNonStrokingColor(couleur);
        flux.newLineAtOffset(x, y);
        flux.showText(nettoyer(texte));
        flux.endText();
    }

    /** Écrit un texte dont le bord droit est aligné sur {@code xFin}. */
    private void ecrireADroite(PDPageContentStream flux, PDFont police, float taille,
                               Color couleur, float xFin, float y, String texte) throws IOException {
        String propre = nettoyer(texte);
        ecrire(flux, police, taille, couleur, xFin - largeurTexte(police, taille, propre), y, propre);
    }

    private void remplirRectangle(PDPageContentStream flux, float x, float y,
                                  float largeur, float hauteur, Color couleur) throws IOException {
        flux.setNonStrokingColor(couleur);
        flux.addRect(x, y, largeur, hauteur);
        flux.fill();
    }

    private void tracerLigne(PDPageContentStream flux, float x1, float y1, float x2, float y2,
                             Color couleur, float epaisseur) throws IOException {
        flux.setStrokingColor(couleur);
        flux.setLineWidth(epaisseur);
        flux.moveTo(x1, y1);
        flux.lineTo(x2, y2);
        flux.stroke();
    }

    private float largeurTexte(PDFont police, float taille, String texte) throws IOException {
        return police.getStringWidth(nettoyer(texte)) / 1000 * taille;
    }

    private String tronquer(String texte, int longueurMax) {
        if (texte == null) return "";
        return texte.length() <= longueurMax ? texte : texte.substring(0, longueurMax - 1) + "…";
    }

    /**
     * Remplace les caractères non représentables par les polices standard PDF.
     *
     * Les polices Type 1 utilisent l'encodage WinAnsi : il couvre le français
     * accentué, mais pas certains caractères typographiques. Sans ce filtrage,
     * une désignation contenant un caractère exotique ferait échouer l'écriture
     * du PDF au lieu de produire une facture.
     */
    private String nettoyer(String texte) {
        if (texte == null) return "";
        String resultat = texte
                .replace("…", "...")
                .replace("’", "'")
                .replace("‘", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("–", "-")
                .replace("—", "-");

        StringBuilder sb = new StringBuilder(resultat.length());
        for (char c : resultat.toCharArray()) {
            sb.append(estRepresentable(c) ? c : '?');
        }
        return sb.toString();
    }

    private boolean estRepresentable(char c) {
        try {
            POLICE.getStringWidth(String.valueOf(c));
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }
}
