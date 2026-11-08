package id.co.bca.crosbatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "cros_region_dim", indexes = @Index(name = "idx_cros_region_dim_periode", columnList = "periode"))
public class RegionDim extends BaseCrosEntity {

    private String regionCdNew;
    private Long regionKey;
    private String regionCd;
    private String regionShrtNm;
    @Column(columnDefinition = "text")
    private String regionLngNm;
    private LocalDate strtEffDt;
    private LocalDate strtEndDt;
    private LocalDate lastUpdateDt;
    private LocalDate businessLoadDt;
    private String sourceSystem;
}
