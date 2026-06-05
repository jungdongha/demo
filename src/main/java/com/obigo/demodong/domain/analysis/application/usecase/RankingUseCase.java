package com.obigo.demodong.domain.analysis.application.usecase;

import com.obigo.demodong.domain.analysis.application.dto.response.RankingEntry;
import com.obigo.demodong.domain.analysis.domain.entity.AnalysisHistory;
import com.obigo.demodong.domain.analysis.domain.repository.AnalysisHistoryRepository;
import com.obigo.demodong.domain.analysis.domain.service.RankingService;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 전략별 랭킹 조회 유스케이스.
 *
 * <p>분석 히스토리(analysis_history)에서 ticker별 최신 레코드를 가져와
 * 지정된 전략 점수 기준 내림차순 정렬 후 상위 N개를 반환한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingUseCase {

    private final AnalysisHistoryRepository analysisHistoryRepository;
    private final RankingService rankingService;
    private final StockReader stockReader;

    /**
     * @param strategy 전략 키 (canslim / momentum / seasonality / minervini / magic / piotroski / reversion / total)
     * @param limit    상위 N개 (1~50, 기본 10)
     */
    @Cacheable(value = "ranking", key = "#strategy + ':' + #limit")
    public List<RankingEntry> getRanking(String strategy, int limit) {
        rankingService.validateStrategy(strategy);

        List<AnalysisHistory> latest = analysisHistoryRepository.findLatestPerTicker();
        log.debug("[Ranking] 전체 ticker수: {}, 전략: {}", latest.size(), strategy);

        List<AnalysisHistory> sorted = rankingService.sort(latest, strategy);

        int safeLimit = Math.min(limit, sorted.size());
        List<RankingEntry> result = new ArrayList<>(safeLimit);

        for (int i = 0; i < safeLimit; i++) {
            AnalysisHistory h = sorted.get(i);
            String name = stockReader.findByTicker(h.getTicker())
                    .map(s -> s.getName())
                    .orElse(h.getTicker());
            result.add(new RankingEntry(i + 1, h.getTicker(), name,
                    rankingService.extractScore(h, strategy)));
        }

        return result;
    }
}
