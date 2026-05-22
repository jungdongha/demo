package com.obigo.demodong.domain.quant.domain.entity;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import com.obigo.demodong.domain.quant.domain.model.QuantScore;
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

    // ── 기존 Feature ──────────────────────────────────────
    @Column(precision = 6, scale = 2)
    private BigDecimal volumeRatio5d;

    @Column(precision = 6, scale = 2)
    private BigDecimal priceMomentum5d;

    @Column(precision = 5, scale = 2)
    private BigDecimal newsFreshnessScore;

    /** 시장 국면 보정 계수 (-10 ~ +10) */
    @Column(precision = 5, scale = 2)
    private BigDecimal marketRegimeBonus;

    // ── 신규 Feature: 밸류에이션 ───────────────────────────
    /** 실제 PER 값 (nullable — 적자 기업·데이터 없음) */
    @Column(precision = 8, scale = 2)
    private BigDecimal perValue;

    /** 실제 PBR 값 (nullable) */
    @Column(precision = 6, scale = 2)
    private BigDecimal pbrValue;

    /** 밸류에이션 정규화 점수 (0~100) */
    @Column(precision = 5, scale = 2)
    private BigDecimal valuationScore;

    // ── 신규 Feature: 목표주가 ────────────────────────────
    /** 증권사 평균 목표주가 (nullable — Phase 10 Stub) */
    @Column(precision = 12, scale = 2)
    private BigDecimal targetPrice;

    /** 목표주가 대비 현재가 상승 여력 % (nullable) */
    @Column(precision = 6, scale = 2)
    private BigDecimal targetPriceUpsidePct;

    /** 목표주가 상승여력 정규화 점수 (0~100) */
    @Column(precision = 5, scale = 2)
    private BigDecimal targetPriceScore;

    // ── 신규 Feature: 섹터 상대 강도 ─────────────────────
    /** 섹터 내 상대 강도 정규화 점수 (0~100) */
    @Column(precision = 5, scale = 2)
    private BigDecimal sectorRelativeScore;

    /** 전체 Feature 원시값 JSON (디버깅용) */
    @Column(columnDefinition = "TEXT")
    private String rawFeatureJson;

    @Column(nullable = false)
    private LocalDateTime snapshotAt;

    /**
     * QuantScore로부터 스냅샷 생성.
     * QuantScore에 포함된 모든 Feature 값·펀더멘털 데이터를 컬럼에 매핑한다.
     */
    public static QuantFeatureSnapshot create(QuantSignal quantSignal, QuantScore score, String rawFeatureJson) {
        QuantFundamentalData f = score.fundamentals();
        double upsidePct = score.featureRawValues()
                .getOrDefault(FeatureType.TARGET_PRICE_UPSIDE, 0.0);

        return QuantFeatureSnapshot.builder()
                .quantSignal(quantSignal)
                .volumeRatio5d(BigDecimal.valueOf(score.volumeRatio5d()))
                .priceMomentum5d(BigDecimal.valueOf(score.priceMomentum5d()))
                .newsFreshnessScore(BigDecimal.valueOf(score.newsFreshness()))
                .marketRegimeBonus(BigDecimal.valueOf(
                        score.featureRawValues().getOrDefault(FeatureType.MARKET_REGIME, 0.0)))
                .perValue(f != null ? f.per() : null)
                .pbrValue(f != null ? f.pbr() : null)
                .valuationScore(BigDecimal.valueOf(score.valuationScore()))
                .targetPrice(f != null ? f.targetPrice() : null)
                .targetPriceUpsidePct(BigDecimal.valueOf(upsidePct))
                .targetPriceScore(BigDecimal.valueOf(score.targetPriceUpsideScore()))
                .sectorRelativeScore(BigDecimal.valueOf(score.sectorRelativeScore()))
                .rawFeatureJson(rawFeatureJson)
                .snapshotAt(LocalDateTime.now())
                .build();
    }
}
