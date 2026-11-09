package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import id.co.bca.crosbatch.file.CrosDateParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Finds the period of the files in the input folder from the TGL_DATA column (first data row
 * of every file that has it). Used when no period is given, e.g. a scheduled run.
 */
@Slf4j
@Component
public class PeriodDetector {

    static final String TGL_DATA = "TGL_DATA";

    private final CrosBatchProperties props;
    private final CrosFileLocator locator;
    private final Pattern delimiter;
    private final Charset charset;
    private final CrosDateParser dateParser;

    public PeriodDetector(CrosBatchProperties props, CrosFileLocator locator) {
        this.props = props;
        this.locator = locator;
        this.delimiter = Pattern.compile(Pattern.quote(props.getDelimiter()));
        this.charset = Charset.forName(props.getCharset());
        this.dateParser = new CrosDateParser(props.getDatePatterns(), props.getTwoDigitYearBase());
    }

    /**
     * @return the period shared by all input files with TGL_DATA; empty when the input folder has no
     *         CROS file, or when file names contain the period (then the name decides)
     * @throws IllegalArgumentException the files belong to different periods, TGL_DATA is invalid,
     *         or there are input files but none of them has TGL_DATA
     */
    public Optional<YearMonth> detect() {
        if (locator.fileNamesContainPeriod()) {
            return Optional.empty();
        }
        Map<String, YearMonth> periods = new LinkedHashMap<>();
        int inputFiles = 0;
        for (CrosBatchProperties.FileConfig file : props.getFiles()) {
            if (!file.isEnabled()) {
                continue;
            }
            Path path = locator.inputFile(file, "");
            if (Files.isRegularFile(path)) {
                inputFiles++;
                firstTglData(path).ifPresent(ym -> periods.put(path.getFileName().toString(), ym));
            }
        }
        if (inputFiles > 0 && periods.isEmpty()) {
            // e.g. only REGION_DIM / BRANCH_DIM / NPL left: guessing the month could load them under the wrong period.
            throw new IllegalArgumentException("cannot determine the period: none of the input files has TGL_DATA."
                    + " Give the period explicitly (crosbatch.period / API body)");
        }
        if (periods.values().stream().distinct().count() > 1) {
            StringBuilder sb = new StringBuilder();
            periods.forEach((name, ym) -> sb.append(name).append('=').append(locator.format(ym)).append(' '));
            throw new IllegalArgumentException("input files belong to different periods: " + sb.toString().trim());
        }
        return periods.values().stream().findFirst();
    }

    private Optional<YearMonth> firstTglData(Path file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file), charset))) {
            String header = reader.readLine();
            if (header == null) {
                return Optional.empty();
            }
            String[] names = delimiter.split(header.replace("\uFEFF", ""), -1);
            int index = -1;
            for (int i = 0; i < names.length; i++) {
                if (TGL_DATA.equalsIgnoreCase(names[i].strip())) {
                    index = i;
                }
            }
            if (index < 0) {
                return Optional.empty();
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = delimiter.split(line, -1);
                String value = index < fields.length ? fields[index].strip() : "";
                if (value.isEmpty()) {
                    throw new IllegalArgumentException(file.getFileName() + ": first data row has no TGL_DATA");
                }
                try {
                    return Optional.of(YearMonth.from(dateParser.parse(value)));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(file.getFileName() + ": invalid TGL_DATA '" + value + "'");
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
    }
}
