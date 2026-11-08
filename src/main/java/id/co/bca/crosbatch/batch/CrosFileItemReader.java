package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import id.co.bca.crosbatch.file.CrosLineMapper;
import id.co.bca.crosbatch.file.Row;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.core.io.FileSystemResource;

/**
 * Spring Batch {@link FlatFileItemReader} for one CROS file. The file path depends on the
 * {@code period} job parameter, so the delegate is created in {@link #beforeStep}.
 */
public class CrosFileItemReader implements ItemStreamReader<Row>, StepExecutionListener {

    private final CrosBatchProperties.FileConfig fileConfig;
    private final CrosFileLocator locator;
    private final CrosBatchProperties props;
    private FlatFileItemReader<Row> delegate;

    public CrosFileItemReader(CrosBatchProperties.FileConfig fileConfig, CrosFileLocator locator,
                              CrosBatchProperties props) {
        this.fileConfig = fileConfig;
        this.locator = locator;
        this.props = props;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String period = CrosFileLocator.period(stepExecution);
        CrosLineMapper lineMapper = new CrosLineMapper(props.getDelimiter(), props.getDatePattern(),
                props.getTwoDigitYearBase());
        delegate = new FlatFileItemReader<>(new FileSystemResource(locator.inputFile(fileConfig, period)), lineMapper);
        delegate.setName(fileConfig.getType() + "_reader");
        delegate.setEncoding(props.getCharset());
        delegate.setLinesToSkip(1);
        delegate.setSkippedLinesCallback(lineMapper); // header -> column names
        delegate.setStrict(true);
        // A rerun always reads the whole file again (see CrosJobConfig), so no read position is kept.
        delegate.setSaveState(false);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public Row read() throws Exception {
        return delegate.read();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        if (delegate != null) {
            delegate.close();
        }
    }
}
