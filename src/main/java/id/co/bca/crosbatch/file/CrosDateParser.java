package id.co.bca.crosbatch.file;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;

/**
 * Parses the date fields. Tries each configured pattern in order, e.g. "dd-MM-yy" (30-11-25) and
 * "dd-MMM-yy" (25-OCT-16). Month names are English and case-insensitive; "yy" maps to the
 * 100-year window starting at the two-digit-year base.
 */
public class CrosDateParser {

    private final List<DateTimeFormatter> formatters;
    private final List<String> patterns;

    public CrosDateParser(List<String> patterns, int twoDigitYearBase) {
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException("at least one date pattern is required");
        }
        this.patterns = List.copyOf(patterns);
        this.formatters = patterns.stream().map(p -> build(p.strip(), twoDigitYearBase)).toList();
    }

    public LocalDate parse(String value) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try the next pattern
            }
        }
        throw new DateTimeParseException("'" + value + "' matches none of the date patterns " + patterns, value, 0);
    }

    private static DateTimeFormatter build(String pattern, int base) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().parseCaseInsensitive();
        int yy = pattern.indexOf("yy");
        if (yy < 0 || pattern.contains("yyy")) {
            builder.appendPattern(pattern);
        } else {
            if (yy > 0) {
                builder.appendPattern(pattern.substring(0, yy));
            }
            builder.appendValueReduced(ChronoField.YEAR, 2, 2, base);
            if (yy + 2 < pattern.length()) {
                builder.appendPattern(pattern.substring(yy + 2));
            }
        }
        return builder.toFormatter(Locale.ENGLISH);
    }
}
