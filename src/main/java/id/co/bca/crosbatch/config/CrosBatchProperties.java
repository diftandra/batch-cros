package id.co.bca.crosbatch.config;

import id.co.bca.crosbatch.file.CrosFileType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * All runtime settings of the batch. Values come from application.yml and can be
 * overridden by environment variables or command-line arguments (--crosbatch.xxx=...).
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "crosbatch")
public class CrosBatchProperties {

    /** Directory that contains the monthly input files. */
    @NotNull
    private Path inputDir;

    /** Directory where processed files are moved. A sub-directory per period is created. */
    @NotNull
    private Path backupDir;

    /** Period to process in {@link #periodFormat}. Empty = previous month of the run date. */
    private String period;

    /** Format of the period in file names and in the {@code period} argument. */
    @NotBlank
    private String periodFormat;

    /** File name template. Placeholders: {prefix}, {period}. */
    @NotBlank
    private String fileNamePattern;

    /** Field delimiter, taken literally (not a regex). */
    @NotBlank
    private String delimiter;

    @NotBlank
    private String charset;

    /** Date pattern of the date fields. "yy" is resolved with {@link #twoDigitYearBase}. */
    @NotBlank
    private String datePattern;

    /** Two-digit years are mapped to [base, base + 99]. 1950 maps 86 -> 1986 and 25 -> 2025. */
    private int twoDigitYearBase;

    /** Number of rows per JDBC batch / flush. */
    @Min(1)
    private int batchSize;

    /** Log a progress line every N rows. */
    @Min(1)
    private long progressLogInterval;

    /** When false, a file with a header but no data rows is treated as an error. */
    private boolean allowEmptyFile;

    /**
     * A job execution still marked as running in the Spring Batch tables blocks a new run, because
     * it may be a run in progress. When the earlier run was killed, set this to true once to mark it
     * FAILED and continue.
     */
    private boolean recoverStaleExecutions;

    /** Timestamp appended to the backup file name. */
    @NotBlank
    private String backupTimestampPattern;

    /** Files to process, in processing order. */
    @NotEmpty
    private List<@Valid FileConfig> files = new ArrayList<>();

    /**
     * true (CLI mode): run the job once at startup, then exit with 0/1.
     * false (API mode): stay up and wait for REST calls.
     */
    private boolean runOnStartup = true;

    @Valid
    private Api api = new Api();

    @Getter
    @Setter
    public static class Api {
        /** HTTP header that carries the API key. */
        @NotBlank
        private String keyHeader = "X-API-KEY";
        /** Required in API mode. Empty = the application refuses to start the web endpoints. */
        private String key;
        /** Maximum number of executions returned by the list endpoint. */
        @Min(1)
        private int maxListSize = 100;
    }

    @Getter
    @Setter
    public static class FileConfig {
        @NotNull
        private CrosFileType type;
        @NotBlank
        private String prefix;
        private boolean enabled = true;
    }
}
