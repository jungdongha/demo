package com.obigo.demodong.domain.analysis.domain.calculator;

import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Piotroski F-Score 전략 Calculator (한국 전용).
 *
 * <p>9개 항목 각 1점 → 합산 × 11.1 → 0~100 정규화</p>
 *
 * <pre>
 * 수익성 (3점)
 *   F1: ROA > 0
 *   F2: 영업현금흐름(CFO) > 0
 *   F3: 전년 대비 ROA 개선
 *
 * 레버리지 / 유동성 (3점)
 *   F4: 부채비율 감소
 *   F5: 유동비율 개선
 *   F6: 신주 미발행 (발행주식수 전년比 증가 없음)
 *
 * 효율성 (3점)
 *   F7: 매출총이익률 개선
 *   F8: 자산회전율 개선
 *   F9: CFO/총자산 > ROA (발생주의 vs 현금 일치)
 * </pre>
 *
 * <p>null 처리:</p>
 * <ul>
 *   <li>fundamental == null 또는 isStub() → 50점 중립 반환</li>
 *   <li>개별 필드 null → 해당 항목 0점 처리 (보수적)</li>
 * </ul>
 */
@Component
public class PiotroskiCalculator implements StrategyCalculator {

    private static final int NEUTRAL = 50;
    private static final double SCALE_FACTOR = 11.1; // 9점 만점 × 11.1 ≈ 100

    @Override
    public StrategyType getSupportedStrategy() {
        return StrategyType.PIOTROSKI;
    }

    @Override
    public StrategyScore calculate(StrategyInput input) {
        FundamentalSnapshot f = input.fundamental();

        if (f == null || f.isStub()) {
            return neutralScore("재무 데이터 없음 — 한국 상장 종목만 지원");
        }

        Map<String, Integer> detail = new LinkedHashMap<>();
        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();

        // ── 수익성 ──
        int f1 = scoreF1(f, detail, positives, negatives);
        int f2 = scoreF2(f, detail, positives, negatives);
        int f3 = scoreF3(f, detail, positives, negatives);

        // ── 레버리지 / 유동성 ──
        int f4 = scoreF4(f, detail, positives, negatives);
        int f5 = scoreF5(f, detail, positives, negatives);
        int f6 = scoreF6(f, detail, positives, negatives);

        // ── 효율성 ──
        int f7 = scoreF7(f, detail, positives, negatives);
        int f8 = scoreF8(f, detail, positives, negatives);
        int f9 = scoreF9(f, detail, positives, negatives);

        int fScore = f1 + f2 + f3 + f4 + f5 + f6 + f7 + f8 + f9;
        detail.put("f_score_raw", fScore);

        int totalScore = Math.min(100, (int) Math.round(fScore * SCALE_FACTOR));

        return new StrategyScore(
                StrategyType.PIOTROSKI,
                totalScore,
                StrategyScore.gradeOf(totalScore),
                Map.copyOf(detail),
                List.copyOf(positives),
                List.copyOf(negatives)
        );
    }

    // ──────────────────────────────────────────────────────────────────────
    //  F-Score 항목별 계산
    // ──────────────────────────────────────────────────────────────────────

    /** F1: ROA > 0 (수익 창출 여부) */
    private int scoreF1(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.roa() == null) { detail.put("F1_roa_positive", 0); return 0; }
        int score = f.roa().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;
        detail.put("F1_roa_positive", score);
        if (score == 1) pos.add(String.format("ROA 양수 (%.2f%%) — 자산 대비 수익 창출", f.roa()));
        else neg.add(String.format("ROA 음수 (%.2f%%) — 자산 대비 손실 발생", f.roa()));
        return score;
    }

    /** F2: 영업현금흐름(CFO) > 0 (현금 창출 여부) */
    private int scoreF2(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.operatingCashFlow() == null) { detail.put("F2_cfo_positive", 0); return 0; }
        int score = f.operatingCashFlow().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;
        detail.put("F2_cfo_positive", score);
        if (score == 1) pos.add("영업현금흐름 양수 — 실질 현금 창출 능력 확인");
        else neg.add("영업현금흐름 음수 — 영업에서 현금 유출 중");
        return score;
    }

    /** F3: 전년 대비 ROA 개선 */
    private int scoreF3(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.roa() == null || f.prevRoa() == null) { detail.put("F3_roa_improving", 0); return 0; }
        int score = f.roa().compareTo(f.prevRoa()) > 0 ? 1 : 0;
        detail.put("F3_roa_improving", score);
        if (score == 1) pos.add(String.format("ROA 개선 (%.2f%% → %.2f%%)", f.prevRoa(), f.roa()));
        else neg.add(String.format("ROA 악화 (%.2f%% → %.2f%%)", f.prevRoa(), f.roa()));
        return score;
    }

    /** F4: 부채비율 감소 (재무 건전성 개선) */
    private int scoreF4(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.debtRatio() == null || f.prevDebtRatio() == null) { detail.put("F4_debt_decreasing", 0); return 0; }
        int score = f.debtRatio().compareTo(f.prevDebtRatio()) < 0 ? 1 : 0;
        detail.put("F4_debt_decreasing", score);
        if (score == 1) pos.add(String.format("부채비율 감소 (%.1f%% → %.1f%%)", f.prevDebtRatio(), f.debtRatio()));
        else neg.add(String.format("부채비율 증가 (%.1f%% → %.1f%%)", f.prevDebtRatio(), f.debtRatio()));
        return score;
    }

    /** F5: 유동비율 개선 (단기 유동성 개선) */
    private int scoreF5(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.currentRatio() == null || f.prevCurrentRatio() == null) { detail.put("F5_liquidity_improving", 0); return 0; }
        int score = f.currentRatio().compareTo(f.prevCurrentRatio()) > 0 ? 1 : 0;
        detail.put("F5_liquidity_improving", score);
        if (score == 1) pos.add(String.format("유동비율 개선 (%.2f → %.2f)", f.prevCurrentRatio(), f.currentRatio()));
        else neg.add(String.format("유동비율 악화 (%.2f → %.2f)", f.prevCurrentRatio(), f.currentRatio()));
        return score;
    }

    /** F6: 신주 미발행 (주주 희석 없음) */
    private int scoreF6(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.sharesOutstanding() == null || f.prevSharesOutstanding() == null) {
            detail.put("F6_no_dilution", 0); return 0;
        }
        int score = f.sharesOutstanding() <= f.prevSharesOutstanding() ? 1 : 0;
        detail.put("F6_no_dilution", score);
        if (score == 1) pos.add("신주 미발행 — 주주 희석 없음");
        else neg.add(String.format("신주 발행 감지 (%,d → %,d주) — 주주 희석 위험",
                f.prevSharesOutstanding(), f.sharesOutstanding()));
        return score;
    }

    /** F7: 매출총이익률 개선 (운영 효율성 개선) */
    private int scoreF7(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.grossProfitMargin() == null || f.prevGrossProfitMargin() == null) {
            detail.put("F7_gpm_improving", 0); return 0;
        }
        int score = f.grossProfitMargin().compareTo(f.prevGrossProfitMargin()) > 0 ? 1 : 0;
        detail.put("F7_gpm_improving", score);
        if (score == 1) pos.add(String.format("매출총이익률 개선 (%.1f%% → %.1f%%)",
                f.prevGrossProfitMargin(), f.grossProfitMargin()));
        else neg.add(String.format("매출총이익률 악화 (%.1f%% → %.1f%%)",
                f.prevGrossProfitMargin(), f.grossProfitMargin()));
        return score;
    }

    /** F8: 자산회전율 개선 (자산 활용 효율 개선) */
    private int scoreF8(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.assetTurnover() == null || f.prevAssetTurnover() == null) {
            detail.put("F8_turnover_improving", 0); return 0;
        }
        int score = f.assetTurnover().compareTo(f.prevAssetTurnover()) > 0 ? 1 : 0;
        detail.put("F8_turnover_improving", score);
        if (score == 1) pos.add(String.format("자산회전율 개선 (%.3f → %.3f)",
                f.prevAssetTurnover(), f.assetTurnover()));
        else neg.add(String.format("자산회전율 악화 (%.3f → %.3f)",
                f.prevAssetTurnover(), f.assetTurnover()));
        return score;
    }

    /** F9: CFO/총자산 > ROA (현금이익 > 발생이익 — 이익의 질) */
    private int scoreF9(FundamentalSnapshot f, Map<String, Integer> detail,
                        List<String> pos, List<String> neg) {
        if (f.operatingCashFlow() == null || f.totalAssets() == null
                || f.roa() == null || f.totalAssets().compareTo(BigDecimal.ZERO) == 0) {
            detail.put("F9_accrual", 0);
            return 0;
        }
        // CFO/총자산 (소수) vs ROA/100 (소수 변환)
        BigDecimal cfoRatio = f.operatingCashFlow().divide(f.totalAssets(),
                java.math.MathContext.DECIMAL64);
        BigDecimal roaDecimal = f.roa().divide(BigDecimal.valueOf(100),
                java.math.MathContext.DECIMAL64);

        int score = cfoRatio.compareTo(roaDecimal) > 0 ? 1 : 0;
        detail.put("F9_accrual", score);
        if (score == 1) pos.add("현금이익 > 발생이익 — 이익의 질 양호 (발생주의 왜곡 없음)");
        else neg.add("현금이익 < 발생이익 — 이익의 질 의심 (발생주의 과대 계상 가능)");
        return score;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Helper
    // ──────────────────────────────────────────────────────────────────────

    private StrategyScore neutralScore(String reason) {
        return new StrategyScore(
                StrategyType.PIOTROSKI,
                NEUTRAL,
                StrategyScore.gradeOf(NEUTRAL),
                Map.of("f_score_raw", -1),
                List.of(),
                List.of(reason)
        );
    }
}
