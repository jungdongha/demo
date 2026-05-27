package com.obigo.demodong.domain.analysis.domain.calculator;

import com.obigo.demodong.domain.analysis.domain.entity.MagicFormulaRank;
import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.analysis.domain.repository.MagicFormulaRankRepository;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MagicFormulaCalculator implements StrategyCalculator {

    private final MagicFormulaRankRepository rankRepository;
    private static final int NEUTRAL = 50;

    @Override
    public StrategyType getSupportedStrategy() {
        return StrategyType.MAGIC_FORMULA;
    }

    @Override
    public StrategyScore calculate(StrategyInput input) {
        Map<String, Integer> detail = new HashMap<>();
        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();

        if (input.market() == MarketType.USA) {
            detail.put("magic_formula_score", NEUTRAL);
            negatives.add("미국 주식은 Magic Formula를 지원하지 않습니다. (한국 시장 전용)");
            return new StrategyScore(
                    StrategyType.MAGIC_FORMULA,
                    NEUTRAL,
                    StrategyScore.gradeOf(NEUTRAL),
                    Map.copyOf(detail),
                    List.copyOf(positives),
                    List.copyOf(negatives)
            );
        }

        Optional<MagicFormulaRank> rankOpt = rankRepository.findTopByTickerOrderByRankDateDesc(input.ticker());
        if (rankOpt.isEmpty()) {
            detail.put("magic_formula_score", NEUTRAL);
            positives.add("마법공식 순위 데이터 계산 전 (중립 점수 반환)");
            return new StrategyScore(
                    StrategyType.MAGIC_FORMULA,
                    NEUTRAL,
                    StrategyScore.gradeOf(NEUTRAL),
                    Map.copyOf(detail),
                    List.copyOf(positives),
                    List.copyOf(negatives)
            );
        }

        MagicFormulaRank rank = rankOpt.get();
        int universeSize = rank.getUniverseSize();
        int combinedRank = rank.getCombinedRank();

        // [2, universeSize * 2] 범위를 [100, 0] 점수로 변환
        int maxRankVal = universeSize * 2;
        int minRankVal = 2;
        int score;
        if (maxRankVal <= minRankVal) {
            score = 100;
        } else {
            double ratio = (double) (maxRankVal - combinedRank) / (maxRankVal - minRankVal);
            score = (int) Math.round(ratio * 100);
        }
        score = Math.max(0, Math.min(100, score));

        detail.put("magic_formula_score", score);
        detail.put("roic_rank", rank.getRoicRank());
        detail.put("earnings_yield_rank", rank.getEarningsYieldRank());
        detail.put("combined_rank", combinedRank);
        detail.put("universe_size", universeSize);

        positives.add(String.format("마법공식 전체 유니버스 %d개 종목 중 통합 순위 %d위 상위 %d%%",
                universeSize, combinedRank / 2, (int) Math.round(((double) combinedRank / (universeSize * 2)) * 100)));

        if (rank.getRoic() != null) {
            positives.add(String.format("투하자본수익률(ROIC) %.2f%% (순위: %d/%d)",
                    rank.getRoic(), rank.getRoicRank(), universeSize));
        }
        if (rank.getEarningsYield() != null) {
            positives.add(String.format("이익수익률(Earnings Yield) %.2f%% (순위: %d/%d)",
                    rank.getEarningsYield(), rank.getEarningsYieldRank(), universeSize));
        }

        if (score >= 80) {
            positives.add("마법공식 최상위권 종목 — 우수한 수익성과 밸류에이션 매력 동시 보유");
        } else if (score < 40) {
            negatives.add("마법공식 하위권 종목 — 수익성 및 밸류에이션 상대적 매력도 부족");
        }

        return new StrategyScore(
                StrategyType.MAGIC_FORMULA,
                score,
                StrategyScore.gradeOf(score),
                Map.copyOf(detail),
                List.copyOf(positives),
                List.copyOf(negatives)
            );
    }
}
