package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import id.co.bca.crosbatch.entity.ActiveCrosEntity;
import id.co.bca.crosbatch.entity.BaseCrosEntity;
import id.co.bca.crosbatch.file.CrosFileDefinitions;
import id.co.bca.crosbatch.file.FileDefinition;
import id.co.bca.crosbatch.file.Row;
import id.co.bca.crosbatch.service.FileBackupService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Spring Batch job {@value #JOB_NAME}: one flow per configured file, run in the configured order.
 *
 * <pre>
 * &lt;TYPE&gt;.prepare --SKIPPED--> (next file)
 *        |COMPLETED
 * &lt;TYPE&gt;.load      chunk step: FlatFileItemReader -> CrosItemProcessor -> JPA writer
 * &lt;TYPE&gt;.finalize  empty-file check, is_active = false for keys not in the file (UPSERT)
 * &lt;TYPE&gt;.backup    move file to backup
 * </pre>
 *
 * Any failure stops the job (status FAILED). Every step is idempotent and allowed to run again,
 * so the next run (new job instance or restart) re-processes every file still in the input
 * folder from the first line, and skips files already moved to backup.
 */
@Configuration
public class CrosJobConfig {

    public static final String JOB_NAME = "crosMonthlyJob";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final EntityManager entityManager;
    private final CrosBatchProperties props;
    private final CrosFileDefinitions definitions;
    private final CrosFileLocator locator;
    private final FileBackupService backupService;

    public CrosJobConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                         EntityManagerFactory entityManagerFactory, EntityManager entityManager,
                         CrosBatchProperties props, CrosFileDefinitions definitions, CrosFileLocator locator,
                         FileBackupService backupService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.entityManager = entityManager;
        this.props = props;
        this.definitions = definitions;
        this.locator = locator;
        this.backupService = backupService;
    }

    @Bean
    public Job crosMonthlyJob() {
        List<Flow> flows = props.getFiles().stream()
                .filter(CrosBatchProperties.FileConfig::isEnabled)
                .map(this::fileFlow)
                .toList();
        if (flows.isEmpty()) {
            throw new IllegalStateException("no file enabled in crosbatch.files");
        }
        JobFlowBuilder builder = new JobBuilder(JOB_NAME, jobRepository).start(flows.get(0));
        for (Flow flow : flows.subList(1, flows.size())) {
            builder.next(flow);
        }
        return builder.end().listener(new JobSummaryListener()).build();
    }

    private Flow fileFlow(CrosBatchProperties.FileConfig fileConfig) {
        return fileFlow(fileConfig, definitions.get(fileConfig.getType()));
    }

    private <E extends BaseCrosEntity> Flow fileFlow(CrosBatchProperties.FileConfig fileConfig,
                                                     FileDefinition<E> definition) {
        String name = fileConfig.getType().name();

        Step prepare = new StepBuilder(name + ".prepare", jobRepository)
                .tasklet(new PrepareFileTasklet(fileConfig, definition, locator, backupService, entityManager),
                        transactionManager)
                .allowStartIfComplete(true)
                .build();

        Step load = new StepBuilder(name + ".load", jobRepository)
                .<Row, E>chunk(props.getBatchSize())
                .transactionManager(transactionManager)
                .reader(new CrosFileItemReader(fileConfig, locator, props))
                .processor(new CrosItemProcessor<>(definition, fileConfig, locator))
                .writer(writer(definition))
                .listener(new ProgressLogListener(props.getProgressLogInterval()))
                .allowStartIfComplete(true)
                .build();

        Step finalizeStep = new StepBuilder(name + ".finalize", jobRepository)
                .tasklet(new FinalizeFileTasklet(fileConfig, definition, locator, entityManager,
                        props.isAllowEmptyFile()), transactionManager)
                .allowStartIfComplete(true)
                .build();

        Step backup = new StepBuilder(name + ".backup", jobRepository)
                .tasklet(new BackupFileTasklet(fileConfig, locator, backupService), transactionManager)
                .allowStartIfComplete(true)
                .build();

        return new FlowBuilder<SimpleFlow>(name + ".flow")
                .start(prepare)
                .on(PrepareFileTasklet.SKIPPED).end()
                .from(prepare).on(ExitStatus.COMPLETED.getExitCode()).to(load)
                .from(prepare).on("*").fail()
                .from(load).on(ExitStatus.COMPLETED.getExitCode()).to(finalizeStep)
                .from(load).on("*").fail()
                .from(finalizeStep).on(ExitStatus.COMPLETED.getExitCode()).to(backup)
                .from(finalizeStep).on("*").fail()
                .from(backup).on(ExitStatus.COMPLETED.getExitCode()).end()
                .from(backup).on("*").fail()
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E extends BaseCrosEntity> ItemWriter<E> writer(FileDefinition<E> definition) {
        if (definition.mode() == FileDefinition.LoadMode.UPSERT) {
            return new UpsertItemWriter((FileDefinition<? extends ActiveCrosEntity>) definition, entityManager);
        }
        JpaItemWriter<E> writer = new JpaItemWriter<>(entityManagerFactory);
        writer.setUsePersist(true); // new rows only; persist avoids a select per row
        return writer;
    }
}
