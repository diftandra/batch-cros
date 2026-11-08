package id.co.bca.crosbatch.api;

import id.co.bca.crosbatch.batch.CrosFileLocator;
import id.co.bca.crosbatch.batch.CrosJobConfig;
import id.co.bca.crosbatch.config.CrosBatchProperties;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Health component "crosBatch" in /actuator/health.
 * DOWN when the input folder cannot be read or the backup folder cannot be written, because
 * then no run can succeed. A failed last run does not make the service DOWN (it is shown as a
 * detail), so a monitoring tool does not restart the service because of bad input data.
 */
@Component
@ConditionalOnWebApplication
public class CrosBatchHealthIndicator extends AbstractHealthIndicator {

    private final CrosBatchProperties props;
    private final JobRepository jobRepository;

    public CrosBatchHealthIndicator(CrosBatchProperties props, JobRepository jobRepository) {
        super("CROS batch health check failed");
        this.props = props;
        this.jobRepository = jobRepository;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Path input = props.getInputDir().toAbsolutePath();
        Path backup = props.getBackupDir().toAbsolutePath();
        boolean inputOk = Files.isDirectory(input) && Files.isReadable(input);
        boolean backupOk = isWritableOrCreatable(backup);

        builder.status(inputOk && backupOk ? "UP" : "DOWN")
                .withDetail("inputDir", input + (inputOk ? "" : " (missing or not readable)"))
                .withDetail("backupDir", backup + (backupOk ? "" : " (not writable)"))
                .withDetail("runningExecutions",
                        jobRepository.findRunningJobExecutions(CrosJobConfig.JOB_NAME).stream()
                                .map(JobExecution::getId).sorted().toList());

        JobInstance lastInstance = jobRepository.getLastJobInstance(CrosJobConfig.JOB_NAME);
        JobExecution last = lastInstance == null ? null : jobRepository.getLastJobExecution(lastInstance);
        if (last != null) {
            builder.withDetail("lastExecution", java.util.Map.of(
                    "executionId", last.getId(),
                    "period", String.valueOf(last.getJobParameters().getString(CrosFileLocator.PERIOD_PARAM)),
                    "status", last.getStatus().name(),
                    "endTime", String.valueOf(last.getEndTime())));
        }
    }

    /** The folder is writable, or does not exist yet but its closest existing parent is writable. */
    private static boolean isWritableOrCreatable(Path dir) {
        Path p = dir;
        while (p != null && !Files.exists(p)) {
            p = p.getParent();
        }
        return p != null && Files.isDirectory(p) && Files.isWritable(p);
    }
}
