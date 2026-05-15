package com.obigo.demodong.domain.quant.domain.entity;

import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Quant Scoring Engine이 선정한 TOP3 종목 시그널 결과.
 * LLM 리포트는 해석 목적이며, 종목 선정은 totalScore가 결정한다.
 */
@Entity
@Table(name = "quant_signal",
        indexes = {
                @Index(name = "idx_quant_signal_date_rank", columnList = "signal_date, signal_rank"),
                @Index(name = "idx_quant_signal_ticker_date", columnList = "ticker, signal_date")
        })
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QuantSignal extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String stockName;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 10)
    private MarketType marketType;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(nullable = false)
    private int signalRank;  // 1~3

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketRegime marketRegime;

    @Column(columnDefinition = "TEXT")
    private String llmReport;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String disclaimer;

    @Column(nullable = false)
    private LocalDate signalDate;

    public static QuantSignal create(String ticker, String stockName, MarketType marketType,
                                     double totalScore, int signalRank, MarketRegime marketRegime,
                                     LocalDate signalDate) {
        return new QuantSignal(ticker, stockName, marketType,
                BigDecimal.valueOf(totalScore), signalRank, marketRegime,
                null,
                "⚠️ 본 시그널은 AI 참고 정보입니다. 투자 판단과 책임은 전적으로 사용자에게 있습니다.",
                signalDate);
    }

    public void updateLlmReport(String report) {
        this.llmReport = report;
    }
}
