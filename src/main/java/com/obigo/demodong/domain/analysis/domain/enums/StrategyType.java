package com.obigo.demodong.domain.analysis.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 7가지 투자 전략 유형.
 */
@Getter
@RequiredArgsConstructor
public enum StrategyType {

    MEAN_REVERSION("평균 회귀"),       // Phase 2
    MINERVINI("미네르비니 추세"),        // Phase 2
    DUAL_MOMENTUM("듀얼 모멘텀"),       // Phase 3
    SEASONALITY("시즌성"),             // Phase 3
    PIOTROSKI("피오트로스키 F-Score"),   // Phase 4
    CANSLIM("CAN SLIM"),              // Phase 5
    MAGIC_FORMULA("매직 포뮬러");       // Phase 6

    private final String displayName;
}
