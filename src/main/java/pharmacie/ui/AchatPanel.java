package pharmacie.ui;

import pharmacie.ui.components.UIFactory;
import pharmacie.util.IconLoader;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/**
 * Écran ACHAT : nouvelle vente et historique des achats.
 *
 * Une vente validée met à jour les stocks : l'historique et la liste des
 * médicaments sont donc rechargés après chaque opération.
 */
public class AchatPanel extends JPanel {

    private final HistoriqueAchatPanel historiquePanel = new HistoriqueAchatPanel();
    private final NouvelleVentePanel nouvelleVentePanel;
    private final JTabbedPane onglets = new JTabbedPane();

    public AchatPanel() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        // Après une vente, l'historique doit refléter le nouvel achat.
        nouvelleVentePanel = new NouvelleVentePanel(historiquePanel::rafraichir);

        add(UIFactory.enteteEcran("Achats",
                "Vente de médicaments et historique des opérations"), BorderLayout.NORTH);
        add(construireOnglets(), BorderLayout.CENTER);
    }

    private JTabbedPane construireOnglets() {
        onglets.setFont(Theme.POLICE_GRASSE);
        onglets.setBackground(Theme.FOND);
        onglets.setBorder(BorderFactory.createEmptyBorder());

        onglets.addTab("Nouvelle vente", IconLoader.get("achat", IconLoader.TAILLE_BOUTON), nouvelleVentePanel);
        onglets.addTab("Historique", IconLoader.get("history", IconLoader.TAILLE_BOUTON), historiquePanel);

        onglets.addChangeListener(e -> rafraichirOngletActif());
        return onglets;
    }

    /** Recharge les données de l'onglet visible. */
    public void rafraichir() {
        rafraichirOngletActif();
    }

    private void rafraichirOngletActif() {
        if (onglets.getSelectedIndex() == 0) {
            nouvelleVentePanel.rafraichir();
        } else {
            historiquePanel.rafraichir();
        }
    }
}
