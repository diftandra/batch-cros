package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starts and inspects {@value CrosJobConfig#JOB_NAME}. Used by the command line runner and the
 * REST controller, so both follow the same rules.
 */
@Slf4j
@Service
public class CrosJobService {

    private final CrosBatchProperties props;
    private final CrosFileLocator locator;
    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final Job crosMonthlyJob;
    /** Executions started by this JVM; they cannot be stale while this JVM is alive. */
    private final Set<Long> startedHere = ConcurrentHashMap.newKeySet();

    public CrosJobService(CrosBatchProperties props, CrosFileLocator locator, JobOperator jobOperator,
                          JobRepository jobRepository, Job crosMonthlyJob) {
        this.props = props;
        this.locator = locator;
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
        this.crosMonthlyJob = crosMonthlyJob;
    }

    /**
     * Launches the job. In CLI mode this blocks until the job ends; in API mode the job runs on
     * the batch task executor and the returned execution is still STARTING.
     *
     * @param requestedPeriod period in {@code crosbatch.period-format}; null/blank = previous month
     * @param recoverStale    mark executions left running by a killed process as FAILED first
     * @throws IllegalArgumentException the period is invalid or the input folder is missing
     * @throws JobAlreadyRunningException another execution is still marked running
     */
    public synchronized JobExecution start(String requestedPeriod, boolean recoverStale) throws Exception {
        String period = resolvePeriod(requestedPeriod);
        if (!Files.isDirectory(props.getInputDir())) {
            throw new IllegalArgumentException("input directory does not exist: "
                    + props.getInputDir().toAbsolutePath());
        }

        Set<JobExecution> running = jobRepository.findRunningJobExecutions(CrosJobConfig.JOB_NAME);
        if (!running.isEmpty()) {
            if (!recoverStale) {
                throw new JobAlreadyRunningException("job execution(s) "
                        + running.stream().map(JobExecution::getId).sorted().toList()
                        + " still marked running. If the earlier run was killed, recover it first");
            }
            for (JobExecution stale : running) {
                recover(stale);
            }
        }

        log.info("==== CROS batch start: period={} input={} backup={} ====",
                period, props.getInputDir().toAbsolutePath(), props.getBackupDir().toAbsolutePath());
        JobParameters parameters = new JobParametersBuilder()
                .addString(CrosFileLocator.PERIOD_PARAM, period)
                .addLocalDateTime("run.time", LocalDateTime.now())
                .toJobParameters();
        JobExecution execution = jobOperator.start(crosMonthlyJob, parameters);
        startedHere.add(execution.getId());
        return execution;
    }

    /** Marks an execution left in a running state by a killed process as FAILED. */
    public synchronized JobExecution recover(long executionId) {
        return recover(find(executionId));
    }

    private JobExecution recover(JobExecution execution) {
        if (!execution.isRunning()) {
            throw new IllegalArgumentException("job execution " + execution.getId() + " is not running ("
                    + execution.getStatus() + "), nothing to recover");
        }
        if (startedHere.contains(execution.getId())) {
            throw new JobAlreadyRunningException("job execution " + execution.getId()
                    + " is running in this process, it is not stale");
        }
        log.warn("Marking stale job execution {} (started {}) as FAILED", execution.getId(), execution.getStartTime());
        return jobOperator.recover(execution);
    }

    public JobExecution find(long executionId) {
        JobExecution execution;
        try {
            execution = jobRepository.getJobExecution(executionId);
        } catch (EmptyResultDataAccessException e) {
            execution = null;
        }
        if (execution == null || !CrosJobConfig.JOB_NAME.equals(execution.getJobInstance().getJobName())) {
            throw new ExecutionNotFoundException(executionId);
        }
        return execution;
    }

    /** Latest executions, newest first. */
    public List<JobExecution> recent(int limit) {
        int size = Math.min(Math.max(limit, 1), props.getApi().getMaxListSize());
        return jobRepository.getJobInstances(CrosJobConfig.JOB_NAME, 0, size).stream()
                .flatMap(instance -> jobRepository.getJobExecutions(instance).stream())
                .sorted(Comparator.comparing(JobExecution::getId).reversed())
                .limit(size)
                .toList();
    }

    private String resolvePeriod(String requested) {
        if (requested == null || requested.isBlank()) {
            return locator.format(YearMonth.now().minusMonths(1));
        }
        try {
            return locator.format(locator.parse(requested.strip()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("period '" + requested + "' does not match format "
                    + props.getPeriodFormat());
        }
    }

    /** Another execution is still marked running. */
    public static class JobAlreadyRunningException extends RuntimeException {
        public JobAlreadyRunningException(String message) {
            super(message);
        }
    }

    public static class ExecutionNotFoundException extends RuntimeException {
        public ExecutionNotFoundException(long id) {
            super("job execution " + id + " not found");
        }
    }
}
