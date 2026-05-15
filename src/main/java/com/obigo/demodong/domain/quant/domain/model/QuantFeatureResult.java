package com.obigo.demodong.domain.quant.domain.model;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;

/**
 * 개별 Feature 계산 결과.
 * rawValue: 원시 계산값 (거래량 배수, 모멘텀 % 등)
 * normalizedScore: 0~100 정규화 점수 (가중치 적용 전)
 */
public record QuantFeatureResult(
        FeatureType featureType,
        double rawValue,
        double normalizedScore
) {}
