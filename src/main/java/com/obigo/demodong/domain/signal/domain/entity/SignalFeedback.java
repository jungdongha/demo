package com.obigo.demodong.domain.signal.domain.entity;

import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Phase 8 — 시그널 피드백 엔티티
 * 과거 시그널의 T+3 / T+10 / T+20 영업일 이후 주가를 추적하여 Alpha를 계산한다.
 */
@Entity
@Table(name = "signal_feedback")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignalFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private SignalReport report;

    // 시그널 시점 주가
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtSignal;

    // T+3 / T+10 / T+20 영업일 종가 (순차적으로 채워짐)
    @Column(precision = 12, scale = 2)
    private BigDecimal priceAfter3d;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceAfter10d;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceAfter20d;

    // 기간 내 최저가 (T+0 ~ T+20 MDD 계산용)
    @Column(precision = 12, scale = 2)
    private BigDecimal priceMdd;

    // 수익률 (RAW — 벤치마크 미차감, Phase 9+에서 확장)
    @Column(precision = 8, scale = 2)
    private BigDecimal alpha3d;

    @Column(precision = 8, scale = 2)
    private BigDecimal alpha10d;    // Primary 평가 지표

    @Column(precision = 8, scale = 2)
    private BigDecimal alpha20d;

    @Column(precision = 8, scale = 2)
    private BigDecimal mddPct;      // 최대 낙폭 %

    // 방향성 일치 여부
    private Boolean wasCorrect3d;   // T+3 기준
    private Boolean wasCorrect10d;  // T+10 기준 (Primary)

    // 실패 판정 — BUY: alpha10d < -5%, SELL: alpha10d > +5%
    private Boolean isFailure;

    private LocalDateTime evaluatedAt;

    // ========================= 비즈니스 메서드 =========================

    /**
     * T+3 데이터를 채운다.
     */
    public void fillT3(BigDecimal priceAfter3d, boolean wasCorrect3d) {
        this.priceAfter3d = priceAfter3d;
        this.alpha3d = calcAlpha(priceAfter3d);
        this.wasCorrect3d = wasCorrect3d;
        this.evaluatedAt = LocalDateTime.now();
    }

    /**
     * T+10 데이터를 채운다.
     */
    public void fillT10(BigDecimal priceAfter10d, BigDecimal priceMdd, boolean wasCorrect10d) {
        this.priceAfter10d = priceAfter10d;
        this.alpha10d = calcAlpha(priceAfter10d);
        this.priceMdd = priceMdd;
        this.mddPct = priceMdd != null ? calcAlpha(priceMdd) : null;
        this.wasCorrect10d = wasCorrect10d;
        this.isFailure = determineFailure(report.getSignalType().name(), this.alpha10d);
        this.evaluatedAt = LocalDateTime.now();
    }

    /**
     * T+20 데이터를 채운다.
     */
    public void fillT20(BigDecimal priceAfter20d, BigDecimal finalMdd) {
        this.priceAfter20d = priceAfter20d;
        this.alpha20d = calcAlpha(priceAfter20d);
        if (finalMdd != null) {
            this.priceMdd = finalMdd;
            this.mddPct = calcAlpha(finalMdd);
        }
        this.evaluatedAt = LocalDateTime.now();
    }

    private BigDecimal calcAlpha(BigDecimal priceAfterNd) {
        if (priceAtSignal == null || priceAtSignal.compareTo(BigDecimal.ZERO) == 0) return null;
        return priceAfterNd.subtract(priceAtSignal)
                .divide(priceAtSignal, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Boolean determineFailure(String signalType, BigDecimal alpha10d) {
        if (alpha10d == null) return null;
        return switch (signalType) {
            case "BUY"  -> alpha10d.compareTo(new BigDecimal("-5.00")) < 0;
            case "SELL" -> alpha10d.compareTo(new BigDecimal("5.00")) > 0;
            default     -> false; // HOLD
        };
    }
}
