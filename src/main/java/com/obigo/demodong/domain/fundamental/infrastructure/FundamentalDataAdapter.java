package com.obigo.demodong.domain.fundamental.infrastructure;

import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.fundamental.domain.port.FundamentalDataPort;
import com.obigo.demodong.domain.fundamental.infrastructure.KisFundamentalAdapter.KisMarketRatios;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * {@link FundamentalDataPort} 구현체.
 *
 * <p>조회 전략:</p>
 * <ul>
 *   <li>USA 또는 dartCorpCode null → stub() 즉시 반환</li>
 *   <li>KOR: DART(재무제표) + KIS(PER/PBR/EPS) 병합 → FundamentalSnapshot 반환</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundamentalDataAdapter implements FundamentalDataPort {

    private final DartFundamentalAdapter dartAdapter;
    private final KisFundamentalAdapter kisAdapter;

    @Override
    public FundamentalSnapshot fetch(String ticker, String dartCorpCode, MarketType market) {
        if (market == MarketType.USA || dartCorpCode == null || dartCorpCode.isBlank()) {
            log.debug("[Fundamental] stub 반환 - ticker: {}, market: {}, hasDartCode: {}",
                    ticker, market, dartCorpCode != null);
            return FundamentalSnapshot.stub();
        }

        // DART: Piotroski 9항목 계산용 재무 데이터
        FundamentalSnapshot dartSnapshot = dartAdapter.fetchFinancials(dartCorpCode);

        // KIS: PER / PBR / EPS / 시가총액
        KisMarketRatios ratios = kisAdapter.fetchMarketRatios(ticker);

        // 두 소스 병합
        return merge(dartSnapshot, ratios);
    }

    /**
     * DART 스냅샷과 KIS 시장 지표를 병합한다.
     * DART 스냅샷의 per/pbr/eps/marketCap 은 null이므로 KIS 값으로 채운다.
     */
    private FundamentalSnapshot merge(FundamentalSnapshot dart, KisMarketRatios ratios) {
        BigDecimal earningsYield = null;
        if (dart.operatingProfit() != null && ratios.marketCap() != null && ratios.marketCap().compareTo(BigDecimal.ZERO) > 0) {
            // marketCap: 억 원 단위 -> 원 단위 환산 (marketCap * 100,000,000)
            // EY (%) = (영업이익 / 시가총액_원) * 100
            //        = 영업이익 / (marketCap * 1,000,000)
            BigDecimal denominator = ratios.marketCap().multiply(BigDecimal.valueOf(1_000_000));
            try {
                earningsYield = dart.operatingProfit()
                        .divide(denominator, new java.math.MathContext(10, java.math.RoundingMode.HALF_UP))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            } catch (Exception e) {
                log.warn("[Fundamental-Merge] Earnings Yield 계산 실패: {}", e.getMessage());
            }
        }

        return new FundamentalSnapshot(
                ratios.per(),
                ratios.pbr(),
                ratios.eps(),
                ratios.marketCap(),
                null,                               // roe — 향후 Phase에서 KIS 또는 DART 추가
                dart.roa(),
                dart.prevRoa(),
                dart.operatingCashFlow(),
                dart.totalAssets(),
                dart.debtRatio(),
                dart.prevDebtRatio(),
                dart.currentRatio(),
                dart.prevCurrentRatio(),
                dart.grossProfitMargin(),
                dart.prevGrossProfitMargin(),
                dart.assetTurnover(),
                dart.prevAssetTurnover(),
                dart.revenueGrowthYoy(),
                dart.sharesOutstanding(),
                dart.prevSharesOutstanding(),
                dart.roic(),
                earningsYield,
                dart.operatingProfit()
        );
    }
}
