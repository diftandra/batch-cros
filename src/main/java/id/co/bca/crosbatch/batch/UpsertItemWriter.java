package id.co.bca.crosbatch.batch;

import id.co.bca.crosbatch.entity.ActiveCrosEntity;
import id.co.bca.crosbatch.file.FileDefinition;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Writer for tables with a composite key (MASTER_DEBITUR, MASTER_PINJAMAN, MASTER_KOMITMEN,
 * JAMINAN_DEBITUR). Key found in the table: the row is updated with the new values and period.
 * Key not found: a new row is inserted. Rows not in the file are deactivated later by
 * {@link FinalizeFileTasklet}.
 */
@Slf4j
public class UpsertItemWriter<E extends ActiveCrosEntity> implements ItemWriter<E>, StepExecutionListener {

    static final String INSERTED = "inserted";
    static final String UPDATED = "updated";
    static final String DUPLICATE_KEYS = "duplicateKeysInFile";

    private final FileDefinition<E> definition;
    private final EntityManager em;
    private final String entityName;

    private Map<List<Object>, UUID> keyToId;
    private Set<List<Object>> seenInFile;
    private long inserted;
    private long updated;
    private long duplicates;

    public UpsertItemWriter(FileDefinition<E> definition, EntityManager em) {
        this.definition = definition;
        this.em = em;
        this.entityName = em.getMetamodel().entity(definition.entityClass()).getName();
    }

    /** Loads composite key -> id of every row already in the table. */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        keyToId = new HashMap<>();
        seenInFile = new HashSet<>();
        inserted = updated = duplicates = 0;
        for (E e : em.createQuery("select e from " + entityName + " e", definition.entityClass()).getResultList()) {
            List<Object> key = definition.businessKey().apply(e);
            if (keyToId.putIfAbsent(key, e.getId()) != null) {
                log.warn("{}: duplicate key {} already in table, only the first row is updated", entityName, key);
            }
        }
        log.info("{}: {} existing keys loaded for matching", entityName, keyToId.size());
    }

    @Override
    public void write(Chunk<? extends E> chunk) {
        // Load the rows this chunk will update with one query, so merge() does not select one by one.
        List<UUID> ids = new ArrayList<>();
        for (E item : chunk) {
            UUID id = keyToId.get(definition.businessKey().apply(item));
            if (id != null) {
                ids.add(id);
            }
        }
        if (!ids.isEmpty()) {
            em.createQuery("select e from " + entityName + " e where e.id in :ids", definition.entityClass())
                    .setParameter("ids", ids)
                    .getResultList();
        }

        for (E item : chunk) {
            List<Object> key = definition.businessKey().apply(item);
            if (!seenInFile.add(key)) {
                duplicates++;
            }
            UUID id = keyToId.get(key);
            if (id == null) {
                em.persist(item);
                keyToId.put(key, item.getId());
                inserted++;
            } else {
                E current = em.find(definition.entityClass(), id);
                if (current == null) {
                    throw new IllegalStateException(entityName + " row " + id + " disappeared during the load");
                }
                item.setId(id);
                item.setCreatedAt(current.getCreatedAt());
                em.merge(item); // copies all values onto the managed row
                updated++;
            }
        }
        em.flush();
        em.clear();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        stepExecution.getExecutionContext().putLong(INSERTED, inserted);
        stepExecution.getExecutionContext().putLong(UPDATED, updated);
        stepExecution.getExecutionContext().putLong(DUPLICATE_KEYS, duplicates);
        if (duplicates > 0) {
            log.warn("{}: {} rows used a composite key that already appeared earlier in the same file;"
                    + " the last row was kept", entityName, duplicates);
        }
        keyToId = null;
        seenInFile = null;
        return null;
    }
}
