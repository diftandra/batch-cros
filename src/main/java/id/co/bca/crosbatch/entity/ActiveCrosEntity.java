package id.co.bca.crosbatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Tables that keep one row per composite key across periods. A row is active when its key
 * appeared in the latest loaded file.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class ActiveCrosEntity extends BaseCrosEntity {

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    /**
     * Spring Batch job execution that last loaded this row. Rows not loaded by the current
     * execution are deactivated, which also works when the same period is loaded again.
     */
    @Column(name = "load_execution_id")
    private Long loadExecutionId;
}
