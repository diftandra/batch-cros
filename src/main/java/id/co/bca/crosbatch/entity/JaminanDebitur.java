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
@Table(name = "cros_jaminan_debitur", indexes = {
        @Index(name = "idx_cros_jaminan_debitur_bkey", columnList = "cust_ln_curr_key, coll_curr_key"),
        @Index(name = "idx_cros_jaminan_debitur_active", columnList = "is_active, periode")
})
public class JaminanDebitur extends ActiveCrosEntity {

    private LocalDate tglData;
    private Long custLnCurrKey;
    private Long collCurrKey;
    private Long collCurrencyKey;
    private String noCollateral;
    @Column(columnDefinition = "text")
    private String tipeJaminan;
    private String mataUang;
    @Column(columnDefinition = "text")
    private String deskripsiJaminan;
    @Column(columnDefinition = "numeric")
    private BigDecimal nilaiPasarOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal nilaiPasarIdr;
    @Column(columnDefinition = "numeric")
    private BigDecimal nilaiTaksasiOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal nilaiTaksasiIdr;
    @Column(columnDefinition = "numeric")
    private BigDecimal nilaiHakTanggunganOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal nilaiHakTanggunganIdr;
    @Column(columnDefinition = "numeric")
    private BigDecimal nilaiLikuidasiOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal nilaiLikuidasiIdr;
    private String solidNonSolid;
    private LocalDate tglBap;
}
