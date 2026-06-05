package com.obigo.demodong.domain.analysis.service;

import com.obigo.demodong.domain.analysis.application.dto.request.ScreeningRequest;
import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisResponse;
import com.obigo.demodong.domain.analysis.application.dto.response.ScreeningResult;
import com.obigo.demodong.domain.analysis.application.dto.response.StrategyScoreResponse;
import com.obigo.demodong.domain.analysis.domain.service.ScreeningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScreeningService 단위 테스트")
class ScreeningServiceTest {

    private ScreeningService screeningService;

    @BeforeEach
    void setUp() {
        screeningService = new ScreeningService();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  정상 케이스
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("스크리닝 필터 정상 동작")
    class 정상동작 {

        @Test
        @DisplayName("단일 전략 조건 만족 종목만 반환한다")
        void 단일조건_통과종목만_반환() {
            AnalysisResponse high = makeAnalysis("005930", "삼성전자", Map.of("CANSLIM", 80, "PIOTROSKI", 70));
            AnalysisResponse low  = makeAnalysis("000660", "SK하이닉스", Map.of("CANSLIM", 40, "PIOTROSKI", 60));

            ScreeningRequest request = new ScreeningRequest(70, null, null, null, null, null, null, null);
            List<ScreeningResult> results = screeningService.filter(List.of(high, low), request);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).ticker()).isEqualTo("005930");
        }

        @Test
        @DisplayName("복수 전략 AND 조건 — 모두 만족하는 종목만 통과한다")
        void 복수조건_AND_모두만족종목만_통과() {
            AnalysisResponse both   = makeAnalysis("005930", "삼성전자",   Map.of("CANSLIM", 75, "PIOTROSKI", 65));
            AnalysisResponse onlyA  = makeAnalysis("000660", "SK하이닉스", Map.of("CANSLIM", 75, "PIOTROSKI", 40));
            AnalysisResponse onlyB  = makeAnalysis("035420", "NAVER",    Map.of("CANSLIM", 30, "PIOTROSKI", 65));

            ScreeningRequest request = new ScreeningRequest(70, null, null, null, null, 60, null, null);
            List<ScreeningResult> results = screeningService.filter(List.of(both, onlyA, onlyB), request);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).ticker()).isEqualTo("005930");
        }

        @Test
        @DisplayName("결과는 totalScore 내림차순으로 정렬된다")
        void 결과_totalScore_내림차순_정렬() {
            AnalysisResponse a = makeAnalysis("A", "종목A", Map.of("CANSLIM", 80, "PIOTROSKI", 70));
            AnalysisResponse b = makeAnalysis("B", "종목B", Map.of("CANSLIM", 90, "PIOTROSKI", 80));
            AnalysisResponse c = makeAnalysis("C", "종목C", Map.of("CANSLIM", 70, "PIOTROSKI", 65));

            ScreeningRequest request = new ScreeningRequest(50, null, null, null, null, 50, null, null);
            List<ScreeningResult> results = screeningService.filter(List.of(a, b, c), request);

            assertThat(results).hasSize(3);
            assertThat(results.get(0).ticker()).isEqualTo("B");   // 85점
            assertThat(results.get(1).ticker()).isEqualTo("A");   // 75점
            assertThat(results.get(2).ticker()).isEqualTo("C");   // 67점
        }

        @Test
        @DisplayName("passedStrategies에 조건을 통과한 전략 목록이 담긴다")
        void passedStrategies에_통과전략_포함() {
            AnalysisResponse a = makeAnalysis("005930", "삼성전자", Map.of("CANSLIM", 80, "MINERVINI", 75, "PIOTROSKI", 50));

            ScreeningRequest request = new ScreeningRequest(70, null, null, 70, null, null, null, null);
            List<ScreeningResult> results = screeningService.filter(List.of(a), request);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).passedStrategies()).containsExactlyInAnyOrder("CANSLIM", "MINERVINI");
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  경계 케이스
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("경계 케이스")
    class 경계케이스 {

        @Test
        @DisplayName("조건을 만족하는 종목이 없으면 빈 리스트를 반환한다")
        void 조건만족종목없음_빈리스트() {
            AnalysisResponse a = makeAnalysis("005930", "삼성전자", Map.of("CANSLIM", 40));

            ScreeningRequest request = new ScreeningRequest(70, null, null, null, null, null, null, null);
            List<ScreeningResult> results = screeningService.filter(List.of(a), request);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("분석 결과 목록이 비어 있으면 빈 리스트를 반환한다")
        void 분석결과없음_빈리스트() {
            ScreeningRequest request = new ScreeningRequest(70, null, null, null, null, null, null, null);
            List<ScreeningResult> results = screeningService.filter(List.of(), request);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("전략 데이터가 없는 종목은 해당 전략 점수를 0으로 처리한다")
        void 전략데이터없음_0점처리() {
            AnalysisResponse a = makeAnalysis("005930", "삼성전자", Map.of());  // 전략 점수 없음

            ScreeningRequest request = new ScreeningRequest(70, null, null, null, null, null, null, null);
            List<ScreeningResult> results = screeningService.filter(List.of(a), request);

            assertThat(results).isEmpty();   // 0 < 70 → 통과 실패
        }

        @Test
        @DisplayName("경계값 점수(minScore와 동일)는 조건을 통과한다")
        void 경계값점수_통과() {
            AnalysisResponse a = makeAnalysis("005930", "삼성전자", Map.of("CANSLIM", 70));

            ScreeningRequest request = new ScreeningRequest(70, null, null, null, null, null, null, null);
            List<ScreeningResult> results = screeningService.filter(List.of(a), request);

            assertThat(results).hasSize(1);  // 70 >= 70 → 통과
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  헬퍼
    // ──────────────────────────────────────────────────────────────────────

    private AnalysisResponse makeAnalysis(String ticker, String name, Map<String, Integer> scoreMap) {
        List<StrategyScoreResponse> strategies = scoreMap.entrySet().stream()
                .map(e -> new StrategyScoreResponse(e.getKey(), e.getKey(), e.getValue(), "B",
                        Map.of(), List.of(), List.of()))
                .toList();

        return new AnalysisResponse(ticker, name, "KOR", "IT", LocalDateTime.now(),
                strategies, null, null, null, "BULL");
    }
}
