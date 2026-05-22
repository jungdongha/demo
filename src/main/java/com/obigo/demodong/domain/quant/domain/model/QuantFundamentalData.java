package com.obigo.demodong.domain.quant.domain.model;

import java.math.BigDecimal;

/**
 * Quant Feature 계산에 사용되는 재무·컨센서스 데이터.
 * KIS inquire-price API로 PER/PBR/현재가 조회.
 * 목표주가(targetPrice)는 Phase 10에서 Naver 스크래핑 연동 예정 — 현재 null(Stub).
 *
 * @param per          P/E ratio (nullable — 적자 기업은 null)
 * @param pbr          P/B ratio (nullable)
 * @param targetPrice  증권사 평균 목표주가 (nullable — Phase 10 Stub)
 * @param currentPrice 현재가 (목표주가 괴리율 계산용)
 */
public record QuantFundamentalData(
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal targetPrice,
        BigDecimal currentPrice
) {
    /** USA / 데이터 조회 실패 시 사용하는 Stub — 모든 Feature가 중립값(50점) 반환 */
    public static QuantFundamentalData stub(BigDecimal currentPrice) {
        return new QuantFundamentalData(null, null, null, currentPrice);
    }
}
