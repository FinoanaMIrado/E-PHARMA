package pharmacie.ui;

import pharmacie.model.Utilisateur;
import pharmacie.service.AuthService;
import pharmacie.ui.components.BoutonAction;
import pharmacie.ui.components.Carte;
import pharmacie.ui.components.Dialogues;
import pharmacie.ui.components.UIFactory;
import pharmacie.util.BusinessException;
import pharmacie.util.DatabaseConnection;
import pharmacie.util.IconLoader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Fenêtre de connexion : premier écran affiché au lancement.
 *
 * Contrôle l'accès à la fenêtre principale. En cas d'échec, un message
 * explicite est affiché et le champ mot de passe est vidé.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private final JTextField champLogin = UIFactory.champTexte(18);
    private final JPasswordField champMotDePasse = new JPasswordField(18);
    private final JLabel messageErreur = new JLabel(" ");

    public LoginFrame() {
        super("Gestion Pharmacie - Connexion");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quitter();
            }
        });

        definirIconeFenetre();
        construireInterface();

        pack();
        setMinimumSize(new Dimension(880, 560));
        setSize(920, 580);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    //  Construction de l'interface
    // ------------------------------------------------------------------

    private void construireInterface() {
        JPanel racine = new JPanel(new BorderLayout());
        racine.setBackground(Theme.BLANC);

        racine.add(construirePanneauMarque(), BorderLayout.WEST);
        racine.add(construirePanneauFormulaire(), BorderLayout.CENTER);

        setContentPane(racine);
    }

    /** Bandeau de gauche : identité visuelle de l'application. */
    private JPanel construirePanneauMarque() {
        JPanel panneau = new JPanel(new GridBagLayout());
        panneau.setBackground(Theme.MENU_FOND);
        panneau.setPreferredSize(new Dimension(360, 0));

        JPanel contenu = new JPanel();
        contenu.setOpaque(false);
        contenu.setLayout(new BoxLayout(contenu, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel(IconLoader.getWhite("logo", 64));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titre = new JLabel("GESTION PHARMACIE");
        titre.setFont(Theme.POLICE_TITRE);
        titre.setForeground(Theme.BLANC);
        titre.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sousTitre = new JLabel("Stock, ventes et bilan");
        sousTitre.setFont(Theme.POLICE_NORMALE);
        sousTitre.setForeground(Theme.MENU_TEXTE);
        sousTitre.setAlignmentX(Component.CENTER_ALIGNMENT);

        contenu.add(logo);
        contenu.add(Box.createVerticalStrut(22));
        contenu.add(titre);
        contenu.add(Box.createVerticalStrut(8));
        contenu.add(sousTitre);

        panneau.add(contenu);
        return panneau;
    }

    /** Zone de droite : formulaire de connexion. */
    private JPanel construirePanneauFormulaire() {
        JPanel conteneur = new JPanel(new GridBagLayout());
        conteneur.setBackground(Theme.FOND);

        Carte carte = new Carte();
        carte.setBorder(Carte.marge(32, 36, 32, 36));
        carte.setLayout(new BoxLayout(carte, BoxLayout.Y_AXIS));

        JLabel titre = UIFactory.titre("Connexion");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel aide = UIFactory.aide("Identifiez-vous pour accéder à l'application.");
        aide.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageErreur.setFont(Theme.POLICE_PETITE);
        messageErreur.setForeground(Theme.ROUGE);
        messageErreur.setAlignmentX(Component.LEFT_ALIGNMENT);

        UIFactory.styliserChamp(champMotDePasse);

        BoutonAction boutonConnexion = BoutonAction.principal("Se connecter", "check");
        boutonConnexion.addActionListener(e -> tenterConnexion());

        BoutonAction boutonQuitter = BoutonAction.secondaire("Quitter", "power");
        boutonQuitter.addActionListener(e -> quitter());

        JPanel boutons = UIFactory.barreBoutons(boutonConnexion, boutonQuitter);
        boutons.setAlignmentX(Component.LEFT_ALIGNMENT);
        boutons.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        carte.add(titre);
        carte.add(Box.createVerticalStrut(6));
        carte.add(aide);
        carte.add(Box.createVerticalStrut(22));
        carte.add(bloc("Nom d'utilisateur", "user", champLogin));
        carte.add(Box.createVerticalStrut(14));
        carte.add(bloc("Mot de passe", "lock", champMotDePasse));
        carte.add(Box.createVerticalStrut(10));
        carte.add(messageErreur);
        carte.add(Box.createVerticalStrut(14));
        carte.add(boutons);

        // La touche Entrée valide le formulaire depuis l'un ou l'autre champ.
        KeyAdapter validationEntree = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) tenterConnexion();
            }
        };
        champLogin.addKeyListener(validationEntree);
        champMotDePasse.addKeyListener(validationEntree);

        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.fill = GridBagConstraints.HORIZONTAL;
        contraintes.weightx = 1;
        contraintes.insets = new java.awt.Insets(0, 40, 0, 40);
        conteneur.add(carte, contraintes);

        return conteneur;
    }

    /** Assemble un libellé (avec son icône) au-dessus de son champ de saisie. */
    private JPanel bloc(String libelle, String nomIcone, JComponent champ) {
        JPanel panneau = new JPanel();
        panneau.setOpaque(false);
        panneau.setLayout(new BoxLayout(panneau, BoxLayout.Y_AXIS));
        panneau.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = UIFactory.libelle(libelle.toUpperCase());
        label.setIcon(IconLoader.get(nomIcone, 14));
        label.setIconTextGap(6);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        champ.setAlignmentX(Component.LEFT_ALIGNMENT);
        champ.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        panneau.add(label);
        panneau.add(Box.createVerticalStrut(6));
        panneau.add(champ);
        return panneau;
    }

    private void definirIconeFenetre() {
        ImageIcon logo = IconLoader.get("logo", 64);
        if (logo != null) setIconImage(logo.getImage());
    }

    // ------------------------------------------------------------------
    //  Actions
    // ------------------------------------------------------------------

    /** Vérifie les identifiants puis ouvre la fenêtre principale. */
    private void tenterConnexion() {
        messageErreur.setText(" ");
        char[] motDePasse = champMotDePasse.getPassword();

        try {
            Utilisateur utilisateur = authService.authentifier(champLogin.getText(), motDePasse);
            ouvrirFenetrePrincipale(utilisateur);
        } catch (BusinessException e) {
            afficherErreur(e.getMessage());
        } finally {
            // Le mot de passe est effacé de la mémoire après usage.
            java.util.Arrays.fill(motDePasse, '\0');
        }
    }

    /**
     * Affiche l'erreur dans le formulaire, et en boîte de dialogue si le message
     * est détaillé (erreur de connexion à la base, par exemple).
     */
    private void afficherErreur(String message) {
        String premiereLigne = message.split("\n")[0];
        messageErreur.setText(premiereLigne);
        champMotDePasse.setText("");
        champMotDePasse.requestFocusInWindow();

        if (message.contains("\n")) {
            Dialogues.erreur(this, message);
        }
    }

    private void ouvrirFenetrePrincipale(Utilisateur utilisateur) {
        MainFrame fenetre = new MainFrame(utilisateur);
        fenetre.setVisible(true);
        dispose();
    }

    /** Ferme l'application après confirmation. */
    private void quitter() {
        if (Dialogues.confirmer(this, "Voulez-vous vraiment quitter l'application ?", "Quitter")) {
            DatabaseConnection.close();
            System.exit(0);
        }
    }

    /** Place le curseur dans le champ nom d'utilisateur à l'affichage. */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) SwingUtilities.invokeLater(champLogin::requestFocusInWindow);
    }
}
