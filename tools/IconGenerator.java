import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Generateur des icones de l'application.
 *
 * Utilitaire de BUILD, il ne fait pas partie du code execute par l'application.
 * Il dessine avec Java2D un jeu d'icones vectorielles homogenes puis les
 * exporte en PNG. Aucun emoji n'est utilise : toutes les icones de
 * l'interface proviennent de ces fichiers.
 *
 * Execution :
 *     java tools/IconGenerator.java src/main/resources/icons
 *
 * Chaque icone est dessinee sur une grille virtuelle de 24x24 unites, avec un
 * trait de 1,9 unite, puis rendue en suréchantillonnage x4 avant reduction
 * progressive : le rendu reste net a toutes les tailles d'affichage.
 */
public final class IconGenerator {

    private static final int GRID = 24;          // grille de dessin
    private static final int OUTPUT = 64;        // taille du PNG produit
    private static final int LOGO_OUTPUT = 128;  // taille du logo
    private static final int SUPERSAMPLE = 4;    // facteur de surechantillonnage
    private static final float STROKE = 1.9f;    // epaisseur du trait (unites grille)

    private static final Color DARK = new Color(0x33, 0x41, 0x55);
    private static final Color WHITE = Color.WHITE;

    public static void main(String[] args) throws Exception {
        File baseDir = new File(args.length > 0 ? args[0] : "src/main/resources/icons");
        File whiteDir = new File(baseDir, "white");
        if (!baseDir.exists() && !baseDir.mkdirs()) throw new IllegalStateException("mkdir " + baseDir);
        if (!whiteDir.exists() && !whiteDir.mkdirs()) throw new IllegalStateException("mkdir " + whiteDir);

        Map<String, Consumer<Graphics2D>> icons = buildIconSet();

        for (Map.Entry<String, Consumer<Graphics2D>> entry : icons.entrySet()) {
            int size = entry.getKey().equals("logo") ? LOGO_OUTPUT : OUTPUT;
            write(new File(baseDir, entry.getKey() + ".png"), entry.getValue(), DARK, size);
            write(new File(whiteDir, entry.getKey() + ".png"), entry.getValue(), WHITE, size);
        }
        System.out.println("Icones generees : " + icons.size() + " x2 variantes -> " + baseDir.getPath());

        // Planche de controle visuel : java tools/IconGenerator.java <dir> <planche.png>
        if (args.length > 1) {
            writeContactSheet(new File(args[1]), icons);
            System.out.println("Planche de controle -> " + args[1]);
        }
    }

    /** Assemble toutes les icones sur une seule image, pour verifier le rendu d'un coup d'oeil. */
    private static void writeContactSheet(File file, Map<String, Consumer<Graphics2D>> icons) throws Exception {
        int cell = 96, cols = 6, pad = 10;
        int rows = (icons.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * cell, rows * (cell + 18), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xEE, 0xF2, 0xF7));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        int index = 0;
        for (Map.Entry<String, Consumer<Graphics2D>> entry : icons.entrySet()) {
            int col = index % cols, row = index++ / cols;
            int x = col * cell, y = row * (cell + 18);
            int size = entry.getKey().equals("logo") ? LOGO_OUTPUT : OUTPUT;

            BufferedImage icon = new BufferedImage(size * SUPERSAMPLE, size * SUPERSAMPLE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = icon.createGraphics();
            ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ig.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            double scale = (double) (size * SUPERSAMPLE) / GRID;
            ig.scale(scale, scale);
            ig.setColor(DARK);
            ig.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            entry.getValue().accept(ig);
            ig.dispose();

            BufferedImage scaled = downscale(icon, 64);
            g.drawImage(scaled, x + (cell - 64) / 2, y + pad, null);
            g.setColor(DARK);
            int textWidth = g.getFontMetrics().stringWidth(entry.getKey());
            g.drawString(entry.getKey(), x + (cell - textWidth) / 2, y + cell + 4);
        }
        g.dispose();
        ImageIO.write(sheet, "png", file);
    }

    // ------------------------------------------------------------------
    //  Rendu
    // ------------------------------------------------------------------

    private static void write(File file, Consumer<Graphics2D> painter, Color color, int size) throws Exception {
        int big = size * SUPERSAMPLE;
        BufferedImage image = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double scale = (double) big / GRID;
        g.scale(scale, scale);
        g.setColor(color);
        g.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        painter.accept(g);
        g.dispose();

        ImageIO.write(downscale(image, size), "png", file);
    }

    /** Reduction progressive par moities : bien plus net qu'une reduction directe. */
    private static BufferedImage downscale(BufferedImage source, int target) {
        BufferedImage current = source;
        int width = source.getWidth();
        while (width / 2 >= target) {
            width /= 2;
            BufferedImage next = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(current, 0, 0, width, width, null);
            g.dispose();
            current = next;
        }
        if (width == target) return current;

        BufferedImage result = new BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(current, 0, 0, target, target, null);
        g.dispose();
        return result;
    }

    // ------------------------------------------------------------------
    //  Primitives de dessin (coordonnees exprimees sur la grille 24x24)
    // ------------------------------------------------------------------

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void circle(Graphics2D g, double cx, double cy, double r) {
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void dot(Graphics2D g, double cx, double cy, double r) {
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void roundRect(Graphics2D g, double x, double y, double w, double h, double arc) {
        g.draw(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    private static void fillRoundRect(Graphics2D g, double x, double y, double w, double h, double arc) {
        g.fill(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    /** Construit un chemin a partir d'une suite de points x,y ; ferme le contour si close. */
    private static Path2D poly(boolean close, double... pts) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i], pts[i + 1]);
        if (close) path.closePath();
        return path;
    }

    // ------------------------------------------------------------------
    //  Definition du jeu d'icones
    // ------------------------------------------------------------------

    private static Map<String, Consumer<Graphics2D>> buildIconSet() {
        Map<String, Consumer<Graphics2D>> icons = new LinkedHashMap<>();

        // Tableau de bord : quatre tuiles
        icons.put("dashboard", g -> {
            roundRect(g, 3.2, 3.2, 7.6, 7.6, 1.6);
            roundRect(g, 13.2, 3.2, 7.6, 4.4, 1.6);
            roundRect(g, 13.2, 10, 7.6, 10.8, 1.6);
            roundRect(g, 3.2, 13.4, 7.6, 7.4, 1.6);
        });

        // Stock : caisse de rangement (couvercle + corps + poignee)
        icons.put("stock", g -> {
            roundRect(g, 2.8, 4.2, 18.4, 4.8, 1.3);
            g.draw(poly(false, 4.7, 9, 4.7, 20.4, 19.3, 20.4, 19.3, 9));
            line(g, 9.5, 13.6, 14.5, 13.6);
        });

        // Medicament : gelule
        icons.put("medicament", g -> {
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(-45), 12, 12);
            roundRect(g, 3.6, 8.6, 16.8, 6.8, 6.8);
            line(g, 12, 8.6, 12, 15.4);
            g.setTransform(old);
        });

        // Entree de stock : carton avec fleche descendante
        icons.put("entree", g -> {
            g.draw(poly(false, 3.6, 13.4, 3.6, 20.6, 20.4, 20.6, 20.4, 13.4));
            line(g, 12, 3.4, 12, 13.6);
            g.draw(poly(false, 8.2, 9.8, 12, 13.6, 15.8, 9.8));
        });

        // Achat : chariot de courses
        icons.put("achat", g -> {
            g.draw(poly(false, 2.4, 3.6, 5.2, 3.6, 8.4, 15.2, 19.2, 15.2));
            g.draw(poly(true, 6.5, 7.2, 21.4, 7.2, 19.4, 12.8, 8.1, 12.8));
            circle(g, 9.6, 18.6, 1.5);
            circle(g, 17.8, 18.6, 1.5);
        });

        // Historique : horloge
        icons.put("history", g -> {
            circle(g, 12, 12, 8.4);
            g.draw(poly(false, 12, 6.6, 12, 12.2, 16.2, 14.4));
        });

        // Bilan : histogramme
        icons.put("bilan", g -> {
            g.draw(poly(false, 4, 3.6, 4, 20.2, 20.8, 20.2));
            fillRoundRect(g, 7, 13.4, 3.2, 6.8, 0.9);
            fillRoundRect(g, 12.1, 9.4, 3.2, 10.8, 0.9);
            fillRoundRect(g, 17.2, 5.8, 3.2, 14.4, 0.9);
        });

        // Ajouter : plus cercle
        icons.put("add", g -> {
            circle(g, 12, 12, 8.4);
            line(g, 12, 7.4, 12, 16.6);
            line(g, 7.4, 12, 16.6, 12);
        });

        // Modifier : crayon
        icons.put("edit", g -> {
            g.draw(poly(true, 3.8, 20.2, 8.2, 19.1, 19.6, 7.7, 16.3, 4.4, 4.9, 15.8));
            line(g, 4.9, 15.8, 8.2, 19.1);
            line(g, 14.2, 6.5, 17.5, 9.8);
        });

        // Supprimer : corbeille
        icons.put("delete", g -> {
            line(g, 3.6, 6.4, 20.4, 6.4);
            g.draw(poly(false, 9, 6.4, 9, 4, 15, 4, 15, 6.4));
            g.draw(poly(false, 5.6, 6.4, 6.9, 20.6, 17.1, 20.6, 18.4, 6.4));
            line(g, 10.2, 10, 10.2, 17);
            line(g, 13.8, 10, 13.8, 17);
        });

        // Rechercher : loupe
        icons.put("search", g -> {
            circle(g, 10.4, 10.4, 6.6);
            line(g, 15.2, 15.2, 20.6, 20.6);
        });

        // Enregistrer : disquette
        icons.put("save", g -> {
            Path2D.Double body = new Path2D.Double();
            body.moveTo(3.6, 6);
            body.quadTo(3.6, 3.6, 6, 3.6);
            body.lineTo(16.4, 3.6);
            body.lineTo(20.4, 7.6);
            body.lineTo(20.4, 18);
            body.quadTo(20.4, 20.4, 18, 20.4);
            body.lineTo(6, 20.4);
            body.quadTo(3.6, 20.4, 3.6, 18);
            body.closePath();
            g.draw(body);
            g.draw(poly(true, 7.8, 3.6, 16, 3.6, 16, 8.6, 7.8, 8.6));
            g.draw(poly(true, 7.2, 13.2, 16.8, 13.2, 16.8, 20.4, 7.2, 20.4));
        });

        // Imprimer : imprimante
        icons.put("print", g -> {
            g.draw(poly(false, 6.6, 8.6, 6.6, 3.6, 17.4, 3.6, 17.4, 8.6));
            Path2D.Double body = new Path2D.Double();
            body.moveTo(6.6, 8.6);
            body.lineTo(4.2, 8.6);
            body.quadTo(2.8, 8.6, 2.8, 10);
            body.lineTo(2.8, 15.6);
            body.quadTo(2.8, 17, 4.2, 17);
            body.lineTo(6.6, 17);
            g.draw(body);
            Path2D.Double body2 = new Path2D.Double();
            body2.moveTo(17.4, 8.6);
            body2.lineTo(19.8, 8.6);
            body2.quadTo(21.2, 8.6, 21.2, 10);
            body2.lineTo(21.2, 15.6);
            body2.quadTo(21.2, 17, 19.8, 17);
            body2.lineTo(17.4, 17);
            g.draw(body2);
            g.draw(poly(true, 6.6, 13.6, 17.4, 13.6, 17.4, 20.6, 6.6, 20.6));
            dot(g, 18.2, 11.2, 0.85);
        });

        // Facture PDF : document avec coin plie
        icons.put("pdf", g -> {
            g.draw(poly(true, 5.8, 2.6, 14.6, 2.6, 19.4, 7.4, 19.4, 21.4, 5.8, 21.4));
            g.draw(poly(false, 14.6, 2.6, 14.6, 7.4, 19.4, 7.4));
            line(g, 8.8, 11.6, 16.4, 11.6);
            line(g, 8.8, 14.8, 16.4, 14.8);
            line(g, 8.8, 18, 13.2, 18);
        });

        // Deconnexion : porte et fleche sortante
        icons.put("logout", g -> {
            g.draw(poly(false, 10.4, 3.8, 4.2, 3.8, 4.2, 20.2, 10.4, 20.2));
            line(g, 10.8, 12, 20.6, 12);
            g.draw(poly(false, 17.2, 8.4, 20.6, 12, 17.2, 15.6));
        });

        // Actualiser : fleche circulaire
        icons.put("refresh", g -> {
            g.draw(new Arc2D.Double(3.6, 3.6, 16.8, 16.8, 60, 280, Arc2D.OPEN));
            double a = Math.toRadians(60);
            double x = 12 + 8.4 * Math.cos(a);
            double y = 12 - 8.4 * Math.sin(a);
            g.draw(poly(false, x - 4.4, y - 0.6, x, y, x + 0.7, y - 4.3));
        });

        // Utilisateur
        icons.put("user", g -> {
            circle(g, 12, 8.2, 4.2);
            Path2D.Double body = new Path2D.Double();
            body.moveTo(4.4, 20.6);
            body.curveTo(4.4, 15.6, 8, 13.4, 12, 13.4);
            body.curveTo(16, 13.4, 19.6, 15.6, 19.6, 20.6);
            g.draw(body);
        });

        // Mot de passe : cadenas
        icons.put("lock", g -> {
            Path2D.Double shackle = new Path2D.Double();
            shackle.moveTo(7.6, 10.2);
            shackle.lineTo(7.6, 7.6);
            shackle.curveTo(7.6, 3.3, 16.4, 3.3, 16.4, 7.6);
            shackle.lineTo(16.4, 10.2);
            g.draw(shackle);
            roundRect(g, 4.4, 10.2, 15.2, 10.4, 2.4);
            dot(g, 12, 14.6, 1.35);
            line(g, 12, 15.4, 12, 17.6);
        });

        // Alerte : triangle
        icons.put("alert", g -> {
            Path2D.Double tri = new Path2D.Double();
            tri.moveTo(12, 3.4);
            tri.lineTo(21.6, 20.2);
            tri.lineTo(2.4, 20.2);
            tri.closePath();
            g.draw(tri);
            line(g, 12, 9.6, 12, 14.6);
            dot(g, 12, 17.4, 0.85);
        });

        // Validation : coche
        icons.put("check", g -> {
            circle(g, 12, 12, 8.4);
            g.draw(poly(false, 7.6, 12.2, 10.8, 15.4, 16.6, 8.6));
        });

        // Vider le formulaire : croix cerclee
        icons.put("clear", g -> {
            circle(g, 12, 12, 8.4);
            line(g, 9.1, 9.1, 14.9, 14.9);
            line(g, 14.9, 9.1, 9.1, 14.9);
        });

        // Quitter : bouton d'alimentation
        icons.put("power", g -> {
            g.draw(new Arc2D.Double(3.8, 4.6, 16.4, 16.4, 63, 294, Arc2D.OPEN));
            line(g, 12, 3, 12, 11.4);
        });

        // Recette : billet
        icons.put("money", g -> {
            roundRect(g, 2.6, 5.8, 18.8, 12.4, 2);
            circle(g, 12, 12, 3.1);
            line(g, 6.1, 9.4, 6.1, 14.6);
            line(g, 17.9, 9.4, 17.9, 14.6);
        });

        // Logo : croix de pharmacie (forme pleine)
        icons.put("logo", g -> {
            Area cross = new Area(new RoundRectangle2D.Double(9, 2.6, 6, 18.8, 2.6, 2.6));
            cross.add(new Area(new RoundRectangle2D.Double(2.6, 9, 18.8, 6, 2.6, 2.6)));
            g.fill(cross);
        });

        return icons;
    }

    private IconGenerator() {
    }
}
