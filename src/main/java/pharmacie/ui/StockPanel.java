package pharmacie.ui;

import pharmacie.ui.components.UIFactory;
import pharmacie.util.IconLoader;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/**
 * Écran STOCK : regroupe la gestion des médicaments et celle des entrées.
 *
 * Les deux sous-écrans sont présentés dans des onglets ; l'onglet affiché est
 * rechargé à chaque changement afin que le stock mis à jour par une entrée soit
 * immédiatement visible dans la liste des médicaments.
 */
public class StockPanel extends JPanel {

    private final MedicamentPanel medicamentPanel = new MedicamentPanel();
    private final EntreePanel entreePanel;
    private final JTabbedPane onglets = new JTabbedPane();

    public StockPanel() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        // Une entrée modifie le stock : la liste des médicaments doit être rechargée.
        entreePanel = new EntreePanel(medicamentPanel::rafraichir);

        add(UIFactory.enteteEcran("Gestion du stock",
                "Médicaments enregistrés et entrées de stock"), BorderLayout.NORTH);
        add(construireOnglets(), BorderLayout.CENTER);
    }

    private JTabbedPane construireOnglets() {
        onglets.setFont(Theme.POLICE_GRASSE);
        onglets.setBackground(Theme.FOND);
        onglets.setBorder(BorderFactory.createEmptyBorder());

        onglets.addTab("Médicaments", IconLoader.get("medicament", IconLoader.TAILLE_BOUTON), medicamentPanel);
        onglets.addTab("Entrées de stock", IconLoader.get("entree", IconLoader.TAILLE_BOUTON), entreePanel);

        onglets.addChangeListener(e -> rafraichirOngletActif());
        return onglets;
    }

    /** Recharge les données de l'onglet visible. */
    public void rafraichir() {
        rafraichirOngletActif();
    }

    private void rafraichirOngletActif() {
        if (onglets.getSelectedIndex() == 0) {
            medicamentPanel.rafraichir();
        } else {
            entreePanel.rafraichir();
        }
    }
}
