package id.co.bca.crosbatch.file;

import org.springframework.batch.infrastructure.item.file.LineCallbackHandler;
import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Turns a line into a {@link Row}. Used by {@code FlatFileItemReader}: the header line (skipped
 * line) is handed to {@link #handleLine(String)} and becomes the column names, so fields are
 * looked up by name and every data line must have the same number of fields as the header.
 */
public class CrosLineMapper implements LineMapper<Row>, LineCallbackHandler {

    private final String delimiter;
    private final DelimitedLineTokenizer tokenizer;
    private final CrosDateParser dateParser;

    public CrosLineMapper(String delimiter, CrosDateParser dateParser) {
        this.delimiter = delimiter;
        this.tokenizer = new DelimitedLineTokenizer(delimiter);
        // The files have no quoting; free text may contain '"', so disable quote handling.
        this.tokenizer.setQuoteCharacter('\u0000');
        this.tokenizer.setStrict(true);
        this.dateParser = dateParser;
    }

    /** Header line. */
    @Override
    public void handleLine(String header) {
        String[] names = Arrays.stream(header.split(Pattern.quote(delimiter), -1))
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
        return new Row(tokenizer.tokenize(line), lineNumber, dateParser);
    }
}
