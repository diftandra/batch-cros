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
@Table(name = "cros_master_debitur", indexes = {
        @Index(name = "idx_cros_master_debitur_bkey",
                columnList = "cust_ln_curr_key, kategori_debitur, kode_kanwil, kode_kcu, kategori_kredit"),
        @Index(name = "idx_cros_master_debitur_active", columnList = "is_active, periode")
})
public class MasterDebitur extends ActiveCrosEntity {

    private LocalDate tglData;
    private Long custLnCurrKey;
    private Long collectKey;
    private Long acctRegionKey;
    private Long acctKcuKey;
    private String cin;
    @Column(columnDefinition = "text")
    private String namaDebitur;
    private LocalDate tglMenjadiNasabah;
    private LocalDate tglMenjadiDebitur;
    private String noGrup;
    @Column(columnDefinition = "text")
    private String namaGrup;
    @Column(columnDefinition = "text")
    private String sektorEkonomi;
    private String kategoriDebitur;
    private String kodeKanwil;
    private String kodeKcu;
    private String kategoriKredit;
    private Integer flagHb;
    @Column(columnDefinition = "numeric")
    private BigDecimal balanceIdr;
    private Integer hariTunggakan;
}
