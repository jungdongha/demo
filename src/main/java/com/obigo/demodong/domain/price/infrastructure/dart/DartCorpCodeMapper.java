package com.obigo.demodong.domain.price.infrastructure.dart;

import com.obigo.demodong.domain.stock.application.exception.StockErrorCode;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import com.obigo.demodong.domain.stock.domain.service.StockWriter;
import com.obigo.demodong.global.common.exception.ApplicationException;
import com.obigo.demodong.global.common.infrastructure.dart.DartProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DART corpCode.xml 파싱 → Stock 테이블의 dartCorpCode 자동 매핑.
 * 앱 시작 시 1회 실행, 이후 매주 월요일 08:30 갱신.
 * corp_name → stock_code 맵도 유지하여 한글 회사명으로 티커 검색 지원.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DartCorpCodeMapper {

    private final WebClient dartWebClient;
    private final DartProperties dartProperties;
    private final StockReader stockReader;
    private final StockWriter stockWriter;

    /** 회사명 → 티커 코드 (한글 이름 검색용) */
    private final Map<String, String> nameToTickerMap = new ConcurrentHashMap<>();

    /** 티커 → DART 법인코드 (신규 종목 생성 시 즉시 매핑용) */
    private final Map<String, String> tickerToCorpCodeMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void syncOnStartup() {
        log.info("[DART] corp_code 자동 매핑 시작");
        try {
            sync();
        } catch (Throwable t) {
            // Error(reactor BlockingOperationError 등) 포함 전체 방어 — 기동 차단 방지
            log.warn("[DART] corp_code 매핑 실패 (서비스 기동에는 영향 없음): {}", t.getMessage());
        }
    }

    public void sync() {
        Map<String, String> stockCodeToCorpCode = loadCorpCodeMap();
        if (stockCodeToCorpCode.isEmpty()) return;

        List<Stock> korStocks = stockReader.findAll().stream()
                .filter(s -> s.getMarketType() == MarketType.KOR)
                .filter(s -> s.getDartCorpCode() == null)
                .toList();

        int updated = 0;
        for (Stock stock : korStocks) {
            String corpCode = stockCodeToCorpCode.get(stock.getTicker());
            if (corpCode != null) {
                stock.updateDartCorpCode(corpCode);
                stockWriter.save(stock);
                updated++;
            }
        }
        log.info("[DART] corp_code 매핑 완료 - {}건 업데이트", updated);
    }

    /**
     * 티커로 DART 법인코드 즉시 조회 (신규 종목 생성 시 사용).
     * corpCode.xml이 로드된 경우에만 유효. 없으면 Optional.empty().
     */
    public Optional<String> resolveCorpCodeByTicker(String ticker) {
        return Optional.ofNullable(tickerToCorpCodeMap.get(ticker));
    }
    /**
     * 한글 회사명으로 티커 검색.
     * <ul>
     *   <li>완전 일치: 즉시 반환</li>
     *   <li>단일 부분 일치: 반환</li>
     *   <li>복수 부분 일치: {@link StockErrorCode#STOCK_NAME_AMBIGUOUS} 예외 (후보 목록 로그 출력)</li>
     *   <li>미일치: {@link StockErrorCode#STOCK_NOT_FOUND} 예외</li>
     * </ul>
     */
    public Optional<String> resolveTickerByName(String name) {
        String exact = nameToTickerMap.get(name);
        if (exact != null) return Optional.of(exact);

        List<Map.Entry<String, String>> matchedEntries = nameToTickerMap.entrySet().stream()
                .filter(e -> e.getKey().contains(name))
                .distinct()
                .toList();

        if (matchedEntries.size() == 1) {
            return Optional.of(matchedEntries.get(0).getValue());
        }

        if (matchedEntries.size() > 1) {
            String candidates = matchedEntries.stream()
                    .map(e -> "'" + e.getKey() + "'(" + e.getValue() + ")")
                    .collect(java.util.stream.Collectors.joining(", "));
            log.warn("[DART] 종목명 복수 후보 발견 - query: '{}', 후보: {}", name, candidates);
            throw new ApplicationException(StockErrorCode.STOCK_NAME_AMBIGUOUS);
        }

        return Optional.empty();
    }

    private Map<String, String> loadCorpCodeMap() {
        try {
            byte[] zipBytes = dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/corpCode.xml")
                            .queryParam("crtfc_key", dartProperties.apiKey())
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (zipBytes == null) {
                log.warn("[DART] corpCode.xml 다운로드 실패 — 응답 없음");
                return Map.of();
            }

            return parseCorpCodeZip(zipBytes);
        } catch (Exception e) {
            log.warn("[DART] corpCode.xml 다운로드 예외: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, String> parseCorpCodeZip(byte[] zipBytes) throws Exception {
        Map<String, String> result = new HashMap<>();
        Map<String, String> newNameMap = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().toUpperCase().contains("CORPCODE")) continue;

                // SAX 파서: DOM과 달리 XML 전체를 메모리에 올리지 않고 스트리밍 처리 → OOM 방지
                SAXParserFactory.newInstance().newSAXParser().parse(zis, new DefaultHandler() {
                    private final StringBuilder buf = new StringBuilder();
                    private String corpCode, stockCode, corpName;

                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attributes) {
                        if ("list".equals(qName)) { corpCode = null; stockCode = null; corpName = null; }
                        buf.setLength(0);
                    }

                    @Override
                    public void characters(char[] ch, int start, int length) {
                        buf.append(ch, start, length);
                    }

                    @Override
                    public void endElement(String uri, String localName, String qName) {
                        String val = buf.toString().trim();
                        switch (qName) {
                            case "corp_code"  -> corpCode  = val;
                            case "stock_code" -> stockCode = val;
                            case "corp_name"  -> corpName  = val;
                            case "list" -> {
                                if (corpCode != null && stockCode != null && !stockCode.isBlank()) {
                                    result.put(stockCode, corpCode);
                                    if (corpName != null && !corpName.isBlank()) {
                                        newNameMap.put(corpName, stockCode);
                                    }
                                }
                            }
                        }
                        buf.setLength(0);
                    }
                });
                break;
            }
        }
        nameToTickerMap.putAll(newNameMap);
        tickerToCorpCodeMap.putAll(result);  // ticker → corpCode 캐시 갱신
        log.info("[DART] corpCode.xml 파싱 완료 - {}개 법인", result.size());
        return result;
    }
}
