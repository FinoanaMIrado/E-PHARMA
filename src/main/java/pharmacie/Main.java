package pharmacie;

import pharmacie.ui.LoginFrame;
import pharmacie.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * Point d'entrée de l'application de gestion de pharmacie.
 *
 * Installe la charte graphique puis ouvre la fenêtre de connexion. Toute la
 * construction de l'interface est effectuée dans le fil de distribution des
 * événements Swing (EDT), comme l'exige la bibliothèque.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.installer();
            new LoginFrame().setVisible(true);
        });
    }
}
