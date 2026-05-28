package com.obigo.demodong.domain.analysis.application.dto.response;

import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;

import java.math.BigDecimal;

/**
 * 기술적 지표 응답 DTO.
 * {@link TechnicalSnapshot}을 API 응답 형태로 변환.
 */
public record TechnicalResponse(
        BigDecimal currentPrice,
        BigDecimal rsi14,
        BigDecimal macd,
        BigDecimal macdSignal,
        BigDecimal macdHistogram,
        BigDecimal bollingerUpper,
        BigDecimal bollingerMid,
        BigDecimal bollingerLower,
        BigDecimal ema20,
        BigDecimal ema50,
        BigDecimal ema150,
        BigDecimal ema200,
        BigDecimal sma20,
        BigDecimal sma60,
        BigDecimal high52w,
        BigDecimal low52w,
        BigDecimal deviationFromSma20,
        BigDecimal atr14,
        BigDecimal adx14,
        // 모멘텀 수익률
        BigDecimal return1m,
        BigDecimal return3m,
        BigDecimal return6m,
        BigDecimal return12m,
        int rsRating
) {
    public static TechnicalResponse from(TechnicalSnapshot t,
                                         BigDecimal realTimePrice,
                                         BigDecimal return1m, BigDecimal return3m,
                                         BigDecimal return6m, BigDecimal return12m,
                                         int rsRating) {
        return new TechnicalResponse(
                realTimePrice != null ? realTimePrice : t.currentPrice(),
                t.rsi14(),
                t.macd(),
                t.macdSignal(),
                t.macdHistogram(),
                t.bollingerUpper(),
                t.bollingerMid(),
                t.bollingerLower(),
                t.emaMap() != null ? t.emaMap().get(20)  : null,
                t.emaMap() != null ? t.emaMap().get(50)  : null,
                t.emaMap() != null ? t.emaMap().get(150) : null,
                t.emaMap() != null ? t.emaMap().get(200) : null,
                t.sma20(),
                t.sma60(),
                t.high52w(),
                t.low52w(),
                t.deviationFromSma20(),
                t.atr14(),
                t.adx14(),
                return1m,
                return3m,
                return6m,
                return12m,
                rsRating
        );
    }
}
