package com.obigo.demodong.domain.analysis.domain.entity;

import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 종목 분석 히스토리 엔티티.
 * 분석 API 호출 시 전략 점수 스냅샷을 저장한다.
 * 당일 ticker 중복 시 UPDATE (ticker + analyze_date UNIQUE).
 * 90일 초과 데이터는 HistoryCleanupScheduler가 삭제한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "analysis_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_analysis_history_ticker_date",
                columnNames = {"ticker", "analyze_date"}
        )
)
@SuperBuilder
public class AnalysisHistory extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false)
    private LocalDate analyzeDate;

    @Column
    private Integer totalScore;

    @Column
    private Integer canslimScore;

    @Column
    private Integer momentumScore;

    @Column
    private Integer seasonalityScore;

    @Column
    private Integer minerviniScore;

    @Column
    private Integer magicFormulaScore;

    @Column
    private Integer piotroskiScore;

    @Column
    private Integer meanReversionScore;

    // ─────────────────────────────────────────
    // 점수 업데이트 (당일 재분석 시 덮어쓰기)
    // ─────────────────────────────────────────

    public void updateScores(Integer total, Integer canslim, Integer momentum,
                             Integer seasonality, Integer minervini, Integer magic,
                             Integer piotroski, Integer reversion) {
        this.totalScore       = total;
        this.canslimScore     = canslim;
        this.momentumScore    = momentum;
        this.seasonalityScore = seasonality;
        this.minerviniScore   = minervini;
        this.magicFormulaScore = magic;
        this.piotroskiScore   = piotroski;
        this.meanReversionScore = reversion;
    }
}
