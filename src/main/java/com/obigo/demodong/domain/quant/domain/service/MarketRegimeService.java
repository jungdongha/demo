package com.obigo.demodong.domain.quant.domain.service;

import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import org.springframework.stereotype.Service;

/**
 * 시장 지수 5일 변동률 기반 시장 국면 판단.
 *
 * 판단 기준 (5일 지수 변동률):
 *   > +3%  → STRONG_BULL
 *   > 0%   → BULL
 *   > -2%  → SIDEWAYS
 *   > -5%  → BEAR
 *   <= -5% → CRISIS
 */
@Service
public class MarketRegimeService {

    public MarketRegime analyze(double indexReturn5d) {
        if (indexReturn5d > 3.0)  return MarketRegime.STRONG_BULL;
        if (indexReturn5d > 0.0)  return MarketRegime.BULL;
        if (indexReturn5d > -2.0) return MarketRegime.SIDEWAYS;
        if (indexReturn5d > -5.0) return MarketRegime.BEAR;
        return MarketRegime.CRISIS;
    }
}
