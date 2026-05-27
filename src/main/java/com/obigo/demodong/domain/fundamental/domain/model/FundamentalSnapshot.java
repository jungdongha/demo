package com.obigo.demodong.domain.fundamental.domain.model;

import java.math.BigDecimal;

/**
 * 재무 분석 스냅샷.
 * 모든 필드는 데이터 부족 시 null — 전략 Calculator는 null을 50점 중립으로 처리한다.
 * 미국 주식 또는 DART 미매핑 종목은 {@link #stub()} 으로 표현한다.
 *
 * <p>Phase 4에서 구현된 필드:</p>
 * <ul>
 *   <li>Market Ratios: per, pbr, eps, marketCap (KIS inquire-price)</li>
 *   <li>Piotroski 9항목 계산용 재무 데이터 (DART 재무제표)</li>
 * </ul>
 *
 * <p>Phase 5 이후 확장 예정:</p>
 * <ul>
 *   <li>roic, peg, operatingMargin, netMargin, epsGrowthQoq (추가 분석 필드)</li>
 * </ul>
 */
public record FundamentalSnapshot(

        // ── Market Ratios (KIS inquire-price) ──
        BigDecimal per,         // 주가수익비율
        BigDecimal pbr,         // 주가순자산비율
        BigDecimal eps,         // 주당순이익 (원)
        BigDecimal marketCap,   // 시가총액 (억 원)

        // ── Profitability ──
        BigDecimal roe,         // 자기자본이익률 (%)
        BigDecimal roa,         // 총자산이익률 (%) 당기 — Piotroski F1, F9
        BigDecimal prevRoa,     // 총자산이익률 (%) 전기 — Piotroski F3

        // ── Cash Flow ──
        BigDecimal operatingCashFlow,   // 영업활동현금흐름 절대값 (원) — Piotroski F2, F9
        BigDecimal totalAssets,         // 총자산 절대값 (원) — Piotroski F9 분모

        // ── Leverage / Liquidity ──
        BigDecimal debtRatio,           // 부채비율 (부채/자산 × 100, %) 당기 — Piotroski F4
        BigDecimal prevDebtRatio,       // 부채비율 전기 — Piotroski F4
        BigDecimal currentRatio,        // 유동비율 (유동자산/유동부채) 당기 — Piotroski F5
        BigDecimal prevCurrentRatio,    // 유동비율 전기 — Piotroski F5

        // ── Efficiency ──
        BigDecimal grossProfitMargin,       // 매출총이익률 (%) 당기 — Piotroski F7
        BigDecimal prevGrossProfitMargin,   // 매출총이익률 (%) 전기 — Piotroski F7
        BigDecimal assetTurnover,           // 자산회전율 (매출/총자산) 당기 — Piotroski F8
        BigDecimal prevAssetTurnover,       // 자산회전율 전기 — Piotroski F8

        // ── Growth ──
        BigDecimal revenueGrowthYoy,        // 매출 YoY 성장률 (%)

        // ── Dilution ──
        Long sharesOutstanding,             // 보통주 발행주식수 당기 — Piotroski F6
        Long prevSharesOutstanding          // 보통주 발행주식수 전기 — Piotroski F6

) {
    /**
     * 미국 주식 또는 DART 미지원 종목용 빈 스냅샷.
     * 모든 필드 null — Calculator는 isStub() 체크 후 50점 중립 반환.
     */
    public static FundamentalSnapshot stub() {
        return new FundamentalSnapshot(
                null, null, null, null,
                null, null, null,
                null, null,
                null, null, null, null,
                null, null, null, null,
                null,
                null, null
        );
    }

    /**
     * Piotroski 계산의 핵심 3개 필드가 모두 null이면 stub으로 판단.
     */
    public boolean isStub() {
        return roa == null && operatingCashFlow == null && totalAssets == null;
    }
}
