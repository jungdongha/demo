package com.obigo.demodong.domain.price.infrastructure.dart;

import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import com.obigo.demodong.domain.stock.domain.service.StockWriter;
import com.obigo.demodong.global.common.infrastructure.dart.DartProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DART corpCode.xml 파싱 → Stock 테이블의 dartCorpCode 자동 매핑.
 * 앱 시작 시 1회 실행, 이후 매주 월요일 08:30 갱신.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DartCorpCodeMapper {

    private final WebClient dartWebClient;
    private final DartProperties dartProperties;
    private final StockReader stockReader;
    private final StockWriter stockWriter;

    @PostConstruct
    public void syncOnStartup() {
        log.info("[DART] corp_code 자동 매핑 시작");
        try {
            sync();
        } catch (Exception e) {
            log.warn("[DART] corp_code 매핑 실패 (서비스 기동에는 영향 없음): {}", e.getMessage());
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

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().toUpperCase().contains("CORPCODE")) continue;

                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(zis);

                NodeList list = doc.getElementsByTagName("list");
                for (int i = 0; i < list.getLength(); i++) {
                    NodeList children = list.item(i).getChildNodes();
                    String corpCode = null, stockCode = null;
                    for (int j = 0; j < children.getLength(); j++) {
                        String tag = children.item(j).getNodeName();
                        String val = children.item(j).getTextContent().trim();
                        if ("corp_code".equals(tag))  corpCode  = val;
                        if ("stock_code".equals(tag)) stockCode = val;
                    }
                    if (corpCode != null && stockCode != null && !stockCode.isBlank()) {
                        result.put(stockCode, corpCode);
                    }
                }
                break;
            }
        }
        log.info("[DART] corpCode.xml 파싱 완료 - {}개 법인", result.size());
        return result;
    }
}
