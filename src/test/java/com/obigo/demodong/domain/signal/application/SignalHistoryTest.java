package com.obigo.demodong.domain.signal.application;

import com.obigo.demodong.domain.signal.application.dto.response.SignalHistoryResponse;
import com.obigo.demodong.domain.signal.application.usecase.StockAnalysisUseCase;
import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.enums.SignalType;
import com.obigo.demodong.domain.signal.domain.enums.SourceType;
import com.obigo.demodong.domain.signal.domain.service.SignalReportReader;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("시그널 히스토리 조회")
class SignalHistoryTest {

    @Mock
    private SignalReportReader signalReportReader;

    @InjectMocks
    private StockAnalysisUseCase stockAnalysisUseCase;

    @Nested
    @DisplayName("ticker 기반 히스토리 조회 시")
    class GetHistoryByTicker {

        @Test
        @DisplayName("유효한 ticker와 페이지 요청 시 히스토리가 반환된다")
        void 유효한_ticker로_히스토리를_조회한다() {
            // given
            Stock stock = Stock.builder()
                    .ticker("005930").name("삼성전자")
                    .marketType(MarketType.KOR).isWatchlist(true)
                    .build();
            SignalReport report = SignalReport.builder()
                    .stock(stock)
                    .signalType(SignalType.BUY)
                    .reason("- 반도체 업황 회복")
                    .content("전체 분석 내용")
                    .rawNewsText("뉴스")
                    .sourceType(SourceType.SCHEDULED)
                    .build();

            Pageable pageable = PageRequest.of(0, 20);
            given(signalReportReader.findByTicker("005930", pageable))
                    .willReturn(new PageImpl<>(List.of(report), pageable, 1));

            // when
            Page<SignalHistoryResponse> result = stockAnalysisUseCase.getHistoryByTicker("005930", pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).ticker()).isEqualTo("005930");
            assertThat(result.getContent().get(0).stockName()).isEqualTo("삼성전자");
            assertThat(result.getContent().get(0).signalType()).isEqualTo(SignalType.BUY);
        }

        @Test
        @DisplayName("존재하지 않는 ticker 조회 시 빈 페이지가 반환된다")
        void 존재하지_않는_ticker_조회_시_빈_페이지를_반환한다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(signalReportReader.findByTicker("UNKNOWN", pageable))
                    .willReturn(Page.empty(pageable));

            // when
            Page<SignalHistoryResponse> result = stockAnalysisUseCase.getHistoryByTicker("UNKNOWN", pageable);

            // then
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("두 번째 페이지 요청 시 올바른 Pageable이 전달된다")
        void 두번째_페이지_요청_시_페이지네이션이_적용된다() {
            // given
            Pageable pageable = PageRequest.of(1, 10);
            given(signalReportReader.findByTicker("005930", pageable))
                    .willReturn(Page.empty(pageable));

            // when
            Page<SignalHistoryResponse> result = stockAnalysisUseCase.getHistoryByTicker("005930", pageable);

            // then
            assertThat(result.getPageable().getPageNumber()).isEqualTo(1);
            assertThat(result.getPageable().getPageSize()).isEqualTo(10);
        }
    }
}
