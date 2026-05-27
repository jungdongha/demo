package com.obigo.demodong.domain.analysis.calculator;

import com.obigo.demodong.domain.analysis.domain.calculator.MagicFormulaCalculator;
import com.obigo.demodong.domain.analysis.domain.entity.MagicFormulaRank;
import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.analysis.domain.repository.MagicFormulaRankRepository;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("MagicFormulaCalculator 단위 테스트")
class MagicFormulaCalculatorTest {

    private MagicFormulaRankRepository rankRepository;
    private MagicFormulaCalculator calculator;

    @BeforeEach
    void setUp() {
        rankRepository = Mockito.mock(MagicFormulaRankRepository.class);
        calculator = new MagicFormulaCalculator(rankRepository);
    }

    @Nested
    @DisplayName("정상 시나리오")
    class 정상시나리오 {

        @Test
        @DisplayName("최상위 순위(combinedRank=2, universeSize=100)는 100점을 반환한다")
        void 최상위순위_100점() {
            MagicFormulaRank rank = MagicFormulaRank.builder()
                    .ticker("005930")
                    .rankDate(LocalDate.now())
                    .roic(BigDecimal.valueOf(45.5))
                    .earningsYield(BigDecimal.valueOf(15.2))
                    .roicRank(1)
                    .earningsYieldRank(1)
                    .combinedRank(2)
                    .universeSize(100)
                    .build();

            when(rankRepository.findTopByTickerOrderByRankDateDesc("005930"))
                    .thenReturn(Optional.of(rank));

            StrategyInput input = new StrategyInput("005930", MarketType.KOR, null);
            StrategyScore score = calculator.calculate(input);

            assertThat(score.score()).isEqualTo(100);
            assertThat(score.grade()).isEqualTo("S");
            assertThat(score.positives()).anyMatch(s -> s.contains("마법공식 최상위권 종목"));
        }

        @Test
        @DisplayName("중간 순위(combinedRank=100, universeSize=100)는 약 51점을 반환한다")
        void 중간순위_약51점() {
            MagicFormulaRank rank = MagicFormulaRank.builder()
                    .ticker("005930")
                    .rankDate(LocalDate.now())
                    .roic(BigDecimal.valueOf(15.5))
                    .earningsYield(BigDecimal.valueOf(8.2))
                    .roicRank(50)
                    .earningsYieldRank(50)
                    .combinedRank(100)
                    .universeSize(100)
                    .build();

            when(rankRepository.findTopByTickerOrderByRankDateDesc("005930"))
                    .thenReturn(Optional.of(rank));

            StrategyInput input = new StrategyInput("005930", MarketType.KOR, null);
            StrategyScore score = calculator.calculate(input);

            assertThat(score.score()).isEqualTo(51);
            assertThat(score.grade()).isEqualTo("B");
        }

        @Test
        @DisplayName("최하위 순위(combinedRank=200, universeSize=100)는 0점을 반환한다")
        void 최하위순위_0점() {
            MagicFormulaRank rank = MagicFormulaRank.builder()
                    .ticker("005930")
                    .rankDate(LocalDate.now())
                    .roic(BigDecimal.valueOf(2.1))
                    .earningsYield(BigDecimal.valueOf(1.2))
                    .roicRank(100)
                    .earningsYieldRank(100)
                    .combinedRank(200)
                    .universeSize(100)
                    .build();

            when(rankRepository.findTopByTickerOrderByRankDateDesc("005930"))
                    .thenReturn(Optional.of(rank));

            StrategyInput input = new StrategyInput("005930", MarketType.KOR, null);
            StrategyScore score = calculator.calculate(input);

            assertThat(score.score()).isEqualTo(0);
            assertThat(score.grade()).isEqualTo("D");
            assertThat(score.negatives()).anyMatch(s -> s.contains("마법공식 하위권 종목"));
        }
    }

    @Nested
    @DisplayName("예외 및 경계값 시나리오")
    class 예외및경계값시나리오 {

        @Test
        @DisplayName("미국 주식은 50점 중립을 반환한다")
        void 미국주식_50점중립() {
            StrategyInput input = new StrategyInput("AAPL", MarketType.USA, null);
            StrategyScore score = calculator.calculate(input);

            assertThat(score.score()).isEqualTo(50);
            assertThat(score.negatives()).anyMatch(s -> s.contains("미국 주식은 Magic Formula를 지원하지 않습니다"));
        }

        @Test
        @DisplayName("DB에 순위 데이터가 없으면 50점 중립을 반환한다")
        void DB데이터없음_50점중립() {
            when(rankRepository.findTopByTickerOrderByRankDateDesc("005930"))
                    .thenReturn(Optional.empty());

            StrategyInput input = new StrategyInput("005930", MarketType.KOR, null);
            StrategyScore score = calculator.calculate(input);

            assertThat(score.score()).isEqualTo(50);
            assertThat(score.positives()).anyMatch(s -> s.contains("마법공식 순위 데이터 계산 전"));
        }

        @Test
        @DisplayName("유니버스 사이즈가 1인 경우 에러 없이 100점을 반환한다")
        void 유니버스사이즈1_나눗셈안전() {
            MagicFormulaRank rank = MagicFormulaRank.builder()
                    .ticker("005930")
                    .rankDate(LocalDate.now())
                    .roicRank(1)
                    .earningsYieldRank(1)
                    .combinedRank(2)
                    .universeSize(1)
                    .build();

            when(rankRepository.findTopByTickerOrderByRankDateDesc("005930"))
                    .thenReturn(Optional.of(rank));

            StrategyInput input = new StrategyInput("005930", MarketType.KOR, null);
            StrategyScore score = calculator.calculate(input);

            assertThat(score.score()).isEqualTo(100);
        }
    }
}
