package pharmacie.ui;

import pharmacie.model.Utilisateur;
import pharmacie.service.AuthService;
import pharmacie.ui.components.Dialogues;
import pharmacie.util.DatabaseConnection;
import pharmacie.util.IconLoader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Fenêtre principale de l'application.
 *
 * Organise l'écran en trois zones : un bandeau supérieur, un menu latéral de
 * navigation et une zone centrale pilotée par un {@link CardLayout}. Chaque
 * entrée du menu affiche le panneau correspondant sans ouvrir de nouvelle fenêtre.
 */
public class MainFrame extends JFrame {

    /** Identifiants des panneaux gérés par le {@link CardLayout}. */
    public static final String VUE_DASHBOARD = "dashboard";
    public static final String VUE_STOCK = "stock";
    public static final String VUE_ACHAT = "achat";
    public static final String VUE_BILAN = "bilan";

    private final Utilisateur utilisateur;

    private final CardLayout dispositionCentrale = new CardLayout();
    private final JPanel zoneCentrale = new JPanel(dispositionCentrale);
    private final List<BoutonMenu> boutonsMenu = new ArrayList<>();

    private DashboardPanel dashboardPanel;
    private StockPanel stockPanel;
    private AchatPanel achatPanel;
    private BilanPanel bilanPanel;

    public MainFrame(Utilisateur utilisateur) {
        super("Gestion Pharmacie");
        this.utilisateur = utilisateur;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quitter();
            }
        });

        definirIconeFenetre();
        construireInterface();

        setMinimumSize(new Dimension(1120, 700));
        setSize(1280, 780);
        setLocationRelativeTo(null);

        afficherVue(VUE_DASHBOARD);
    }

    // ------------------------------------------------------------------
    //  Construction de l'interface
    // ------------------------------------------------------------------

    private void construireInterface() {
        JPanel racine = new JPanel(new BorderLayout());
        racine.setBackground(Theme.FOND);

        racine.add(construireBandeau(), BorderLayout.NORTH);
        racine.add(construireMenuLateral(), BorderLayout.WEST);
        racine.add(construireZoneCentrale(), BorderLayout.CENTER);
        racine.add(construirePiedDePage(), BorderLayout.SOUTH);

        setContentPane(racine);
    }

    /** Bandeau supérieur : nom de l'application et utilisateur connecté. */
    private JPanel construireBandeau() {
        JPanel bandeau = new JPanel(new BorderLayout());
        bandeau.setBackground(Theme.BLANC);
        bandeau.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDURE),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JLabel titre = new JLabel("PHARMACIE", IconLoader.get("logo", 22), SwingConstants.LEFT);
        titre.setFont(Theme.POLICE_LOGO);
        titre.setForeground(Theme.VERT);
        titre.setIconTextGap(10);

        JPanel droite = new JPanel();
        droite.setOpaque(false);
        droite.setLayout(new BoxLayout(droite, BoxLayout.X_AXIS));

        JLabel nomUtilisateur = new JLabel(utilisateur.getNomComplet(),
                IconLoader.get("user", 18), SwingConstants.LEFT);
        nomUtilisateur.setFont(Theme.POLICE_GRASSE);
        nomUtilisateur.setForeground(Theme.TEXTE);
        nomUtilisateur.setIconTextGap(8);

        JLabel role = new JLabel(utilisateur.getRole());
        role.setFont(Theme.POLICE_PETITE);
        role.setForeground(Theme.TEXTE_DOUX);
        role.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        droite.add(nomUtilisateur);
        droite.add(role);

        bandeau.add(titre, BorderLayout.WEST);
        bandeau.add(droite, BorderLayout.EAST);
        return bandeau;
    }

    /** Menu latéral de navigation. */
    private JPanel construireMenuLateral() {
        JPanel menu = new JPanel();
        menu.setBackground(Theme.MENU_FOND);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setPreferredSize(new Dimension(215, 0));
        menu.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        menu.add(intituleSection("NAVIGATION"));
        menu.add(ajouterEntree(menu, "Dashboard", "dashboard", VUE_DASHBOARD));
        menu.add(ajouterEntree(menu, "Stock", "stock", VUE_STOCK));
        menu.add(ajouterEntree(menu, "Achat", "achat", VUE_ACHAT));
        menu.add(ajouterEntree(menu, "Bilan", "bilan", VUE_BILAN));

        menu.add(Box.createVerticalGlue());

        JPanel separateur = new JPanel();
        separateur.setBackground(Theme.MENU_SEPARATEUR);
        separateur.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        menu.add(separateur);
        menu.add(Box.createVerticalStrut(10));

        BoutonMenu deconnexion = new BoutonMenu("Déconnexion", "logout");
        deconnexion.addActionListener(e -> deconnecter());
        menu.add(deconnexion);

        return menu;
    }

    private JLabel intituleSection(String texte) {
        JLabel label = new JLabel(texte);
        label.setFont(Theme.POLICE_LIBELLE);
        label.setForeground(new Color(0x7C, 0x8C, 0x9E));
        label.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** Crée une entrée de menu et l'enregistre pour la gestion de l'état actif. */
    private BoutonMenu ajouterEntree(JPanel menu, String libelle, String icone, String vue) {
        BoutonMenu bouton = new BoutonMenu(libelle, icone);
        bouton.addActionListener(e -> afficherVue(vue));
        bouton.vue = vue;
        boutonsMenu.add(bouton);
        return bouton;
    }

    /** Zone centrale : les quatre écrans empilés dans un CardLayout. */
    private JPanel construireZoneCentrale() {
        zoneCentrale.setBackground(Theme.FOND);
        zoneCentrale.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        dashboardPanel = new DashboardPanel(this);
        stockPanel = new StockPanel();
        achatPanel = new AchatPanel();
        bilanPanel = new BilanPanel();

        zoneCentrale.add(dashboardPanel, VUE_DASHBOARD);
        zoneCentrale.add(stockPanel, VUE_STOCK);
        zoneCentrale.add(achatPanel, VUE_ACHAT);
        zoneCentrale.add(bilanPanel, VUE_BILAN);

        return zoneCentrale;
    }

    private JPanel construirePiedDePage() {
        JPanel pied = new JPanel(new BorderLayout());
        pied.setBackground(Theme.BLANC);
        pied.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDURE),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));

        JLabel texte = new JLabel("Gestion de pharmacie");
        texte.setFont(Theme.POLICE_PETITE);
        texte.setForeground(Theme.TEXTE_DOUX);
        pied.add(texte, BorderLayout.WEST);

        return pied;
    }

    private void definirIconeFenetre() {
        ImageIcon logo = IconLoader.get("logo", 64);
        if (logo != null) setIconImage(logo.getImage());
    }

    // ------------------------------------------------------------------
    //  Navigation
    // ------------------------------------------------------------------

    /**
     * Affiche l'écran demandé et rafraîchit ses données.
     *
     * Les données sont rechargées à chaque affichage : un achat effectué dans
     * l'écran ACHAT est ainsi immédiatement visible dans le Dashboard et le Bilan.
     */
    public void afficherVue(String vue) {
        dispositionCentrale.show(zoneCentrale, vue);

        for (BoutonMenu bouton : boutonsMenu) {
            bouton.setActif(vue.equals(bouton.vue));
        }

        switch (vue) {
            case VUE_DASHBOARD -> dashboardPanel.rafraichir();
            case VUE_STOCK -> stockPanel.rafraichir();
            case VUE_ACHAT -> achatPanel.rafraichir();
            case VUE_BILAN -> bilanPanel.rafraichir();
            default -> {
                // Aucune autre vue n'est enregistrée.
            }
        }
    }

    /** Ferme la session et revient à l'écran de connexion. */
    private void deconnecter() {
        if (!Dialogues.confirmer(this, "Voulez-vous vraiment vous déconnecter ?", "Déconnexion")) {
            return;
        }
        AuthService.deconnecter();
        dispose();
        new LoginFrame().setVisible(true);
    }

    /** Ferme l'application après confirmation. */
    private void quitter() {
        if (Dialogues.confirmer(this, "Voulez-vous vraiment quitter l'application ?", "Quitter")) {
            DatabaseConnection.close();
            System.exit(0);
        }
    }

    // ------------------------------------------------------------------
    //  Bouton du menu latéral
    // ------------------------------------------------------------------

    /**
     * Entrée du menu latéral : fond foncé, icône blanche, indicateur vert
     * lorsqu'elle est active.
     */
    private static class BoutonMenu extends JButton {

        private String vue;
        private boolean actif;
        private boolean survol;

        BoutonMenu(String libelle, String nomIcone) {
            super(libelle);
            setIcon(IconLoader.getWhite(nomIcone, IconLoader.TAILLE_MENU));
            setIconTextGap(12);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(Theme.POLICE_NORMALE);
            setForeground(Theme.MENU_TEXTE);
            setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    survol = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    survol = false;
                    repaint();
                }
            });
        }

        void setActif(boolean actif) {
            this.actif = actif;
            setFont(actif ? Theme.POLICE_GRASSE : Theme.POLICE_NORMALE);
            setForeground(actif ? Theme.BLANC : Theme.MENU_TEXTE);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (actif) {
                g2.setColor(Theme.MENU_FOND_SURVOL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.VERT);
                g2.fillRect(0, 0, 4, getHeight());
            } else if (survol) {
                g2.setColor(Theme.MENU_FOND_SURVOL);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
