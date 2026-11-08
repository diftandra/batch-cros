package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import id.co.bca.crosbatch.service.FileBackupService;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * Last step of every file: moves the loaded file to the backup folder. It runs only after the
 * database work of the file is committed, so a backed-up file is always fully loaded.
 */
public class BackupFileTasklet implements Tasklet {

    private final CrosBatchProperties.FileConfig fileConfig;
    private final CrosFileLocator locator;
    private final FileBackupService backupService;

    public BackupFileTasklet(CrosBatchProperties.FileConfig fileConfig, CrosFileLocator locator,
                             FileBackupService backupService) {
        this.fileConfig = fileConfig;
        this.locator = locator;
        this.backupService = backupService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String period = CrosFileLocator.period(contribution.getStepExecution());
        backupService.backup(locator.inputFile(fileConfig, period), period);
        return RepeatStatus.FINISHED;
    }
}
