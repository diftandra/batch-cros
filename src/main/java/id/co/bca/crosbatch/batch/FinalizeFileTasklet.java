package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import id.co.bca.crosbatch.file.FileDefinition;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Runs after the load step.
 * <ul>
 *   <li>Fails when the file had no data rows (unless allowed), so an empty UPSERT file cannot
 *       deactivate the whole table.</li>
 *   <li>UPSERT files: every active row not loaded by this job execution (its key was not in
 *       this file) is set to is_active = false.</li>
 * </ul>
 */
@Slf4j
public class FinalizeFileTasklet implements Tasklet {

    static final String DEACTIVATED = "deactivated";

    private final CrosBatchProperties.FileConfig fileConfig;
    private final FileDefinition<?> definition;
    private final CrosFileLocator locator;
    private final EntityManager em;
    private final boolean allowEmptyFile;

    public FinalizeFileTasklet(CrosBatchProperties.FileConfig fileConfig, FileDefinition<?> definition,
                               CrosFileLocator locator, EntityManager em, boolean allowEmptyFile) {
        this.fileConfig = fileConfig;
        this.definition = definition;
        this.locator = locator;
        this.em = em;
        this.allowEmptyFile = allowEmptyFile;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        StepExecution stepExecution = contribution.getStepExecution();
        String period = CrosFileLocator.period(stepExecution);
        LocalDate periode = locator.periode(period);
        String fileName = locator.fileName(fileConfig, period);
        String entity = em.getMetamodel().entity(definition.entityClass()).getName();

        long rows = em.createQuery("select count(e) from " + entity + " e where e.periode = :periode", Long.class)
                .setParameter("periode", periode)
                .getSingleResult();
        if (rows == 0 && !allowEmptyFile) {
            throw new IllegalStateException(fileName
                    + " has no data rows (set crosbatch.allow-empty-file=true to accept it)");
        }

        if (definition.mode() == FileDefinition.LoadMode.UPSERT) {
            // Every active row not loaded by this execution: its key was not in this file.
            int deactivated = em.createQuery("update " + entity + " e set e.active = false, e.updatedAt = :now"
                            + " where e.active = true"
                            + " and (e.loadExecutionId is null or e.loadExecutionId <> :executionId)")
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("executionId", stepExecution.getJobExecutionId())
                    .executeUpdate();
            stepExecution.getExecutionContext().putLong(DEACTIVATED, deactivated);
            log.info("{}: {} rows of period {} in table, {} rows set inactive", fileName, rows, period, deactivated);
        } else {
            log.info("{}: {} rows of period {} in table", fileName, rows, period);
        }
        return RepeatStatus.FINISHED;
    }
}
