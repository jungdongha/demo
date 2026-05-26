package com.obigo.demodong.domain.analysis.domain.model;

import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;

import java.util.List;
import java.util.Map;

/**
 * 전략 점수 결과.
 *
 * @param type      전략 유형
 * @param score     종합 점수 (0~100)
 * @param grade     등급 (S/A/B/C/D)
 * @param detail    항목별 세부 점수 — Explainable Quant 원칙
 * @param positives 긍정 근거 메시지 목록
 * @param negatives 부정 근거 메시지 목록
 */
public record StrategyScore(
        StrategyType type,
        int score,
        String grade,
        Map<String, Integer> detail,
        List<String> positives,
        List<String> negatives
) {

    /**
     * 점수 → 등급 변환.
     * S: 80~100 / A: 60~79 / B: 40~59 / C: 20~39 / D: 0~19
     */
    public static String gradeOf(int score) {
        if (score >= 80) return "S";
        if (score >= 60) return "A";
        if (score >= 40) return "B";
        if (score >= 20) return "C";
        return "D";
    }
}
