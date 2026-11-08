package id.co.bca.crosbatch.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "cros_relasi_jaminan_fasilitas",
        indexes = @Index(name = "idx_cros_relasi_jaminan_fasilitas_periode", columnList = "periode"))
public class RelasiJaminanFasilitas extends BaseCrosEntity {

    private LocalDate tglData;
    private Long custLnCurrKey;
    private Long acctLnCurrKey;
    private Long commCurrKey;
    private Long collCurrKey;
}
