package com.obigo.demodong.domain.analysis.application.usecase;

import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisHistoryResponse;
import com.obigo.demodong.domain.analysis.domain.repository.AnalysisHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 분석 히스토리 조회 유스케이스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisHistoryUseCase {

    private final AnalysisHistoryRepository analysisHistoryRepository;

    /**
     * @param ticker 종목코드
     * @param days   조회 기간 (1~90일)
     */
    public List<AnalysisHistoryResponse> getHistory(String ticker, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        return analysisHistoryRepository
                .findByTickerAndAnalyzeDateAfterOrderByAnalyzeDateAsc(ticker, from)
                .stream()
                .map(AnalysisHistoryResponse::from)
                .toList();
    }
}
