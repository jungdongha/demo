package com.obigo.demodong.domain.quant.infrastructure;

import com.obigo.demodong.domain.quant.domain.entity.QuantUniverse;
import com.obigo.demodong.domain.quant.domain.repository.QuantUniverseRepository;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 앱 시작 시 quant_universe 테이블이 비어있으면 KOR 10 + USA 10 초기 데이터를 삽입한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuantUniverseInitializer {

    private final QuantUniverseRepository quantUniverseRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (quantUniverseRepository.countByActiveTrue() > 0) {
            log.info("[QuantUniverseInit] 유니버스 데이터 이미 존재 — 초기화 건너뜀");
            return;
        }

        log.info("[QuantUniverseInit] 유니버스 초기 데이터 삽입 시작 (KOR 10 + USA 10)");
        quantUniverseRepository.saveAll(buildInitialUniverse());
        log.info("[QuantUniverseInit] 유니버스 초기화 완료");
    }

    private List<QuantUniverse> buildInitialUniverse() {
        return List.of(
                // ─── KOR TOP10 ───
                QuantUniverse.create("005930", "삼성전자",         MarketType.KOR, 1,  "반도체"),
                QuantUniverse.create("000660", "SK하이닉스",       MarketType.KOR, 2,  "반도체"),
                QuantUniverse.create("005380", "현대차",           MarketType.KOR, 3,  "자동차"),
                QuantUniverse.create("035420", "NAVER",           MarketType.KOR, 4,  "IT"),
                QuantUniverse.create("051910", "LG화학",           MarketType.KOR, 5,  "화학"),
                QuantUniverse.create("006400", "삼성SDI",          MarketType.KOR, 6,  "배터리"),
                QuantUniverse.create("207940", "삼성바이오로직스", MarketType.KOR, 7,  "바이오"),
                QuantUniverse.create("005490", "POSCO홀딩스",      MarketType.KOR, 8,  "철강"),
                QuantUniverse.create("035720", "카카오",           MarketType.KOR, 9,  "IT"),
                QuantUniverse.create("000270", "기아",             MarketType.KOR, 10, "자동차"),

                // ─── USA TOP10 ───
                QuantUniverse.create("AAPL",  "Apple",      MarketType.USA, 1,  "IT"),
                QuantUniverse.create("MSFT",  "Microsoft",  MarketType.USA, 2,  "IT"),
                QuantUniverse.create("NVDA",  "NVIDIA",     MarketType.USA, 3,  "반도체"),
                QuantUniverse.create("AMZN",  "Amazon",     MarketType.USA, 4,  "커머스"),
                QuantUniverse.create("GOOGL", "Alphabet",   MarketType.USA, 5,  "IT"),
                QuantUniverse.create("META",  "Meta",       MarketType.USA, 6,  "소셜미디어"),
                QuantUniverse.create("TSLA",  "Tesla",      MarketType.USA, 7,  "자동차"),
                QuantUniverse.create("BRK.B", "Berkshire",  MarketType.USA, 8,  "금융"),
                QuantUniverse.create("LLY",   "Eli Lilly",  MarketType.USA, 9,  "바이오"),
                QuantUniverse.create("JPM",   "JPMorgan",   MarketType.USA, 10, "금융")
        );
    }
}
