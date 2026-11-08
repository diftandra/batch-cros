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
@Table(name = "cros_branch_dim", indexes = @Index(name = "idx_cros_branch_dim_periode", columnList = "periode"))
public class BranchDim extends BaseCrosEntity {

    private Long branchKey;
    private String branchCd;
    private String branchShrtNm;
    @Column(columnDefinition = "text")
    private String branchName;
    @Column(columnDefinition = "text")
    private String branchAddress1;
    @Column(columnDefinition = "text")
    private String branchAddress2;
    private String jaboNonjabotabek;
    private String kcuCd;
    private Long cityKey;
    private Long regionKey;
    private LocalDate lastUpdateDt;
    private LocalDate businessLoadDt;
    private LocalDate strtEffDt;
    private LocalDate strtEndDt;
    private String sourceSystem;
    @Column(columnDefinition = "text")
    private String phone;
    @Column(columnDefinition = "text")
    private String contactPerson;
    private String regionBatch;
    @Column(columnDefinition = "text")
    private String groupCity;
    private LocalDate branchClosedDt;
    private String ckkCd;
    private String referralCd;
    private String ruangPrioritasInd;
    private Long regionKeyDkk;
    private String timezone;
    @Column(columnDefinition = "text")
    private String geography;
    @Column(columnDefinition = "text")
    private String centralKfkk;
    private String flagBigCity;
    private String ccJaboInd;
    private String ccRegionCd;
    private String centerId;
    private String depotId;
    private LocalDate actvDt;
    private String status;
    private String tripCd;
    private String citVendor;
    private String glRegion;
    private String soaCd;
    private String classId;
    private String slaProfileId;
    @Column(columnDefinition = "numeric")
    private BigDecimal latitude;
    @Column(columnDefinition = "numeric")
    private BigDecimal longitude;
    @Column(columnDefinition = "text")
    private String fax;
    @Column(columnDefinition = "text")
    private String email;
    private String dati2Cd;
    private String conBranchCd;
}
