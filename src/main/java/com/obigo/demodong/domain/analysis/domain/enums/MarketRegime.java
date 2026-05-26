package com.obigo.demodong.domain.analysis.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시장 국면 구분 Enum.
 */
@Getter
@RequiredArgsConstructor
public enum MarketRegime {
    STRONG_BULL("강세장"),   // 지수 5d > +3%
    BULL("상승장"),          // 5d > 0%, 20d > 0%
    SIDEWAYS("횡보장"),      // -1% < 5d < +1%
    BEAR("하락장"),          // 5d < -1% or 20d < -2%
    CRISIS("급락장");         // 5d < -5%

    private final String displayName;
}
