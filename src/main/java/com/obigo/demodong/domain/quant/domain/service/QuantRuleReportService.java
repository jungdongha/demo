package com.obigo.demodong.domain.quant.domain.service;

import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import com.obigo.demodong.domain.quant.domain.model.QuantScore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * LLM 없이 Quant Feature 점수 기반으로 판단 근거 텍스트를 생성하는 룰 엔진.
 *
 * <p>각 Feature별 임계값(HIGH≥70 / LOW≤30)을 적용해 "근거 후보"를 생성하고,
 * 중립(50점)으로부터 가장 멀리 벗어난 상위 3개를 선별해 ①②③ 형식으로 출력한다.
 */
@Service
public class QuantRuleReportService {

    /**
     * QuantScore로부터 판단 근거 리포트 문자열을 생성한다.
     *
     * @param score 채점 완료된 Quant Score
     * @param rank  TOP 순위 (1~3)
     * @return ①②③ 형식의 판단 근거 + 면책 문구
     */
    public String generate(QuantScore score, int rank) {
        List<ScoredReason> candidates = new ArrayList<>();

        // ── 1. 거래량 배수 ──────────────────────────────────────────────
        double vol = score.volumeRatio5d();
        double volScore = toVolumeScore(vol);
        String volText = buildVolumeText(vol);
        candidates.add(new ScoredReason(volScore, volText));

        // ── 2. 5일 가격 모멘텀 ─────────────────────────────────────────
        double mom = score.priceMomentum5d();
        double momScore = toMomentumScore(mom);
        String momText = buildMomentumText(mom);
        candidates.add(new ScoredReason(momScore, momText));

        // ── 3. 뉴스 신선도 ─────────────────────────────────────────────
        double news = score.newsFreshness();
        candidates.add(new ScoredReason(news, buildNewsText(news)));

        // ── 4. 밸류에이션 ──────────────────────────────────────────────
        double val = score.valuationScore();
        candidates.add(new ScoredReason(val, buildValuationText(val, score.fundamentals())));

        // ── 5. 목표주가 상승여력 ────────────────────────────────────────
        double tp = score.targetPriceUpsideScore();
        candidates.add(new ScoredReason(tp, buildTargetPriceText(tp, score.fundamentals())));

        // ── 6. 섹터 상대 강도 ──────────────────────────────────────────
        double sector = score.sectorRelativeScore();
        candidates.add(new ScoredReason(sector, buildSectorText(sector)));

        // ── 7. 시장 국면 (보정 요소) ────────────────────────────────────
        double regimeScore = toRegimeScore(score.marketRegime());
        candidates.add(new ScoredReason(regimeScore, buildRegimeText(score.marketRegime())));

        // ── 중립(50)에서 가장 먼 TOP3 선별 ─────────────────────────────
        List<String> top3 = candidates.stream()
                .sorted(Comparator.comparingDouble((ScoredReason r) -> Math.abs(r.score() - 50.0)).reversed())
                .limit(3)
                .map(ScoredReason::text)
                .toList();

        return String.format("""
                ① %s
                ② %s
                ③ %s
                ⚠️ 본 시그널은 AI 참고 정보입니다. 투자 판단과 책임은 전적으로 사용자에게 있습니다.""",
                top3.get(0), top3.get(1), top3.get(2));
    }

    // ── 거래량 ────────────────────────────────────────────────────────────

    private double toVolumeScore(double ratio) {
        if (ratio >= 3.0) return 95;
        if (ratio >= 2.0) return 80;
        if (ratio >= 1.5) return 65;
        if (ratio >= 0.8) return 50;
        return 25;
    }

    private String buildVolumeText(double ratio) {
        if (ratio >= 3.0) return String.format("거래량 5일 평균 대비 %.1f배 급증 — 강한 수급 집중 신호", ratio);
        if (ratio >= 2.0) return String.format("거래량 5일 평균 대비 %.1f배 증가 — 수급 뚜렷이 개선", ratio);
        if (ratio >= 1.5) return String.format("거래량 5일 평균 대비 %.1f배 소폭 증가 — 수급 관심 진입", ratio);
        if (ratio >= 0.8) return String.format("거래량 5일 평균 수준 유지(%.1f배) — 중립적 수급", ratio);
        return String.format("거래량 5일 평균 대비 %.1f배 감소 — 수급 위축 주의", ratio);
    }

    // ── 5일 가격 모멘텀 ───────────────────────────────────────────────────

    private double toMomentumScore(double pct) {
        if (pct >= 7) return 90;
        if (pct >= 3) return 72;
        if (pct >= 0) return 55;
        if (pct >= -3) return 35;
        return 15;
    }

    private String buildMomentumText(double pct) {
        if (pct >= 7) return String.format("5일 가격 모멘텀 +%.1f%% — 강한 단기 상승 추세", pct);
        if (pct >= 3) return String.format("5일 가격 모멘텀 +%.1f%% — 완만한 상승 흐름 유지", pct);
        if (pct >= 0) return String.format("5일 가격 모멘텀 +%.1f%% — 약보합 수준", pct);
        if (pct >= -3) return String.format("5일 가격 모멘텀 %.1f%% — 단기 하락 압력", pct);
        return String.format("5일 가격 모멘텀 %.1f%% — 강한 단기 하락, 반등 여부 확인 필요", pct);
    }

    // ── 뉴스 신선도 ───────────────────────────────────────────────────────

    private String buildNewsText(double score) {
        if (score >= 70) return String.format("뉴스 신선도 %.0f점 — 최근 긍정적 뉴스 다수, 모멘텀 양호", score);
        if (score >= 40) return String.format("뉴스 신선도 %.0f점 — 최근 뉴스 보통 수준", score);
        return String.format("뉴스 신선도 %.0f점 — 최근 관련 뉴스 부족 또는 중립적", score);
    }

    // ── 밸류에이션 ────────────────────────────────────────────────────────

    private String buildValuationText(double score, QuantFundamentalData fund) {
        String perStr = (fund != null && fund.per() != null && fund.per().compareTo(BigDecimal.ZERO) > 0)
                ? String.format("PER %.1f", fund.per().doubleValue()) : "PER 미제공";
        String pbrStr = (fund != null && fund.pbr() != null && fund.pbr().compareTo(BigDecimal.ZERO) > 0)
                ? String.format("PBR %.2f", fund.pbr().doubleValue()) : "PBR 미제공";

        if (score >= 70) return String.format("%s / %s — 저평가 구간, 밸류에이션 매력 높음", perStr, pbrStr);
        if (score >= 40) return String.format("%s / %s — 적정 밸류에이션 수준", perStr, pbrStr);
        return String.format("%s / %s — 고평가 우려, 밸류에이션 부담", perStr, pbrStr);
    }

    // ── 목표주가 상승여력 ─────────────────────────────────────────────────

    private String buildTargetPriceText(double score, QuantFundamentalData fund) {
        if (fund == null || fund.targetPrice() == null) {
            return "증권사 목표주가 데이터 미제공 — 업데이트 대기";
        }
        String tpStr = String.format("%,.0f원", fund.targetPrice().doubleValue());
        if (score >= 70) return String.format("증권사 목표주가 %s 대비 큰 상승 여력 — 컨센서스 긍정적", tpStr);
        if (score >= 40) return String.format("증권사 목표주가 %s 수준 — 적정 상승 여력", tpStr);
        return String.format("증권사 목표주가 %s 대비 상승 여력 제한적", tpStr);
    }

    // ── 섹터 상대 강도 ────────────────────────────────────────────────────

    private String buildSectorText(double score) {
        if (score >= 70) return String.format("섹터 상대 강도 %.0f점 — 동일 섹터 대비 뚜렷이 아웃퍼폼", score);
        if (score >= 40) return String.format("섹터 상대 강도 %.0f점 — 동일 섹터 대비 보통 수준", score);
        return String.format("섹터 상대 강도 %.0f점 — 동일 섹터 대비 언더퍼폼, 섹터 내 상대적 약세", score);
    }

    // ── 시장 국면 ─────────────────────────────────────────────────────────

    private double toRegimeScore(MarketRegime regime) {
        return switch (regime) {
            case STRONG_BULL -> 90;
            case BULL -> 70;
            case SIDEWAYS -> 50;
            case BEAR -> 25;
            case CRISIS -> 10;
        };
    }

    private String buildRegimeText(MarketRegime regime) {
        return switch (regime) {
            case STRONG_BULL -> "강세장 국면(STRONG_BULL) — 시장 전반 상승 동력 뚜렷";
            case BULL       -> "완만한 강세(BULL) — 시장 분위기 양호, 공격적 포지션 유리";
            case SIDEWAYS   -> "횡보장(SIDEWAYS) — 시장 방향성 불명확, 선별 종목 중요";
            case BEAR       -> "약세장(BEAR) — 시장 전반 하락 압력, 방어적 접근 권고";
            case CRISIS     -> "위기 국면(CRISIS) — 시그널 신뢰도 낮음, 매우 신중한 접근 필요";
        };
    }

    // ── 내부 레코드 ───────────────────────────────────────────────────────

    private record ScoredReason(double score, String text) {}
}
