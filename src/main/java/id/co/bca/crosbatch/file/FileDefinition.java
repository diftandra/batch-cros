package id.co.bca.crosbatch.file;

import id.co.bca.crosbatch.entity.ActiveCrosEntity;
import id.co.bca.crosbatch.entity.BaseCrosEntity;

import java.util.List;
import java.util.function.Function;

/**
 * How one file type is loaded.
 *
 * @param entityClass  target JPA entity
 * @param mapper       converts a file row into a new entity
 * @param businessKey  composite key for {@link LoadMode#UPSERT}; {@code null} for {@link LoadMode#SNAPSHOT}
 */
public record FileDefinition<E extends BaseCrosEntity>(
        Class<E> entityClass,
        Function<Row, E> mapper,
        LoadMode mode,
        Function<E, List<Object>> businessKey) {

    public enum LoadMode {
        /**
         * Match on the composite key: update the existing row, otherwise insert. Rows whose key is
         * not in the current file are set to is_active = false.
         */
        UPSERT,
        /** Keep one full copy per period: delete the period's rows, then insert the file. */
        SNAPSHOT
    }

    public static <E extends ActiveCrosEntity> FileDefinition<E> upsert(
            Class<E> entityClass, Function<Row, E> mapper, Function<E, List<Object>> businessKey) {
        return new FileDefinition<>(entityClass, mapper, LoadMode.UPSERT, businessKey);
    }

    public static <E extends BaseCrosEntity> FileDefinition<E> snapshot(Class<E> entityClass, Function<Row, E> mapper) {
        return new FileDefinition<>(entityClass, mapper, LoadMode.SNAPSHOT, null);
    }
}
