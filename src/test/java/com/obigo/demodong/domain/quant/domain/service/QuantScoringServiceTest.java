package com.obigo.demodong.domain.quant.domain.service;

import com.obigo.demodong.domain.quant.domain.calculator.*;
import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import com.obigo.demodong.domain.quant.domain.model.QuantScore;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuantScoringService 단위 테스트")
class QuantScoringServiceTest {

    private QuantScoringService quantScoringService;
    private MarketRegimeService marketRegimeService;
    private RiskFilterService riskFilterService;

    @BeforeEach
    void setUp() {
        marketRegimeService = new MarketRegimeService();
        List<QuantFeatureCalculator> calculators = List.of(
                new VolumeRatio5dCalculator(),
                new PriceMomentum5dCalculator(),
                new NewsFreshnessCalculator(),
                new MarketRegimeScoreCalculator(marketRegimeService),
                new ValuationScoreCalculator(),
                new TargetPriceUpsideCalculator(),
                new SectorRelativeStrengthCalculator()
        );
        quantScoringService = new QuantScoringService(calculators, marketRegimeService);
        riskFilterService = new RiskFilterService();
    }

    @Nested
    @DisplayName("Quant Score 계산")
    class ScoreCalculation {

        @Test
        @DisplayName("거래량이 5일 평균의 3배이면 volume 점수가 최대에 가깝다")
        void 거래량_급증시_점수가_높다() {
            // given
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(60000), 3_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(1), BigDecimal.valueOf(59000), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(2), BigDecimal.valueOf(58500), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(3), BigDecimal.valueOf(58000), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(57500), 1_000_000L)
            );
            QuantFeatureInput input = new QuantFeatureInput("005930", "삼성전자", MarketType.KOR,
                    prices, List.of("뉴스1", "뉴스2"), 1.5, null, 0.0);

            // when
            QuantScore score = quantScoringService.score(input);

            // then
            assertThat(score.ticker()).isEqualTo("005930");
            assertThat(score.totalScore()).isGreaterThan(30.0);
            assertThat(score.volumeRatio5d()).isGreaterThan(1.5);
            assertThat(score.excluded()).isFalse();
        }

        @Test
        @DisplayName("BULL 국면에서는 market_regime_bonus +5점이 적용된다")
        void BULL_국면에서_보너스_점수가_적용된다() {
            // given
            List<DailyQuote> prices = buildFlatPrices(50000L, 5);
            QuantFeatureInput inputBull     = new QuantFeatureInput("AAPL", "Apple", MarketType.USA, prices, List.of(), 1.0, null, 0.0);
            QuantFeatureInput inputSideways = new QuantFeatureInput("AAPL", "Apple", MarketType.USA, prices, List.of(), -1.0, null, 0.0);

            // when
            QuantScore scoreBull     = quantScoringService.score(inputBull);
            QuantScore scoreSideways = quantScoringService.score(inputSideways);

            // then: BULL이 SIDEWAYS보다 높아야 함
            assertThat(scoreBull.totalScore()).isGreaterThan(scoreSideways.totalScore());
            assertThat(scoreBull.marketRegime()).isEqualTo(MarketRegime.BULL);
            assertThat(scoreSideways.marketRegime()).isEqualTo(MarketRegime.SIDEWAYS);
        }

        @Test
        @DisplayName("총점은 0~100 범위를 벗어나지 않는다")
        void 점수는_0에서_100_사이다() {
            // given
            List<DailyQuote> prices = buildFlatPrices(100000L, 5);
            List<String> lotsOfNews = List.of("뉴스1","뉴스2","뉴스3","뉴스4","뉴스5",
                    "뉴스6","뉴스7","뉴스8","뉴스9","뉴스10","뉴스11");
            QuantFundamentalData lowValuation = new QuantFundamentalData(
                    BigDecimal.valueOf(5.0), BigDecimal.valueOf(0.8), null, BigDecimal.valueOf(50000));
            QuantFeatureInput input = new QuantFeatureInput("NVDA", "NVIDIA", MarketType.USA,
                    prices, lotsOfNews, 5.0, lowValuation, 0.0); // STRONG_BULL + 저평가

            // when
            QuantScore score = quantScoringService.score(input);

            // then
            assertThat(score.totalScore()).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("저평가 종목(PER=8, PBR=0.9)은 valuationScore가 높다")
        void 저평가_종목은_밸류에이션_점수가_높다() {
            // given
            List<DailyQuote> prices = buildFlatPrices(50000L, 5);
            QuantFundamentalData lowValuation = new QuantFundamentalData(
                    BigDecimal.valueOf(8.0), BigDecimal.valueOf(0.9), null, BigDecimal.valueOf(50000));
            QuantFeatureInput input = new QuantFeatureInput("005930", "삼성전자", MarketType.KOR,
                    prices, List.of(), 0.0, lowValuation, 0.0);

            // when
            QuantScore score = quantScoringService.score(input);

            // then
            assertThat(score.valuationScore()).isGreaterThan(90.0);
        }

        @Test
        @DisplayName("섹터 평균보다 5일 모멘텀이 높으면 sectorRelativeScore가 50점 초과이다")
        void 섹터_강자_종목은_상대강도_점수가_높다() {
            // given — 종목 +5% 상승, 섹터 평균 -2% (sector avg)
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(52500), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(50000), 1_000_000L)
            );
            QuantFeatureInput input = new QuantFeatureInput("005930", "삼성전자", MarketType.KOR,
                    prices, List.of(), 0.0, null, -2.0); // sectorAvg = -2%

            // when
            QuantScore score = quantScoringService.score(input);

            // then: selfMomentum(+5%) > sectorAvg(-2%) → relative=+7% → score > 50
            assertThat(score.sectorRelativeScore()).isGreaterThan(50.0);
        }
    }

    @Nested
    @DisplayName("Risk Filter")
    class RiskFilterTests {

        @Test
        @DisplayName("거래량 배수가 0.3 미만이면 유동성 필터로 제외된다")
        void 유동성_부족_종목은_제외된다() {
            // given — 오늘 거래량 100, 5일 평균 1000 → ratio 0.1
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(50000), 100L),
                    new DailyQuote(LocalDate.now().minusDays(1), BigDecimal.valueOf(50100), 1000L),
                    new DailyQuote(LocalDate.now().minusDays(2), BigDecimal.valueOf(50200), 1000L),
                    new DailyQuote(LocalDate.now().minusDays(3), BigDecimal.valueOf(50300), 1000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(50400), 1000L)
            );
            QuantFeatureInput input = new QuantFeatureInput("000270", "기아", MarketType.KOR,
                    prices, List.of(), 0.0, null, 0.0);
            QuantScore score = quantScoringService.score(input);

            // when
            List<QuantScore> filtered = riskFilterService.filter(List.of(score));

            // then
            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0).excluded()).isTrue();
            assertThat(filtered.get(0).excludeReason()).contains("유동성");
        }

        @Test
        @DisplayName("5일 모멘텀 20% 초과면 선반영 필터로 제외된다")
        void 급등_선반영_종목은_제외된다() {
            // given — 5일 전 대비 +25%
            List<DailyQuote> prices = List.of(
                    new DailyQuote(LocalDate.now(),              BigDecimal.valueOf(62500), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(1), BigDecimal.valueOf(60000), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(2), BigDecimal.valueOf(58000), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(3), BigDecimal.valueOf(55000), 1_000_000L),
                    new DailyQuote(LocalDate.now().minusDays(4), BigDecimal.valueOf(50000), 1_000_000L)
            );
            QuantFeatureInput input = new QuantFeatureInput("035420", "NAVER", MarketType.KOR,
                    prices, List.of(), 0.0, null, 0.0);
            QuantScore score = quantScoringService.score(input);

            // when
            List<QuantScore> filtered = riskFilterService.filter(List.of(score));

            // then
            assertThat(filtered.get(0).excluded()).isTrue();
            assertThat(filtered.get(0).excludeReason()).contains("선반영");
        }

        @Test
        @DisplayName("CRISIS 국면에서 analyze가 CRISIS를 반환한다")
        void CRISIS_국면_판단() {
            assertThat(marketRegimeService.analyze(-6.0)).isEqualTo(MarketRegime.CRISIS);
        }
    }

    @Nested
    @DisplayName("TOP3 선별")
    class Top3Selection {

        @Test
        @DisplayName("필터 통과 종목이 3개 미만이면 가능한 수만큼 반환한다")
        void 필터통과_2개면_2개만_반환한다() {
            // given
            List<DailyQuote> goodPrices = buildFlatPrices(1_000_000L, 5);
            QuantFeatureInput input1 = new QuantFeatureInput("005930", "삼성전자", MarketType.KOR, goodPrices, List.of("뉴스"), 1.0, null, 0.0);
            QuantFeatureInput input2 = new QuantFeatureInput("000660", "SK하이닉스", MarketType.KOR, goodPrices, List.of("뉴스"), 1.0, null, 0.0);

            QuantScore score1 = quantScoringService.score(input1);
            QuantScore score2 = quantScoringService.score(input2);

            // when
            List<QuantScore> top3 = quantScoringService.selectTop3(List.of(score1, score2));

            // then
            assertThat(top3.size()).isLessThanOrEqualTo(2);
            assertThat(top3).allMatch(s -> !s.excluded());
        }

        @Test
        @DisplayName("점수 높은 종목이 먼저 선정된다")
        void 점수_높은_종목이_상위에_오른다() {
            // given — 저평가 종목 vs 고평가 종목
            List<DailyQuote> prices = buildFlatPrices(1_000_000L, 5);
            QuantFundamentalData lowVal = new QuantFundamentalData(
                    BigDecimal.valueOf(5.0), BigDecimal.valueOf(0.5), null, BigDecimal.valueOf(50000));
            QuantFundamentalData highVal = new QuantFundamentalData(
                    BigDecimal.valueOf(50.0), BigDecimal.valueOf(5.0), null, BigDecimal.valueOf(50000));

            QuantFeatureInput lowValInput  = new QuantFeatureInput("AAPL", "Apple", MarketType.USA, prices, List.of("n1","n2","n3","n4","n5","n6","n7","n8","n9","n10"), 1.0, lowVal, 0.0);
            QuantFeatureInput highValInput = new QuantFeatureInput("MSFT", "Microsoft", MarketType.USA, prices, List.of(), 1.0, highVal, 0.0);

            QuantScore lowValScore  = quantScoringService.score(lowValInput);
            QuantScore highValScore = quantScoringService.score(highValInput);

            // when
            List<QuantScore> top3 = quantScoringService.selectTop3(List.of(highValScore, lowValScore));

            // then
            assertThat(top3.get(0).ticker()).isEqualTo("AAPL"); // 저평가+뉴스 많은 종목이 1위
        }
    }

    // ─── helpers ───

    private List<DailyQuote> buildFlatPrices(long volume, int days) {
        return java.util.stream.IntStream.range(0, days)
                .mapToObj(i -> new DailyQuote(
                        LocalDate.now().minusDays(i),
                        BigDecimal.valueOf(50000),
                        volume))
                .toList();
    }
}
