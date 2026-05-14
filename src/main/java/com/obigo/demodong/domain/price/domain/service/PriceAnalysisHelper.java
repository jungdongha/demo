package com.obigo.demodong.domain.price.domain.service;

import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * 주가 스냅샷 목록에서 AI 프롬프트용 통계 지표를 계산하는 유틸 컴포넌트.
 *
 * <p>계산 지표:
 * <ul>
 *   <li>MA20 및 현재가 대비 이격률 (괴리도 과열 판단)</li>
 *   <li>20일 최고/최저가</li>
 *   <li>추세 레이블 (최근 5일 vs 이전 5일 평균 비교)</li>
 *   <li>변동성 (표준편차/평균)</li>
 *   <li>최근 5일 등락률 모멘텀</li>
 *   <li>거래량 지표 — 최근 5일 평균 대비 전일 거래량 증감률</li>
 *   <li>52주 고/저가 대비 현재 위치 (제공 시)</li>
 * </ul>
 */
@Component
public class PriceAnalysisHelper {

    /**
     * @param snapshots    날짜 오름차순 스냅샷 목록
     * @param w52Range     52주 [고가, 저가] 배열. Optional.empty()면 미출력.
     */
    public String buildPriceContext(List<PriceSnapshot> snapshots,
                                    Optional<BigDecimal[]> w52Range) {
        if (snapshots == null || snapshots.isEmpty()) {
            return "주가 데이터 없음";
        }

        List<BigDecimal> prices = snapshots.stream()
                .map(PriceSnapshot::getClosePrice)
                .toList();

        BigDecimal latest   = prices.get(prices.size() - 1);
        BigDecimal ma20     = calcAverage(prices);
        BigDecimal ma20Gap  = calcGapPercent(latest, ma20);  // 이격률

        BigDecimal high20   = prices.stream().max(BigDecimal::compareTo).orElse(latest);
        BigDecimal low20    = prices.stream().min(BigDecimal::compareTo).orElse(latest);

        String trend        = detectTrend(prices);
        String volatility   = detectVolatility(prices, ma20);
        String momentum5    = calcRecentMomentum(prices);
        String volumeInfo   = calcVolumeInfo(snapshots);
        String w52Info      = build52WeekInfo(latest, w52Range);
        String rawLast10    = buildRawLines(snapshots);

        // 이격률 경고 레이블
        String gapWarning = "";
        if (ma20Gap.compareTo(BigDecimal.valueOf(10)) >= 0) {
            gapWarning = "  ⚠️ MA20 이격률 " + ma20Gap + "% — 단기 과열 구간";
        } else if (ma20Gap.compareTo(BigDecimal.valueOf(-10)) <= 0) {
            gapWarning = "  ⚠️ MA20 이격률 " + ma20Gap + "% — 단기 과매도 구간";
        }

        return String.format(
                """
                현재가: %s
                20일 평균(MA20): %s  (이격률: %s%%)%s
                20일 최고: %s / 최저: %s
                %s
                추세: %s  |  변동성: %s
                최근 5일 등락률: %s
                거래량: %s

                [최근 10거래일 종가]
                %s""",
                latest,
                ma20, ma20Gap, gapWarning,
                high20, low20,
                w52Info,
                trend, volatility,
                momentum5,
                volumeInfo,
                rawLast10
        );
    }

    // ──────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────

    private BigDecimal calcAverage(List<BigDecimal> prices) {
        if (prices.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(prices.size()), 0, RoundingMode.HALF_UP);
    }

    /** (target - base) / base × 100, 소수점 2자리 */
    private BigDecimal calcGapPercent(BigDecimal target, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return target.subtract(base)
                .divide(base, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 추세 판단 — 최근 5일 평균과 이전 5일 평균 비교.
     * +2% 이상 → 상승, -2% 이하 → 하락, 그 사이 → 횡보
     */
    private String detectTrend(List<BigDecimal> prices) {
        if (prices.size() < 10) return "데이터 부족";
        int n = prices.size();
        BigDecimal recentAvg = calcAverage(prices.subList(n - 5, n));
        BigDecimal prevAvg   = calcAverage(prices.subList(n - 10, n - 5));
        BigDecimal changeRate = calcGapPercent(recentAvg, prevAvg);

        if (changeRate.compareTo(BigDecimal.valueOf(2)) >= 0)  return "📈 상승 (" + changeRate + "%)";
        if (changeRate.compareTo(BigDecimal.valueOf(-2)) <= 0) return "📉 하락 (" + changeRate + "%)";
        return "➡️ 횡보 (" + changeRate + "%)";
    }

    /**
     * 변동성 — 표준편차/평균.
     * 5% 초과 → 고변동성, 2~5% → 보통, 2% 미만 → 저변동성
     */
    private String detectVolatility(List<BigDecimal> prices, BigDecimal mean) {
        if (prices.size() < 5) return "데이터 부족";
        double meanD = mean.doubleValue();
        double variance = prices.stream()
                .mapToDouble(p -> Math.pow(p.doubleValue() - meanD, 2))
                .average().orElse(0);
        double ratio = meanD == 0 ? 0 : Math.sqrt(variance) / meanD * 100;

        if (ratio > 5) return String.format("⚡ 고변동성 (%.1f%%)", ratio);
        if (ratio > 2) return String.format("보통 (%.1f%%)", ratio);
        return String.format("🔵 저변동성 (%.1f%%)", ratio);
    }

    /** 최근 5거래일 전일 대비 등락률 */
    private String calcRecentMomentum(List<BigDecimal> prices) {
        if (prices.size() < 6) return "데이터 부족";
        int n = prices.size();
        StringBuilder sb = new StringBuilder();
        for (int i = n - 5; i < n; i++) {
            BigDecimal rate = calcGapPercent(prices.get(i), prices.get(i - 1));
            sb.append(rate.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "").append(rate).append("%");
            if (i < n - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * 거래량 분석 — 최근 5일 평균 대비 전일 거래량.
     * volume이 null인 스냅샷이 많으면 "거래량 데이터 없음" 반환.
     */
    private String calcVolumeInfo(List<PriceSnapshot> snapshots) {
        int n = snapshots.size();
        if (n < 6) return "거래량 데이터 부족";

        // 전일(가장 최신)
        Long latestVol = snapshots.get(n - 1).getVolume();
        if (latestVol == null) return "거래량 데이터 없음";

        // 최근 5일(전일 포함) 평균
        long validCount = 0;
        long sum = 0;
        for (int i = n - 5; i < n; i++) {
            Long v = snapshots.get(i).getVolume();
            if (v != null) { sum += v; validCount++; }
        }
        if (validCount < 3) return "거래량 데이터 부족";

        long avg5 = sum / validCount;
        if (avg5 == 0) return "거래량 데이터 없음";

        BigDecimal changeRate = calcGapPercent(
                BigDecimal.valueOf(latestVol), BigDecimal.valueOf(avg5));

        String label;
        if (changeRate.compareTo(BigDecimal.valueOf(50)) >= 0) {
            label = "🔥 급증 — 수급 신호 강함";
        } else if (changeRate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            label = "↑ 증가 — 관심 유입";
        } else if (changeRate.compareTo(BigDecimal.valueOf(-20)) <= 0) {
            label = "↓ 감소 — 거래 위축 (모멘텀 신뢰도 낮음)";
        } else {
            label = "보통";
        }

        return String.format("전일 %,d주 / 5일평균 %,d주 / 증감 %s%%  → %s",
                latestVol, avg5,
                changeRate.compareTo(BigDecimal.ZERO) >= 0 ? "+" + changeRate : changeRate,
                label);
    }

    /** 52주 고/저가 대비 현재 위치 계산 */
    private String build52WeekInfo(BigDecimal current, Optional<BigDecimal[]> w52Range) {
        if (w52Range.isEmpty()) return "52주 고/저가: 미지원 (해외 종목)";
        BigDecimal high52 = w52Range.get()[0];
        BigDecimal low52  = w52Range.get()[1];

        BigDecimal fromHigh = calcGapPercent(current, high52);  // 음수 → 고점 대비 하락
        BigDecimal fromLow  = calcGapPercent(current, low52);   // 양수 → 저점 대비 상승

        String positionLabel;
        if (fromHigh.abs().compareTo(BigDecimal.valueOf(5)) <= 0) {
            positionLabel = "🔝 52주 신고가 근처 (저항선 돌파 여부 주목)";
        } else if (fromLow.compareTo(BigDecimal.valueOf(20)) <= 0) {
            positionLabel = "🔻 52주 저점 근처 (바닥권 — 반등 기대 or 추가 하락 리스크)";
        } else {
            positionLabel = "중간 구간";
        }

        return String.format("52주 고가: %s (현재 대비 %s%%) / 저가: %s (현재 대비 +%s%%)  → %s",
                high52, fromHigh, low52, fromLow, positionLabel);
    }

    /** 마지막 10거래일 원시 종가 (날짜: 종가) */
    private String buildRawLines(List<PriceSnapshot> snapshots) {
        int from = Math.max(0, snapshots.size() - 10);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < snapshots.size(); i++) {
            PriceSnapshot s = snapshots.get(i);
            sb.append(s.getRecordedDate()).append(": ").append(s.getClosePrice());
            if (s.getVolume() != null) sb.append("  (거래량: ").append(String.format("%,d", s.getVolume())).append(")");
            if (i < snapshots.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
