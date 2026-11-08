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
@Table(name = "cros_npl", indexes = @Index(name = "idx_cros_npl_periode", columnList = "periode"))
public class Npl extends BaseCrosEntity {

    private Long custLnCurrKey;
    private Long acctLnCurrKey;
    private Long commCurrKey;
    private Long productSubTypeKey;
    private Long facLnSegKey;
    private Long collectKey;
    private Long currKey;
    private Long acctRegionKey;
    private Long acctKcuKey;
    private String noRekeningPinjaman;
    private String noKomitmen;
    @Column(columnDefinition = "text")
    private String tipeProduk;
    private String kodeProduk;
    @Column(columnDefinition = "text")
    private String namaProduk;

    private LocalDate nplDate;
    @Column(columnDefinition = "numeric")
    private BigDecimal nplBalanceOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal nplBalanceIdr;
    @Column(columnDefinition = "numeric")
    private BigDecimal nplNotesIntReceivablesOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal nplNotesIntReceivablesIdr;
    @Column(columnDefinition = "numeric")
    private BigDecimal nplCurrAssessedOcur;
    @Column(columnDefinition = "numeric")
    private BigDecimal nplCurrAssessedIdr;

    // Explicit names: the default naming strategy does not put "_" between a digit and a letter.
    @Column(name = "col_3_date")
    private LocalDate col3Date;
    @Column(name = "col_3_balance_ocur", columnDefinition = "numeric")
    private BigDecimal col3BalanceOcur;
    @Column(name = "col_3_balance_idr", columnDefinition = "numeric")
    private BigDecimal col3BalanceIdr;
    @Column(name = "col_3_notes_int_receivables_ocur", columnDefinition = "numeric")
    private BigDecimal col3NotesIntReceivablesOcur;
    @Column(name = "col_3_notes_int_receivables_idr", columnDefinition = "numeric")
    private BigDecimal col3NotesIntReceivablesIdr;
    @Column(name = "col_3_curr_assessed_ocur", columnDefinition = "numeric")
    private BigDecimal col3CurrAssessedOcur;
    @Column(name = "col_3_curr_assessed_idr", columnDefinition = "numeric")
    private BigDecimal col3CurrAssessedIdr;

    @Column(name = "col_4_date")
    private LocalDate col4Date;
    @Column(name = "col_4_balance_ocur", columnDefinition = "numeric")
    private BigDecimal col4BalanceOcur;
    @Column(name = "col_4_balance_idr", columnDefinition = "numeric")
    private BigDecimal col4BalanceIdr;
    @Column(name = "col_4_notes_int_receivables_ocur", columnDefinition = "numeric")
    private BigDecimal col4NotesIntReceivablesOcur;
    @Column(name = "col_4_notes_int_receivables_idr", columnDefinition = "numeric")
    private BigDecimal col4NotesIntReceivablesIdr;
    @Column(name = "col_4_curr_assessed_ocur", columnDefinition = "numeric")
    private BigDecimal col4CurrAssessedOcur;
    @Column(name = "col_4_curr_assessed_idr", columnDefinition = "numeric")
    private BigDecimal col4CurrAssessedIdr;

    @Column(name = "col_5_date")
    private LocalDate col5Date;
    @Column(name = "col_5_balance_ocur", columnDefinition = "numeric")
    private BigDecimal col5BalanceOcur;
    @Column(name = "col_5_balance_idr", columnDefinition = "numeric")
    private BigDecimal col5BalanceIdr;
    @Column(name = "col_5_notes_int_receivables_ocur", columnDefinition = "numeric")
    private BigDecimal col5NotesIntReceivablesOcur;
    @Column(name = "col_5_notes_int_receivables_idr", columnDefinition = "numeric")
    private BigDecimal col5NotesIntReceivablesIdr;
    @Column(name = "col_5_curr_assessed_ocur", columnDefinition = "numeric")
    private BigDecimal col5CurrAssessedOcur;
    @Column(name = "col_5_curr_assessed_idr", columnDefinition = "numeric")
    private BigDecimal col5CurrAssessedIdr;
}
