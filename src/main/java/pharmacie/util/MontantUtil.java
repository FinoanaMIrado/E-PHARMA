package pharmacie.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formatage des montants.
 *
 * Le cahier des charges exprime les montants en ariary (« 3 500 Ar ») avec des
 * valeurs entières : les prix et totaux sont donc des entiers, sans décimale.
 */
public final class MontantUtil {

    /** Devise utilisée dans l'interface et sur les factures. */
    public static final String DEVISE = "Ar";

    private static final DecimalFormat FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setGroupingSeparator(' ');
        FORMAT = new DecimalFormat("#,##0", symbols);
    }

    private MontantUtil() {
    }

    /** Formate un montant avec séparateur de milliers, sans devise (ex. « 3 500 »). */
    public static String format(long montant) {
        return FORMAT.format(montant);
    }

    /** Formate un montant suivi de la devise (ex. « 3 500 Ar »). */
    public static String formatAvecDevise(long montant) {
        return FORMAT.format(montant) + " " + DEVISE;
    }
}
