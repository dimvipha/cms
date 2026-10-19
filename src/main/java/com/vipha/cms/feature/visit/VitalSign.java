package com.vipha.cms.feature.visit;

import com.vipha.cms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "vital_sign")
@Getter
@Setter
public class VitalSign extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @Column(nullable = false)
    private Instant recordedAt;

    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer pulseRate;
    private Integer respiratoryRate;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperatureCelsius;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(precision = 5, scale = 2)
    private BigDecimal heightCm;

    private Integer oxygenSaturation;

    /** kg/m², one decimal; null unless both weight and height were recorded. */
    public BigDecimal getBmi() {
        if (weightKg == null || heightCm == null || heightCm.signum() <= 0) {
            return null;
        }
        BigDecimal heightM = heightCm.movePointLeft(2);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }
}
