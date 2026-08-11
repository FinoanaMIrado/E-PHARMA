package pharmacie.ui;

import pharmacie.model.Utilisateur;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Capture chaque écran de l'application dans un fichier PNG.
 *
 * Outil de vérification visuelle : ouvre les fenêtres réelles, affiche
 * successivement chaque vue puis enregistre le rendu. N'est pas utilisé par
 * l'application elle-même.
 */
public class CaptureEcrans {

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        File dossier = new File(args.length > 0 ? args[0] : "/tmp/captures");
        dossier.mkdirs();

        Theme.installer();

        // 1. Ecran de connexion
        LoginFrame login = construire(LoginFrame::new);
        capturer(login, new File(dossier, "01-login.png"));
        SwingUtilities.invokeAndWait(login::dispose);

        // 2. Fenetre principale : une vue par capture
        Utilisateur utilisateur = new Utilisateur(1, "admin", "Administrateur", "ADMIN");
        MainFrame principale = construire(() -> new MainFrame(utilisateur));

        capturerVue(principale, MainFrame.VUE_DASHBOARD, new File(dossier, "02-dashboard.png"));
        capturerVue(principale, MainFrame.VUE_STOCK, new File(dossier, "03-stock.png"));
        capturerVue(principale, MainFrame.VUE_ACHAT, new File(dossier, "04-achat.png"));
        capturerVue(principale, MainFrame.VUE_BILAN, new File(dossier, "05-bilan.png"));

        SwingUtilities.invokeAndWait(principale::dispose);
        System.out.println("Captures enregistrees dans " + dossier.getAbsolutePath());
        System.exit(0);
    }

    private static <T extends JFrame> T construire(java.util.function.Supplier<T> fabrique) throws Exception {
        final Object[] resultat = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            JFrame fenetre = fabrique.get();
            fenetre.setVisible(true);
            resultat[0] = fenetre;
        });
        @SuppressWarnings("unchecked")
        T fenetre = (T) resultat[0];
        return fenetre;
    }

    private static void capturerVue(MainFrame fenetre, String vue, File fichier) throws Exception {
        SwingUtilities.invokeAndWait(() -> fenetre.afficherVue(vue));
        capturer(fenetre, fichier);
    }

    /** Peint la fenetre dans une image hors ecran : fonctionne meme sans gestionnaire de fenetres. */
    private static void capturer(JFrame fenetre, File fichier) throws Exception {
        // Laisse le temps aux chargements de donnees et a la mise en page de s'appliquer.
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(() -> {
            fenetre.validate();
            BufferedImage image = new BufferedImage(
                    fenetre.getWidth(), fenetre.getHeight(), BufferedImage.TYPE_INT_RGB);
            fenetre.paint(image.getGraphics());
            try {
                ImageIO.write(image, "png", fichier);
                System.out.println("  -> " + fichier.getName()
                        + " (" + image.getWidth() + "x" + image.getHeight() + ")");
            } catch (Exception e) {
                System.err.println("Echec capture " + fichier + " : " + e.getMessage());
            }
        });
    }
}
