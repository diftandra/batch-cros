package id.co.bca.crosbatch.file;

import org.springframework.batch.infrastructure.item.file.LineCallbackHandler;
import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;

/**
 * Turns a line into a {@link Row}. Used by {@code FlatFileItemReader}: the header line (skipped
 * line) is handed to {@link #handleLine(String)} and becomes the column names, so fields are
 * looked up by name and every data line must have the same number of fields as the header.
 */
public class CrosLineMapper implements LineMapper<Row>, LineCallbackHandler {

    private final String delimiter;
    private final DelimitedLineTokenizer tokenizer;
    private final DateTimeFormatter dateFormatter;

    public CrosLineMapper(String delimiter, String datePattern, int twoDigitYearBase) {
        this.delimiter = delimiter;
        this.tokenizer = new DelimitedLineTokenizer(delimiter);
        // The files have no quoting; free text may contain '"', so disable quote handling.
        this.tokenizer.setQuoteCharacter('\u0000');
        this.tokenizer.setStrict(true);
        this.dateFormatter = buildDateFormatter(datePattern, twoDigitYearBase);
    }

    /** Header line. */
    @Override
    public void handleLine(String header) {
        String[] names = Arrays.stream(header.split(java.util.regex.Pattern.quote(delimiter), -1))
                .map(n -> n.replace("\uFEFF", "").strip().toUpperCase())
                .toArray(String[]::new);
        tokenizer.setNames(names);
    }

    @Override
    public Row mapLine(String line, int lineNumber) {
        if (line.isBlank()) {
            return Row.blank(lineNumber);
        }
        if (!tokenizer.hasNames()) {
            throw new IllegalStateException("file has no header line");
        }
        return new Row(tokenizer.tokenize(line), lineNumber, dateFormatter);
    }

    /** Builds a formatter where "yy" maps to the 100-year window starting at {@code base}. */
    static DateTimeFormatter buildDateFormatter(String pattern, int base) {
        int yy = pattern.indexOf("yy");
        if (yy < 0 || pattern.contains("yyy")) {
            return DateTimeFormatter.ofPattern(pattern);
        }
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        if (yy > 0) {
            builder.appendPattern(pattern.substring(0, yy));
        }
        builder.appendValueReduced(ChronoField.YEAR, 2, 2, base);
        if (yy + 2 < pattern.length()) {
            builder.appendPattern(pattern.substring(yy + 2));
        }
        return builder.toFormatter();
    }
}
