package id.co.bca.crosbatch.api;

import id.co.bca.crosbatch.batch.CrosFileLocator;
import id.co.bca.crosbatch.batch.JobSummaryListener;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** JSON view of a Spring Batch job execution. */
public record ExecutionResponse(
        long executionId,
        String period,
        String status,
        String exitCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<StepResponse> steps,
        List<String> failures) {

    public record StepResponse(
            String name,
            String status,
            String exitCode,
            long readCount,
            long writeCount,
            long filterCount,
            Map<String, Long> counters) {
    }

    public static ExecutionResponse from(JobExecution execution) {
        List<StepResponse> steps = execution.getStepExecutions().stream()
                .sorted(Comparator.comparing(StepExecution::getId))
                .map(s -> new StepResponse(s.getStepName(), s.getStatus().name(), s.getExitStatus().getExitCode(),
                        s.getReadCount(), s.getWriteCount(), s.getFilterCount(), JobSummaryListener.counterMap(s)))
                .toList();
        // Exceptions are not stored in the Spring Batch tables, only the exit description (stack trace).
        List<String> failures = execution.getStepExecutions().stream()
                .sorted(Comparator.comparing(StepExecution::getId))
                .map(s -> firstLine(s.getExitStatus().getExitDescription()))
                .filter(line -> !line.isEmpty())
                .toList();
        if (failures.isEmpty() && !firstLine(execution.getExitStatus().getExitDescription()).isEmpty()) {
            failures = List.of(firstLine(execution.getExitStatus().getExitDescription()));
        }
        return new ExecutionResponse(
                execution.getId(),
                execution.getJobParameters().getString(CrosFileLocator.PERIOD_PARAM),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                execution.getStartTime(),
                execution.getEndTime(),
                steps,
                failures);
    }

    private static String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int end = text.indexOf('\n');
        return (end < 0 ? text : text.substring(0, end)).strip();
    }
}
