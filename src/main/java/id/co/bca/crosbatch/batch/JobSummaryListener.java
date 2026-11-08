package id.co.bca.crosbatch.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Logs a per-step summary when the job ends, whether it was started from the CLI or the API. */
@Slf4j
public class JobSummaryListener implements JobExecutionListener {

    @Override
    public void afterJob(JobExecution execution) {
        log.info("==== CROS batch summary: period={} jobExecutionId={} ====",
                execution.getJobParameters().getString(CrosFileLocator.PERIOD_PARAM), execution.getId());
        execution.getStepExecutions().stream()
                .sorted(Comparator.comparing(StepExecution::getId))
                .forEach(step -> log.info("  {} {} {}",
                        String.format("%-36s", step.getStepName()),
                        String.format("%-10s", step.getExitStatus().getExitCode()),
                        (step.getStepName().endsWith(".load")
                                ? "read=" + step.getReadCount() + " written=" + step.getWriteCount()
                                + " filtered=" + step.getFilterCount() + " "
                                : "") + counters(step)));
        execution.getAllFailureExceptions().forEach(e -> log.error("  failure: {}", rootMessage(e)));
        Duration duration = execution.getStartTime() != null
                ? Duration.between(execution.getStartTime(), java.time.LocalDateTime.now())
                : Duration.ZERO;
        log.info("==== CROS batch {} in {}s ====", execution.getStatus(), duration.toSeconds());
    }

    /** Numeric counters the steps put in their execution context (inserted, updated, deleted, ...). */
    public static Map<String, Long> counterMap(StepExecution step) {
        Map<String, Long> counters = new LinkedHashMap<>();
        ExecutionContext context = step.getExecutionContext();
        context.entrySet().stream()
                .filter(e -> e.getValue() instanceof Number)
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> counters.put(e.getKey(), ((Number) e.getValue()).longValue()));
        return counters;
    }

    private static String counters(StepExecution step) {
        StringBuilder sb = new StringBuilder();
        counterMap(step).forEach((k, v) -> sb.append(k).append('=').append(v).append(' '));
        return sb.toString().trim();
    }

    public static String rootMessage(Throwable e) {
        Throwable t = e;
        StringBuilder sb = new StringBuilder(String.valueOf(e.getMessage()));
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
            if (t.getMessage() != null && !sb.toString().contains(t.getMessage())) {
                sb.append(" <- ").append(t.getMessage());
            }
        }
        return sb.toString();
    }
}
