package id.co.bca.crosbatch;

import id.co.bca.crosbatch.batch.CrosJobService;
import id.co.bca.crosbatch.config.CrosBatchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Monthly CROS batch.
 *
 * <ul>
 *   <li><b>CLI mode</b> (default): runs the Spring Batch job once for the period
 *       ({@code --crosbatch.period}, or previous month) and exits with 0 (COMPLETED) or 1.</li>
 *   <li><b>API mode</b> ({@code --spring.profiles.active=api}): starts a web server and waits for
 *       REST calls, see {@code CrosBatchController}.</li>
 * </ul>
 *
 * Every launch is a new job instance. Files already loaded were moved to backup and are skipped;
 * files still in the input folder are loaded again from the first line, so running the same
 * period again after a failure only redoes what is left.
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(CrosBatchProperties.class)
public class CrosbatchApplication implements CommandLineRunner, ExitCodeGenerator {

    private final CrosBatchProperties props;
    private final CrosJobService jobService;
    private int exitCode = 1;

    public CrosbatchApplication(CrosBatchProperties props, CrosJobService jobService) {
        this.props = props;
        this.jobService = jobService;
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CrosbatchApplication.class, args);
        if (context.getBean(CrosBatchProperties.class).isRunOnStartup()) {
            System.exit(SpringApplication.exit(context));
        }
        // API mode: keep running until the process is stopped.
    }

    @Override
    public void run(String... args) throws Exception {
        if (!props.isRunOnStartup()) {
            log.info("API mode: waiting for requests on /api/cros-batch");
            return;
        }
        // Blocks until the job ends; the summary is logged by JobSummaryListener.
        JobExecution execution = jobService.start(props.getPeriod(), props.isRecoverStaleExecutions());
        exitCode = execution.getStatus() == BatchStatus.COMPLETED ? 0 : 1;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
