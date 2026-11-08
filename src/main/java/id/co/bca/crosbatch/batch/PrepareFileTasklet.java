package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import id.co.bca.crosbatch.file.FileDefinition;
import id.co.bca.crosbatch.service.FileBackupService;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * First step of every file.
 * <ul>
 *   <li>File not in input but in backup: an earlier run loaded it -> exit status {@value #SKIPPED}.</li>
 *   <li>File in neither place: fail the job.</li>
 *   <li>SNAPSHOT files: delete the rows of this period left by an earlier run.</li>
 * </ul>
 */
@Slf4j
public class PrepareFileTasklet implements Tasklet {

    public static final String SKIPPED = "SKIPPED";
    static final String DELETED = "deleted";

    private final CrosBatchProperties.FileConfig fileConfig;
    private final FileDefinition<?> definition;
    private final CrosFileLocator locator;
    private final FileBackupService backupService;
    private final EntityManager em;

    public PrepareFileTasklet(CrosBatchProperties.FileConfig fileConfig, FileDefinition<?> definition,
                              CrosFileLocator locator, FileBackupService backupService, EntityManager em) {
        this.fileConfig = fileConfig;
        this.definition = definition;
        this.locator = locator;
        this.backupService = backupService;
        this.em = em;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        StepExecution stepExecution = contribution.getStepExecution();
        String period = CrosFileLocator.period(stepExecution);
        String fileName = locator.fileName(fileConfig, period);
        Path file = locator.inputFile(fileConfig, period);

        if (!Files.isRegularFile(file)) {
            if (backupService.hasBackup(fileName, period)) {
                log.info("{}: not in input but found in backup, already processed -> skipped", fileName);
                contribution.setExitStatus(new ExitStatus(SKIPPED, "already processed"));
                return RepeatStatus.FINISHED;
            }
            throw new IllegalStateException(fileName + " not found in " + file.getParent().toAbsolutePath());
        }

        log.info("{}: found, mode {}", fileName, definition.mode());
        if (definition.mode() == FileDefinition.LoadMode.SNAPSHOT) {
            String entity = em.getMetamodel().entity(definition.entityClass()).getName();
            int deleted = em.createQuery("delete from " + entity + " e where e.periode = :periode")
                    .setParameter("periode", locator.periode(period))
                    .executeUpdate();
            stepExecution.getExecutionContext().putLong(DELETED, deleted);
            if (deleted > 0) {
                log.info("{}: removed {} rows of period {} from an earlier run", fileName, deleted, period);
            }
        }
        return RepeatStatus.FINISHED;
    }
}
