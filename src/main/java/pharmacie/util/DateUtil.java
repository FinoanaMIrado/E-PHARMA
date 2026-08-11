package pharmacie.util;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Conversions et formatage des dates.
 *
 * Le format de saisie et d'affichage retenu pour toute l'application est
 * {@code jj/mm/aaaa}, conforme aux exemples du cahier des charges.
 */
public final class DateUtil {

    /** Format affiché et saisi dans l'interface. */
    public static final DateTimeFormatter FORMAT_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMAT_MOIS =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

    private DateUtil() {
    }

    /** Formate une date en {@code jj/mm/aaaa} ; chaîne vide si la date est absente. */
    public static String format(LocalDate date) {
        return date == null ? "" : date.format(FORMAT_FR);
    }

    /**
     * Analyse une date saisie au format {@code jj/mm/aaaa}.
     *
     * @return la date, ou {@code null} si le texte est vide ou invalide
     */
    public static LocalDate parse(String texte) {
        if (texte == null || texte.isBlank()) return null;
        try {
            return LocalDate.parse(texte.trim(), FORMAT_FR);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Libellé court d'un mois, utilisé sous les barres de l'histogramme (ex. « août 2026 »). */
    public static String formatMois(YearMonth mois) {
        return mois.format(FORMAT_MOIS);
    }

    /** Convertit une date SQL en {@link LocalDate}, en tolérant la valeur nulle. */
    public static LocalDate toLocalDate(Date sqlDate) {
        return sqlDate == null ? null : sqlDate.toLocalDate();
    }

    /** Convertit une {@link LocalDate} en date SQL, en tolérant la valeur nulle. */
    public static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }
}
