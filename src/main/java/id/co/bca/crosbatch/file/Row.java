package id.co.bca.crosbatch.file;

import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * One line of an input file, as produced by the Spring Batch reader. Values are looked up by
 * header column name and trimmed of spaces and tabs; blank values are returned as {@code null}.
 * A blank line in the file becomes a {@link #isBlank() blank} row that the processor filters out.
 */
public final class Row {

    private final FieldSet fieldSet;
    private final int lineNumber;
    private final DateTimeFormatter dateFormatter;

    Row(FieldSet fieldSet, int lineNumber, DateTimeFormatter dateFormatter) {
        this.fieldSet = fieldSet;
        this.lineNumber = lineNumber;
        this.dateFormatter = dateFormatter;
    }

    static Row blank(int lineNumber) {
        return new Row(null, lineNumber, null);
    }

    public boolean isBlank() {
        return fieldSet == null;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String str(String column) {
        String value = fieldSet.readString(column); // throws if the column is not in the header
        return value == null || value.isEmpty() ? null : value;
    }

    public Long lng(String column) {
        String v = str(column);
        return v == null ? null : parse(column, v, Long::valueOf);
    }

    public Integer integer(String column) {
        String v = str(column);
        return v == null ? null : parse(column, v, Integer::valueOf);
    }

    public BigDecimal dec(String column) {
        String v = str(column);
        return v == null ? null : parse(column, v, BigDecimal::new);
    }

    public LocalDate date(String column) {
        String v = str(column);
        return v == null ? null : parse(column, v, s -> LocalDate.parse(s, dateFormatter));
    }

    private static <T> T parse(String column, String value, Function<String, T> parser) {
        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid value '" + value + "' for column " + column, e);
        }
    }
}
