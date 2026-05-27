package com.obigo.demodong.domain.analysis.domain.service;

import com.obigo.demodong.domain.analysis.domain.entity.MagicFormulaRank;
import com.obigo.demodong.domain.analysis.domain.entity.MagicFormulaUniverse;
import com.obigo.demodong.domain.analysis.domain.repository.MagicFormulaRankRepository;
import com.obigo.demodong.domain.analysis.domain.repository.MagicFormulaUniverseRepository;
import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.fundamental.domain.port.FundamentalDataPort;
import com.obigo.demodong.domain.price.infrastructure.dart.DartCorpCodeMapper;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MagicFormulaBatchService {

    private final MagicFormulaUniverseRepository universeRepository;
    private final MagicFormulaRankRepository rankRepository;
    private final StockRepository stockRepository;
    private final FundamentalDataPort fundamentalDataPort;
    private final DartCorpCodeMapper dartCorpCodeMapper;

    /**
     * 매 거래일 18시에 전체 종목 순위 계산 및 저장 실행.
     */
    @Scheduled(cron = "0 0 18 * * MON-FRI")
    @Transactional
    public void runDailyRanking() {
        LocalDate today = LocalDate.now();
        log.info("[Magic-Formula] 일일 마법공식 순위 계산 배치 시작 - 기준일: {}", today);
        calculateAndSaveRanks(today);
    }

    /**
     * 특정 날짜 기준으로 순위를 계산하고 저장하는 실질적인 로직 (수동 실행/배치 공동 사용)
     */
    @Transactional
    public void calculateAndSaveRanks(LocalDate date) {
        List<MagicFormulaUniverse> universe = universeRepository.findAllByActiveTrue();
        if (universe.isEmpty()) {
            log.warn("[Magic-Formula] 활성화된 유니버스 종목이 없습니다. 배치 종료.");
            return;
        }

        int universeSize = universe.size();
        List<RawMetric> rawMetrics = new ArrayList<>();

        for (MagicFormulaUniverse item : universe) {
            String ticker = item.getTicker();
            try {
                // Stock 정보 조회 및 생성/매핑
                Stock stock = getOrCreateStock(ticker, item.getCompanyName());
                
                // 재무 스냅샷 조회
                FundamentalSnapshot snapshot = fundamentalDataPort.fetch(
                        ticker,
                        stock.getDartCorpCode(),
                        stock.getMarketType()
                );

                rawMetrics.add(new RawMetric(ticker, snapshot.roic(), snapshot.earningsYield()));
            } catch (Exception e) {
                log.warn("[Magic-Formula] 재무 지표 수집 실패 - ticker: {}, error: {}", ticker, e.getMessage());
                rawMetrics.add(new RawMetric(ticker, null, null));
            }
        }

        // 1. ROIC 정렬 및 순위 매기기 (내림차순, 큰 값이 1등)
        rawMetrics.sort((a, b) -> {
            if (a.roic == null && b.roic == null) return 0;
            if (a.roic == null) return 1; // null을 뒤로 보냄
            if (b.roic == null) return -1;
            return b.roic.compareTo(a.roic);
        });

        Map<String, Integer> roicRanks = new HashMap<>();
        for (int i = 0; i < rawMetrics.size(); i++) {
            RawMetric metric = rawMetrics.get(i);
            int rank = (metric.roic == null) ? universeSize : (i + 1);
            roicRanks.put(metric.ticker, rank);
        }

        // 2. EarningsYield 정렬 및 순위 매기기 (내림차순, 큰 값이 1등)
        rawMetrics.sort((a, b) -> {
            if (a.earningsYield == null && b.earningsYield == null) return 0;
            if (a.earningsYield == null) return 1;
            if (b.earningsYield == null) return -1;
            return b.earningsYield.compareTo(a.earningsYield);
        });

        Map<String, Integer> eyRanks = new HashMap<>();
        for (int i = 0; i < rawMetrics.size(); i++) {
            RawMetric metric = rawMetrics.get(i);
            int rank = (metric.earningsYield == null) ? universeSize : (i + 1);
            eyRanks.put(metric.ticker, rank);
        }

        // 3. 순위 병합 및 저장
        for (RawMetric metric : rawMetrics) {
            String ticker = metric.ticker;
            int roicRank = roicRanks.get(ticker);
            int eyRank = eyRanks.get(ticker);
            int combined = roicRank + eyRank;

            // 기존 순위 데이터 존재 시 삭제 (Unique 제약 조건 충돌 방지)
            rankRepository.findByTickerAndRankDate(ticker, date)
                    .ifPresent(rankRepository::delete);

            MagicFormulaRank rankEntity = MagicFormulaRank.builder()
                    .ticker(ticker)
                    .rankDate(date)
                    .roic(metric.roic)
                    .earningsYield(metric.earningsYield)
                    .roicRank(roicRank)
                    .earningsYieldRank(eyRank)
                    .combinedRank(combined)
                    .universeSize(universeSize)
                    .build();

            rankRepository.save(rankEntity);
        }

        log.info("[Magic-Formula] 순위 계산 완료 - 총 {}개 종목 저장됨", universeSize);
    }

    private Stock getOrCreateStock(String ticker, String companyName) {
        return stockRepository.findByTicker(ticker).orElseGet(() -> {
            log.info("[Magic-Formula] 유니버스 종목이 Stock 테이블에 없어 신규 추가합니다 - ticker: {}", ticker);
            String corpCode = dartCorpCodeMapper.resolveCorpCodeByTicker(ticker).orElse(null);
            Stock newStock = Stock.builder()
                    .ticker(ticker)
                    .name(companyName)
                    .marketType(MarketType.KOR)
                    .isWatchlist(true)
                    .dartCorpCode(corpCode)
                    .build();
            return stockRepository.save(newStock);
        });
    }

    @PostConstruct
    @Transactional
    public void initUniverse() {
        if (universeRepository.count() > 0) {
            log.info("[Magic-Formula] 유니버스 초기 데이터가 이미 존재합니다.");
            return;
        }

        log.info("[Magic-Formula] 유니버스 초기 대형주 데이터 구축 시작");
        Map<String, String> initialUniverse = Map.of(
                "005930", "삼성전자",
                "000660", "SK하이닉스",
                "035420", "NAVER",
                "005380", "현대자동차",
                "051910", "LG화학",
                "006400", "삼성SDI",
                "003550", "LG",
                "028260", "삼성물산",
                "207940", "삼성바이오로직스",
                "068270", "셀트리온"
        );

        for (Map.Entry<String, String> entry : initialUniverse.entrySet()) {
            MagicFormulaUniverse universeItem = MagicFormulaUniverse.builder()
                    .ticker(entry.getKey())
                    .companyName(entry.getValue())
                    .active(true)
                    .build();
            universeRepository.save(universeItem);
            
            // Stock 테이블에도 연계 등록 보장
            getOrCreateStock(entry.getKey(), entry.getValue());
        }
        log.info("[Magic-Formula] 유니버스 초기 대형주 데이터 {}건 구축 완료", initialUniverse.size());
    }

    private record RawMetric(String ticker, BigDecimal roic, BigDecimal earningsYield) {}
}
