package id.co.bca.crosbatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "cros_pemegang_saham",
        indexes = @Index(name = "idx_cros_pemegang_saham_periode", columnList = "periode"))
public class PemegangSaham extends BaseCrosEntity {

    private LocalDate tglData;
    private Long custLnCurrKey;
    @Column(columnDefinition = "text")
    private String namaPemegangSaham;
    @Column(columnDefinition = "text")
    private String pengurusPemilikSahamInd;
    @Column(columnDefinition = "numeric")
    private BigDecimal ownerShare;
}
