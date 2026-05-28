package com.obigo.demodong.domain.analysis.application.usecase;

import com.obigo.demodong.domain.analysis.application.dto.response.StockSearchResponse;
import com.obigo.demodong.domain.price.infrastructure.dart.DartCorpCodeMapper;
import com.obigo.demodong.domain.stock.application.exception.StockErrorCode;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 종목 검색 유스케이스.
 *
 * <p>검색 우선순위:</p>
 * <ol>
 *   <li>6자리 숫자 → KOR 티커로 직접 조회</li>
 *   <li>영문 → USA 티커로 직접 조회 (대소문자 무관)</li>
 *   <li>한글 → DART 회사명 검색 → 티커 변환 후 조회</li>
 *   <li>키워드 → DB ticker/name LIKE 검색</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockSearchUseCase {

    private final StockReader stockReader;
    private final DartCorpCodeMapper dartCorpCodeMapper;

    /**
     * 키워드로 종목을 검색한다.
     *
     * @param query 종목코드 / 영문 티커 / 한글 회사명
     * @return 검색된 종목 목록 (최대 20개)
     */
    public List<StockSearchResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String trimmed = query.trim();

        // 1. 6자리 숫자 → KOR 정확 조회
        if (trimmed.matches("\\d{6}")) {
            return stockReader.findByTicker(trimmed)
                    .map(s -> List.of(StockSearchResponse.from(s)))
                    .orElse(List.of());
        }

        // 2. 영문 → USA 정확 조회 (대문자 변환)
        if (trimmed.matches("[A-Za-z.\\-]{1,10}")) {
            String upper = trimmed.toUpperCase();
            return stockReader.findByTicker(upper)
                    .map(s -> List.of(StockSearchResponse.from(s)))
                    .orElse(List.of());
        }

        // 3. 한글 → DART 회사명 검색 → 티커로 DB 조회
        if (trimmed.matches(".*[가-힣].*")) {
            try {
                Optional<String> ticker = dartCorpCodeMapper.resolveTickerByName(trimmed);
                if (ticker.isPresent()) {
                    return stockReader.findByTicker(ticker.get())
                            .map(s -> List.of(StockSearchResponse.from(s)))
                            .orElse(List.of());
                }
            } catch (ApplicationException e) {
                if (e.getErrorCode() == StockErrorCode.STOCK_NAME_AMBIGUOUS) {
                    log.debug("[StockSearch] 복수 후보 — query: {}", trimmed);
                }
                return List.of();
            }
        }

        // 4. DB에서 ticker/name 부분 일치 검색
        return stockReader.findAll().stream()
                .filter(s -> s.getTicker().contains(trimmed.toUpperCase())
                          || (s.getName() != null && s.getName().contains(trimmed)))
                .limit(20)
                .map(StockSearchResponse::from)
                .toList();
    }

    /**
     * 전체 종목 목록 조회 (관리자용).
     */
    public List<StockSearchResponse> findAll() {
        return stockReader.findAll().stream()
                .map(StockSearchResponse::from)
                .toList();
    }
}
