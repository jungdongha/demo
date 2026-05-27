package com.obigo.demodong.domain.analysis.domain.calculator;

import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;
import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.MomentumSnapshot;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.flow.domain.model.FlowSnapshot;
import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CAN SLIM 전략 Calculator — 재무 + 기술 + 수급 통합 전략.
 *
 * <pre>
 * 7항목 가중 평균 (합계 = 1.0):
 *   C (Current Earnings)  20% — revenueGrowthYoy 기반 매출 성장
 *   A (Annual Earnings)   15% — ROA 기반 수익성 지속성
 *   N (New High)          10% — 52주 신고가 근접도 (currentPrice / high52w)
 *   S (Supply/Demand)     20% — 기관 + 외국인 합산 순매수 방향
 *   L (Leader)            15% — RS Rating (0~100 그대로)
 *   I (Institutional)     10% — 기관 단독 순매수 강도
 *   M (Market)            10% — 시장 국면 변환 점수
 * </pre>
 *
 * <p>null 처리:</p>
 * <ul>
 *   <li>개별 항목 데이터 null → 50점 중립 (패널티 없음)</li>
 *   <li>flow null 또는 stub → S / I 항목 50점 중립</li>
 * </ul>
 */
@Component
public class CANSLIMCalculator implements StrategyCalculator {

    private static final int NEUTRAL = 50;

    // 항목별 가중치 (합계 = 1.0)
    private static final double W_C = 0.20;
    private static final double W_A = 0.15;
    private static final double W_N = 0.10;
    private static final double W_S = 0.20;
    private static final double W_L = 0.15;
    private static final double W_I = 0.10;
    private static final double W_M = 0.10;

    @Override
    public StrategyType getSupportedStrategy() {
        return StrategyType.CANSLIM;
    }

    @Override
    public StrategyScore calculate(StrategyInput input) {
        Map<String, Integer> detail = new LinkedHashMap<>();
        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();

        FundamentalSnapshot f = input.fundamental();
        TechnicalSnapshot t   = input.technical();
        MomentumSnapshot m    = input.momentum();
        FlowSnapshot flow      = input.flow();
        MarketRegime regime    = input.marketRegime();

        int c = scoreC(f, detail, positives, negatives);
        int a = scoreA(f, detail, positives, negatives);
        int n = scoreN(t, detail, positives, negatives);
        int s = scoreS(flow, detail, positives, negatives);
        int l = scoreL(m, detail, positives, negatives);
        int i = scoreI(flow, detail, positives, negatives);
        int mScore = scoreM(regime, detail, positives, negatives);

        double raw = c * W_C + a * W_A + n * W_N + s * W_S + l * W_L + i * W_I + mScore * W_M;
        int total = (int) Math.round(Math.max(0, Math.min(100, raw)));
        detail.put("canslim_total_raw", total);

        return new StrategyScore(
                StrategyType.CANSLIM,
                total,
                StrategyScore.gradeOf(total),
                Map.copyOf(detail),
                List.copyOf(positives),
                List.copyOf(negatives)
        );
    }

    // ──────────────────────────────────────────────────────────────────────
    //  C — Current Earnings (매출 성장률)
    // ──────────────────────────────────────────────────────────────────────

    /** C: revenueGrowthYoy ≥ 25% → 100점. 0~25% → 선형. <0% → 0점. */
    private int scoreC(FundamentalSnapshot f, Map<String, Integer> detail,
                       List<String> pos, List<String> neg) {
        if (f == null || f.isStub() || f.revenueGrowthYoy() == null) {
            detail.put("C_revenue_growth", NEUTRAL);
            return NEUTRAL;
        }
        double growth = f.revenueGrowthYoy().doubleValue();
        int score;
        if (growth >= 25.0) {
            score = 100;
        } else if (growth > 0.0) {
            score = (int) Math.round((growth / 25.0) * 100);
        } else {
            score = 0;
        }
        detail.put("C_revenue_growth", score);
        if (score >= 75) pos.add(String.format("매출 성장률 %.1f%% — CAN SLIM 기준(25%%) 달성", growth));
        else if (score >= 40) pos.add(String.format("매출 성장률 %.1f%% — 양호 (기준 25%%에 접근 중)", growth));
        else neg.add(String.format("매출 성장률 %.1f%% — CAN SLIM 기준(25%%) 미달", growth));
        return score;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  A — Annual Earnings (ROA 기반 수익성 지속성)
    // ──────────────────────────────────────────────────────────────────────

    /** A: roa > 20% → 100점. 0~20% → 선형. ≤ 0% → 0점. */
    private int scoreA(FundamentalSnapshot f, Map<String, Integer> detail,
                       List<String> pos, List<String> neg) {
        if (f == null || f.isStub() || f.roa() == null) {
            detail.put("A_roa_quality", NEUTRAL);
            return NEUTRAL;
        }
        double roa = f.roa().doubleValue();
        int score;
        if (roa > 20.0) {
            score = 100;
        } else if (roa > 0.0) {
            score = (int) Math.round((roa / 20.0) * 100);
        } else {
            score = 0;
        }
        detail.put("A_roa_quality", score);
        if (score >= 80) pos.add(String.format("ROA %.2f%% — 수익성 우수 (지속 성장 확인)", roa));
        else if (score >= 40) pos.add(String.format("ROA %.2f%% — 수익성 양호", roa));
        else neg.add(String.format("ROA %.2f%% — 수익성 부족 또는 손실", roa));
        return score;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  N — New High (52주 신고가 근접도)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * N: currentPrice / high52w 비율.
     * ≥ 0.95 → 100점 (신고가 5% 이내).
     * 0.60 ~ 0.95 → 선형 (0~100).
     * < 0.60 → 0점.
     */
    private int scoreN(TechnicalSnapshot t, Map<String, Integer> detail,
                       List<String> pos, List<String> neg) {
        if (t == null || t.currentPrice() == null || t.high52w() == null
                || t.high52w().compareTo(BigDecimal.ZERO) == 0) {
            detail.put("N_new_high_proximity", NEUTRAL);
            return NEUTRAL;
        }
        double ratio = t.currentPrice().doubleValue() / t.high52w().doubleValue();
        int score;
        if (ratio >= 0.95) {
            score = 100;
        } else if (ratio >= 0.60) {
            score = (int) Math.round(((ratio - 0.60) / (0.95 - 0.60)) * 100);
        } else {
            score = 0;
        }
        detail.put("N_new_high_proximity", score);
        double pctFromHigh = (1.0 - ratio) * 100;
        if (score >= 80) pos.add(String.format("52주 신고가 대비 %.1f%% 이내 — 강세 구간", pctFromHigh));
        else if (score >= 40) pos.add(String.format("52주 신고가 대비 %.1f%% — 중간 구간", pctFromHigh));
        else neg.add(String.format("52주 신고가 대비 %.1f%% 하락 — 신고가에서 멀리 이탈", pctFromHigh));
        return score;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  S — Supply/Demand (기관 + 외국인 합산 순매수 방향)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * S: institutionalNetBuy + foreignerNetBuy 합산.
     * 둘 다 양수 → 100점.
     * 합산 양수 → 75점.
     * 합산 음수 → 25점.
     * 둘 다 음수 → 0점.
     */
    private int scoreS(FlowSnapshot flow, Map<String, Integer> detail,
                       List<String> pos, List<String> neg) {
        if (flow == null || flow.isStub()) {
            detail.put("S_supply_demand", NEUTRAL);
            return NEUTRAL;
        }
        Long inst  = flow.institutionalNetBuy();
        Long frgn  = flow.foreignerNetBuy();

        // 둘 다 null이면 stub으로 이미 처리됨 (isStub 체크됨)
        // 한쪽만 null인 경우 0으로 처리
        long instVal = inst  != null ? inst  : 0L;
        long frgnVal = frgn  != null ? frgn  : 0L;

        long combined = instVal + frgnVal;
        int score;
        if (instVal > 0 && frgnVal > 0) {
            score = 100;
        } else if (combined > 0) {
            score = 75;
        } else if (combined < 0 && instVal < 0 && frgnVal < 0) {
            score = 0;
        } else {
            score = 25;
        }
        detail.put("S_supply_demand", score);
        if (score == 100) pos.add(String.format("기관(%,d) + 외국인(%,d) 동반 순매수 — 강한 수급 지지", instVal, frgnVal));
        else if (score == 75) pos.add(String.format("합산 순매수(%,d주) — 수급 우위", combined));
        else if (score == 25) neg.add(String.format("합산 순매도(%,d주) — 수급 압박", combined));
        else neg.add(String.format("기관(%,d) + 외국인(%,d) 동반 순매도 — 강한 매도 압력", instVal, frgnVal));
        return score;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  L — Leader (RS Rating)
    // ──────────────────────────────────────────────────────────────────────

    /** L: rsRating 0~100 그대로 사용. null → 50점. */
    private int scoreL(MomentumSnapshot m, Map<String, Integer> detail,
                       List<String> pos, List<String> neg) {
        if (m == null) {
            detail.put("L_rs_rating", NEUTRAL);
            return NEUTRAL;
        }
        int rs = m.rsRating();
        detail.put("L_rs_rating", rs);
        if (rs >= 80) pos.add(String.format("RS Rating %d — 시장 대비 강한 상대강도 (업종 리더)", rs));
        else if (rs >= 50) pos.add(String.format("RS Rating %d — 시장 대비 중간 상대강도", rs));
        else neg.add(String.format("RS Rating %d — 시장 대비 약한 상대강도 (업종 후발)", rs));
        return rs;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  I — Institutional (기관 단독 순매수 강도)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * I: institutionalNetBuy 단독.
     * > 1,000,000주 → 100점 (대규모 매수).
     * > 0주        → 75점.
     * = 0주        → 50점.
     * < 0주        → 25점.
     * stub         → 50점 중립.
     */
    private int scoreI(FlowSnapshot flow, Map<String, Integer> detail,
                       List<String> pos, List<String> neg) {
        if (flow == null || flow.isStub() || flow.institutionalNetBuy() == null) {
            detail.put("I_institutional_buy", NEUTRAL);
            return NEUTRAL;
        }
        long inst = flow.institutionalNetBuy();
        int score;
        if (inst > 1_000_000L) {
            score = 100;
        } else if (inst > 0) {
            score = 75;
        } else if (inst == 0) {
            score = NEUTRAL;
        } else {
            score = 25;
        }
        detail.put("I_institutional_buy", score);
        if (score == 100) pos.add(String.format("기관 대규모 순매수 (%,d주) — 강한 기관 지지", inst));
        else if (score == 75) pos.add(String.format("기관 순매수 (%,d주) — 기관 매수 우세", inst));
        else if (score == 25) neg.add(String.format("기관 순매도 (%,d주) — 기관 매도 압력", inst));
        return score;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  M — Market Direction (시장 국면)
    // ──────────────────────────────────────────────────────────────────────

    /** M: MarketRegime → 점수 변환. null → 50점 중립. */
    private int scoreM(MarketRegime regime, Map<String, Integer> detail,
                       List<String> pos, List<String> neg) {
        if (regime == null) {
            detail.put("M_market_regime", NEUTRAL);
            return NEUTRAL;
        }
        int score = switch (regime) {
            case STRONG_BULL -> 100;
            case BULL        -> 75;
            case SIDEWAYS    -> 50;
            case BEAR        -> 25;
            case CRISIS      -> 0;
        };
        detail.put("M_market_regime", score);
        if (score >= 75) pos.add("시장 국면 " + regime.name() + " — 상승장 우호적 환경");
        else if (score == 50) pos.add("시장 국면 SIDEWAYS — 중립적 시장 환경");
        else neg.add("시장 국면 " + regime.name() + " — 매수 불리한 환경");
        return score;
    }
}
