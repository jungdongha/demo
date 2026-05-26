package com.obigo.demodong.domain.analysis.domain.calculator;

import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;

/**
 * 전략 점수 계산기 인터페이스 (OCP 준수).
 * 새 전략 추가 시 이 인터페이스를 구현하는 것만으로 확장된다.
 *
 * <p>구현 규칙:</p>
 * <ul>
 *   <li>null 데이터 → 50점 중립 (0점 패널티 없음)</li>
 *   <li>항목별 점수는 반드시 detail Map에 기록 (Explainable Quant)</li>
 *   <li>score 범위: 0~100</li>
 * </ul>
 */
public interface StrategyCalculator {

    /**
     * 전략 점수를 계산한다.
     *
     * @param input 전략 입력 데이터 (기술/재무/수급 스냅샷 포함)
     * @return 전략 점수 결과 (score, grade, detail, positives, negatives)
     */
    StrategyScore calculate(StrategyInput input);

    /**
     * 이 Calculator가 지원하는 전략 유형을 반환한다.
     */
    StrategyType getSupportedStrategy();
}
