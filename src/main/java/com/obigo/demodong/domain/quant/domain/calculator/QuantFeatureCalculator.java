package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;

/**
 * Feature 계산 전략 인터페이스 (Strategy Pattern — OCP 준수).
 * 새 Feature 추가 시 이 인터페이스를 구현하는 Calculator만 추가한다.
 */
public interface QuantFeatureCalculator {

    QuantFeatureResult calculate(QuantFeatureInput input);

    FeatureType getFeatureType();
}
