package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/** Resolves the period job parameter into file names and the {@code periode} column value. */
@Component
public class CrosFileLocator {

    /** Identifying job parameter holding the period, e.g. "112025". */
    public static final String PERIOD_PARAM = "period";

    private final CrosBatchProperties props;
    private final DateTimeFormatter periodFormat;

    public CrosFileLocator(CrosBatchProperties props) {
        this.props = props;
        this.periodFormat = DateTimeFormatter.ofPattern(props.getPeriodFormat());
    }

    public static String period(StepExecution stepExecution) {
        String period = stepExecution.getJobExecution().getJobParameters().getString(PERIOD_PARAM);
        if (period == null) {
            throw new IllegalStateException("job parameter '" + PERIOD_PARAM + "' is missing");
        }
        return period;
    }

    /** True when the file name pattern has {period}, e.g. "{prefix}_{period}.txt". */
    public boolean fileNamesContainPeriod() {
        return props.getFileNamePattern().contains("{period}");
    }

    public String fileName(CrosBatchProperties.FileConfig file, String period) {
        return props.getFileNamePattern()
                .replace("{prefix}", file.getPrefix())
                .replace("{period}", period);
    }

    public Path inputFile(CrosBatchProperties.FileConfig file, String period) {
        return props.getInputDir().resolve(fileName(file, period));
    }

    /** First day of the period month. */
    public LocalDate periode(String period) {
        return YearMonth.parse(period, periodFormat).atDay(1);
    }

    public YearMonth parse(String period) {
        return YearMonth.parse(period, periodFormat);
    }

    public String format(YearMonth yearMonth) {
        return yearMonth.format(periodFormat);
    }
}
