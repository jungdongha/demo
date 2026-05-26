package com.obigo.demodong.domain.technical.domain.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 기술적 분석 지표 스냅샷.
 * 모든 필드는 데이터 부족 시 null — 전략 Calculator는 null을 50점 중립으로 처리한다.
 *
 * @param emaMap          기간별 현재 EMA 값 (key: 20/50/150/200)
 * @param ema200OneMonthAgo EMA200 20거래일 전 값 — Minervini 조건2 (우상향 판단)용
 * @param deviationFromSma20 이격률 = (현재가 - SMA20) / SMA20 × 100 (%)
 */
public record TechnicalSnapshot(
        BigDecimal rsi14,
        BigDecimal macd,
        BigDecimal macdSignal,
        BigDecimal macdHistogram,
        BigDecimal bollingerUpper,
        BigDecimal bollingerMid,
        BigDecimal bollingerLower,
        Map<Integer, BigDecimal> emaMap,
        BigDecimal ema200OneMonthAgo,
        BigDecimal atr14,
        BigDecimal adx14,
        BigDecimal sma20,
        BigDecimal sma60,
        BigDecimal currentPrice,
        BigDecimal high52w,
        BigDecimal low52w,
        BigDecimal deviationFromSma20
) {}
