package com.obigo.demodong.domain.analysis.domain.model;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;
import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;

/**
 * 전략 Calculator 공통 입력 데이터.
 * 도메인 간 데이터 교환의 유일한 통로 (도메인 직접 의존 금지 원칙).
 *
 * <p>Phase별 확장 계획:</p>
 * <ul>
 *   <li>Phase 3: marketRegime, monthlyReturns, sector 추가</li>
 *   <li>Phase 4: fundamentalSnapshot 추가 (FundamentalSnapshot)</li>
 *   <li>Phase 5: flowSnapshot 추가 (FlowSnapshot)</li>
 * </ul>
 */
public record StrategyInput(
        String ticker,
        MarketType market,
        TechnicalSnapshot technical,
        MomentumSnapshot momentum,      // Phase 3 추가
        MarketRegime marketRegime,      // Phase 3 추가
        String sector                   // Phase 3 추가 (Stock.sector)
) {
    // 하위 호환성을 위한 3인자 생성자 추가
    public StrategyInput(String ticker, MarketType market, TechnicalSnapshot technical) {
        this(ticker, market, technical, null, null, null);
    }
}

