package com.obigo.demodong.domain.price.infrastructure.dart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.obigo.demodong.domain.price.domain.model.DisclosureItem;
import com.obigo.demodong.domain.price.domain.port.CorporateDisclosurePort;
import com.obigo.demodong.global.common.infrastructure.dart.DartProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DartDisclosureProvider implements CorporateDisclosurePort {

    private final WebClient dartWebClient;
    private final DartProperties dartProperties;

    @Override
    public List<DisclosureItem> fetchRecentDisclosures(String dartCorpCode, int limit) {
        if (dartCorpCode == null || dartCorpCode.isBlank()) {
            return Collections.emptyList();
        }
        try {
            DartListResponse response = dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/list.json")
                            .queryParam("crtfc_key", dartProperties.apiKey())
                            .queryParam("corp_code", dartCorpCode)
                            .queryParam("pblntf_ty", "A")  // 수시공시
                            .queryParam("page_no", 1)
                            .queryParam("page_count", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(DartListResponse.class)
                    .block(Duration.ofSeconds(15));

            if (response == null || !"000".equals(response.status()) || response.list() == null) {
                log.warn("[DART] 공시 조회 실패 - corpCode: {}, status: {}",
                        dartCorpCode, response != null ? response.status() : "null");
                return Collections.emptyList();
            }

            return response.list().stream()
                    .map(item -> new DisclosureItem(item.reportNm(), item.rcpDt(), item.pblntfTy()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[DART] 공시 조회 예외 - corpCode: {}, error: {}", dartCorpCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String format(List<DisclosureItem> items) {
        if (items.isEmpty()) return "공시 정보 없음";
        return items.stream()
                .map(item -> String.format("[%s] %s (%s)", item.date(), item.title(), item.type()))
                .collect(Collectors.joining("\n"));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DartListResponse(
            String status,
            String message,
            List<DartListItem> list
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DartListItem(
            @JsonProperty("report_nm") String reportNm,
            @JsonProperty("rcept_dt") String rcpDt,
            @JsonProperty("pblntf_detail_ty") String pblntfTy
    ) {}
}
