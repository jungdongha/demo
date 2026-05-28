package com.obigo.demodong.domain.analysis.application.dto.response;

import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;

import java.math.BigDecimal;

/**
 * 재무 지표 응답 DTO.
 * KOR 전용 지표. {@code supported = false}이면 프론트에서 미지원 안내를 렌더링한다.
 */
public record FundamentalResponse(
        boolean supported,

        // Market Ratios
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal eps,
        BigDecimal marketCapBillionKrw,   // 시가총액 (억 원)

        // Profitability
        BigDecimal roa,

        // Cash Flow
        BigDecimal operatingCashFlow,

        // Leverage / Liquidity
        BigDecimal debtRatio,
        BigDecimal currentRatio,

        // Efficiency
        BigDecimal grossProfitMargin,
        BigDecimal assetTurnover,

        // Growth
        BigDecimal revenueGrowthYoy,

        // Magic Formula
        BigDecimal roic,
        BigDecimal earningsYield,
        BigDecimal operatingProfit
) {
    public static FundamentalResponse from(FundamentalSnapshot f) {
        if (f == null || f.isStub()) {
            return unsupported();
        }
        return new FundamentalResponse(
                true,
                f.per(),
                f.pbr(),
                f.eps(),
                f.marketCap(),
                f.roa(),
                f.operatingCashFlow(),
                f.debtRatio(),
                f.currentRatio(),
                f.grossProfitMargin(),
                f.assetTurnover(),
                f.revenueGrowthYoy(),
                f.roic(),
                f.earningsYield(),
                f.operatingProfit()
        );
    }

    /** 미국 주식 또는 재무 데이터 미지원 종목용 */
    public static FundamentalResponse unsupported() {
        return new FundamentalResponse(
                false,
                null, null, null, null,
                null,
                null,
                null, null,
                null, null,
                null,
                null, null, null
        );
    }
}
