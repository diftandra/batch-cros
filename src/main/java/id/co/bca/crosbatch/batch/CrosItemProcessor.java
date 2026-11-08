package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import id.co.bca.crosbatch.entity.ActiveCrosEntity;
import id.co.bca.crosbatch.entity.BaseCrosEntity;
import id.co.bca.crosbatch.file.FileDefinition;
import id.co.bca.crosbatch.file.FileParseException;
import id.co.bca.crosbatch.file.Row;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.time.LocalDate;

/**
 * Converts a {@link Row} into its entity and stamps the period. Blank lines are filtered
 * (returned as {@code null}, counted as filterCount).
 */
public class CrosItemProcessor<E extends BaseCrosEntity> implements ItemProcessor<Row, E>, StepExecutionListener {

    private final FileDefinition<E> definition;
    private final CrosBatchProperties.FileConfig fileConfig;
    private final CrosFileLocator locator;
    private LocalDate periode;
    private String fileName;

    public CrosItemProcessor(FileDefinition<E> definition, CrosBatchProperties.FileConfig fileConfig,
                             CrosFileLocator locator) {
        this.definition = definition;
        this.fileConfig = fileConfig;
        this.locator = locator;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String period = CrosFileLocator.period(stepExecution);
        this.periode = locator.periode(period);
        this.fileName = locator.fileName(fileConfig, period);
    }

    @Override
    public E process(Row row) {
        if (row.isBlank()) {
            return null;
        }
        E entity;
        try {
            entity = definition.mapper().apply(row);
        } catch (RuntimeException e) {
            throw new FileParseException(fileName, row.getLineNumber(), e.getMessage(), e);
        }
        entity.setPeriode(periode);
        if (entity instanceof ActiveCrosEntity active) {
            active.setActive(Boolean.TRUE);
        }
        return entity;
    }
}
