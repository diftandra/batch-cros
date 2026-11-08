package id.co.bca.crosbatch.service;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Moves processed files to {@code <backup-dir>/<period>/<name>_<timestamp>.<ext>}.
 */
@Slf4j
@Service
public class FileBackupService {

    private final Path backupDir;
    private final DateTimeFormatter timestampFormat;

    public FileBackupService(CrosBatchProperties props) {
        this.backupDir = props.getBackupDir();
        this.timestampFormat = DateTimeFormatter.ofPattern(props.getBackupTimestampPattern());
    }

    public Path backup(Path file, String period) throws IOException {
        Path targetDir = backupDir.resolve(period);
        Files.createDirectories(targetDir);
        String name = file.getFileName().toString();
        Path target = targetDir.resolve(baseName(name) + "_" + LocalDateTime.now().format(timestampFormat)
                + extension(name));
        try {
            Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Backup dir is on another file system: fall back to copy + delete.
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Backed up {} -> {}", file, target);
        return target;
    }

    /** True when a backup of this file name exists for the period, i.e. an earlier run loaded it. */
    public boolean hasBackup(String fileName, String period) throws IOException {
        Path dir = backupDir.resolve(period);
        if (!Files.isDirectory(dir)) {
            return false;
        }
        String prefix = baseName(fileName) + "_";
        String ext = extension(fileName);
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                    .anyMatch(n -> n.startsWith(prefix) && n.endsWith(ext));
        }
    }

    private static String baseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }
}
