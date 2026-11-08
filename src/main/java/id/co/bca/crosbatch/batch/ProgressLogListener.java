package id.co.bca.crosbatch.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

/** Logs a progress line every {@code interval} written rows. */
@Slf4j
public class ProgressLogListener implements ItemWriteListener<Object>, StepExecutionListener {

    private final long interval;
    private String stepName;
    private long written;
    private long nextLog;

    public ProgressLogListener(long interval) {
        this.interval = interval;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepName = stepExecution.getStepName();
        written = 0;
        nextLog = interval;
    }

    @Override
    public void afterWrite(Chunk<?> items) {
        written += items.size();
        if (written >= nextLog) {
            log.info("{}: {} rows written", stepName, written);
            nextLog += interval;
        }
    }
}
