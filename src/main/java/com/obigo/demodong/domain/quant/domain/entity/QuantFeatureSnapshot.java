package com.obigo.demodong.domain.quant.domain.entity;

import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Quant Score 산출 시점의 Feature 값 스냅샷.
 * Phase 11 Self-Correction 가중치 조정 및 디버깅에 활용.
 */
@Entity
@Table(name = "quant_feature_snapshot")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QuantFeatureSnapshot extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quant_signal_id", nullable = false, unique = true)
    private QuantSignal quantSignal;

    @Column(precision = 6, scale = 2)
    private BigDecimal volumeRatio5d;

    @Column(precision = 6, scale = 2)
    private BigDecimal priceMomentum5d;

    @Column(precision = 5, scale = 2)
    private BigDecimal newsFreshnessScore;

    /** 시장 국면 보정 계수 (-10 ~ +10) */
    @Column(precision = 5, scale = 2)
    private BigDecimal marketRegimeBonus;

    /** 전체 Feature 원시값 JSON (디버깅용) */
    @Column(columnDefinition = "TEXT")
    private String rawFeatureJson;

    @Column(nullable = false)
    private LocalDateTime snapshotAt;

    public static QuantFeatureSnapshot create(QuantSignal quantSignal,
                                              double volumeRatio5d,
                                              double priceMomentum5d,
                                              double newsFreshnessScore,
                                              double marketRegimeBonus,
                                              String rawFeatureJson) {
        return new QuantFeatureSnapshot(
                quantSignal,
                BigDecimal.valueOf(volumeRatio5d),
                BigDecimal.valueOf(priceMomentum5d),
                BigDecimal.valueOf(newsFreshnessScore),
                BigDecimal.valueOf(marketRegimeBonus),
                rawFeatureJson,
                LocalDateTime.now()
        );
    }
}
