package com.obigo.demodong.domain.quant.domain.service;

import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.quant.domain.model.QuantScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Quant Score가 높아도 아래 조건 해당 시 후보에서 제외하는 리스크 필터.
 *
 * 필터 3개:
 * 1. 유동성 필터: volumeRatio5d < 0.3
 * 2. 하락장 필터: BEAR + priceMomentum5d < -5.0
 * 3. 급등 선반영 필터: priceMomentum5d > 20.0
 */
@Slf4j
@Service
public class RiskFilterService {

    public List<QuantScore> filter(List<QuantScore> scores) {
        return scores.stream()
                .map(this::applyFilters)
                .toList();
    }

    private QuantScore applyFilters(QuantScore score) {
        // 유동성 필터
        if (score.volumeRatio5d() < 0.3) {
            log.info("[RiskFilter] 유동성 필터 EXCLUDE - ticker={}, volumeRatio={}", score.ticker(), score.volumeRatio5d());
            return QuantScore.excluded(score.ticker(), score.stockName(), score.marketType(),
                    score.marketRegime(), "유동성 부족 (거래량 급감)");
        }

        // 급등 선반영 필터
        if (score.priceMomentum5d() > 20.0) {
            log.info("[RiskFilter] 선반영 필터 EXCLUDE - ticker={}, momentum={}%", score.ticker(), score.priceMomentum5d());
            return QuantScore.excluded(score.ticker(), score.stockName(), score.marketType(),
                    score.marketRegime(), "급등 선반영 리스크 (5일 +20% 초과)");
        }

        // 하락장 필터
        if (score.marketRegime() == MarketRegime.BEAR && score.priceMomentum5d() < -5.0) {
            log.info("[RiskFilter] 하락장 필터 EXCLUDE - ticker={}, momentum={}%", score.ticker(), score.priceMomentum5d());
            return QuantScore.excluded(score.ticker(), score.stockName(), score.marketType(),
                    score.marketRegime(), "하락장 + 약세 종목");
        }

        return score;
    }
}
