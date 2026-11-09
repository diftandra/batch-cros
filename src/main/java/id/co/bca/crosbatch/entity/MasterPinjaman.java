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
@Table(name = "cros_master_pinjaman", indexes = {
        @Index(name = "idx_cros_master_pinjaman_bkey",
                columnList = "cust_ln_curr_key, kode_kanwil, kode_kcu, no_rekening_pinjaman, no_pinjaman"),
        @Index(name = "idx_cros_master_pinjaman_active", columnList = "is_active, periode")
})
public class MasterPinjaman extends ActiveCrosEntity {

    private LocalDate tglData;
    private Long custLnCurrKey;
    private Long acctLnCurrKey;
    private Long commCurrKey;
    private Long lnCurrKey;
    private Long productSubTypeKey;
    private Long facLnSegKey;
    private Long collectKey;
    private Long acctRegionKey;
    private Long acctKcuKey;
    private String kodeKanwil;
    private String kodeKcu;
    private String noRekeningPinjaman;
    private String noPinjaman;
    private LocalDate tglBukaPinjaman;
    private LocalDate tglTutupPinjaman;
    private String kategoriKredit;
    @Column(columnDefinition = "text")
    private String tipeProduk;
    private String kodeProduk;
    @Column(columnDefinition = "text")
    private String namaProduk;
    private String mataUang;
    @Column(columnDefinition = "numeric")
    private BigDecimal balanceOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal balanceIdr;
    @Column(columnDefinition = "numeric")
    private BigDecimal bungaOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal bungaIdr;
    @Column(columnDefinition = "numeric")
    private BigDecimal dendaOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal dendaIdr;
    @Column(columnDefinition = "numeric")
    private BigDecimal balanceOcurHb;
    @Column(columnDefinition = "numeric")
    private BigDecimal balanceIdrHb;
    @Column(columnDefinition = "numeric")
    private BigDecimal bungaOcurHb;
    @Column(columnDefinition = "numeric")
    private BigDecimal bungaIdrHb;
    @Column(columnDefinition = "numeric")
    private BigDecimal dendaOcurHb;
    @Column(columnDefinition = "numeric")
    private BigDecimal dendaIdrHb;
    private LocalDate tglHb;
    @Column(columnDefinition = "numeric")
    private BigDecimal balanceOcurHt;
    @Column(columnDefinition = "numeric")
    private BigDecimal balanceIdrHt;
    @Column(columnDefinition = "numeric")
    private BigDecimal bungaOcurHt;
    @Column(columnDefinition = "numeric")
    private BigDecimal bungaIdrHt;
    @Column(columnDefinition = "numeric")
    private BigDecimal dendaOcurHt;
    @Column(columnDefinition = "numeric")
    private BigDecimal dendaIdrHt;
    private LocalDate tglHt;
    private Integer hariTunggakan;
    private Integer isRestrukturisasi;
    private String kodeRestrukturisasi;
    private Integer flagHb;
}
