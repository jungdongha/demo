package com.obigo.demodong.domain.analysis.domain.model;

import com.obigo.demodong.domain.flow.domain.model.FlowSnapshot;
import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;
import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;

/**
 * 전략 Calculator 공통 입력 데이터.
 * 도메인 간 데이터 교환의 유일한 통로 (도메인 직접 의존 금지 원칙).
 *
 * <p>Phase별 확장 이력:</p>
 * <ul>
 *   <li>Phase 3: marketRegime, momentum, sector 추가</li>
 *   <li>Phase 4: fundamental 추가 (FundamentalSnapshot)</li>
 *   <li>Phase 5: flow 추가 (FlowSnapshot)</li>
 * </ul>
 */
public record StrategyInput(
        String ticker,
        MarketType market,
        TechnicalSnapshot technical,
        MomentumSnapshot momentum,          // Phase 3 추가
        MarketRegime marketRegime,          // Phase 3 추가
        String sector,                      // Phase 3 추가 (Stock.sector)
        FundamentalSnapshot fundamental,    // Phase 4 추가
        FlowSnapshot flow                   // Phase 5 추가
) {
    /** Phase 4 코드 하위 호환 (7인자) */
    public StrategyInput(String ticker, MarketType market, TechnicalSnapshot technical,
                         MomentumSnapshot momentum, MarketRegime marketRegime, String sector,
                         FundamentalSnapshot fundamental) {
        this(ticker, market, technical, momentum, marketRegime, sector, fundamental, null);
    }

    /** Phase 3 이전 코드 하위 호환 (6인자) */
    public StrategyInput(String ticker, MarketType market, TechnicalSnapshot technical,
                         MomentumSnapshot momentum, MarketRegime marketRegime, String sector) {
        this(ticker, market, technical, momentum, marketRegime, sector, null, null);
    }

    /** Phase 2 이전 코드 하위 호환 (3인자) */
    public StrategyInput(String ticker, MarketType market, TechnicalSnapshot technical) {
        this(ticker, market, technical, null, null, null, null, null);
    }
}

