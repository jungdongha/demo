package com.obigo.demodong.domain.analysis.presentation;

import com.obigo.demodong.domain.analysis.domain.entity.MagicFormulaRank;
import com.obigo.demodong.domain.analysis.domain.entity.MagicFormulaUniverse;
import com.obigo.demodong.domain.analysis.domain.repository.MagicFormulaRankRepository;
import com.obigo.demodong.domain.analysis.domain.repository.MagicFormulaUniverseRepository;
import com.obigo.demodong.domain.analysis.domain.service.MagicFormulaBatchService;
import com.obigo.demodong.global.common.response.ApiResponse;
import com.obigo.demodong.global.common.response.GlobalResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/magic-formula")
public class MagicFormulaUniverseController {

    private final MagicFormulaUniverseRepository universeRepository;
    private final MagicFormulaRankRepository rankRepository;
    private final MagicFormulaBatchService batchService;

    @GetMapping("/universe")
    public ApiResponse<List<MagicFormulaUniverse>> getUniverse() {
        return ApiResponse.ok(GlobalResponseCode.SUCCESS, universeRepository.findAllByActiveTrue());
    }

    @PostMapping("/universe")
    public ApiResponse<MagicFormulaUniverse> addUniverse(@RequestBody UniverseRequest request) {
        MagicFormulaUniverse item = universeRepository.findByTicker(request.ticker())
                .map(existing -> {
                    existing.updateActive(true);
                    return universeRepository.save(existing);
                })
                .orElseGet(() -> {
                    MagicFormulaUniverse newU = MagicFormulaUniverse.builder()
                            .ticker(request.ticker())
                            .companyName(request.companyName())
                            .active(true)
                            .build();
                    return universeRepository.save(newU);
                });

        return ApiResponse.ok(GlobalResponseCode.CREATED, item);
    }

    @DeleteMapping("/universe/{ticker}")
    public ApiResponse<Void> removeUniverse(@PathVariable String ticker) {
        universeRepository.findByTicker(ticker).ifPresent(item -> {
            item.updateActive(false);
            universeRepository.save(item);
        });
        return ApiResponse.ok(GlobalResponseCode.SUCCESS);
    }

    @PostMapping("/run")
    public ApiResponse<Void> runManualBatch() {
        batchService.calculateAndSaveRanks(LocalDate.now());
        return ApiResponse.ok(GlobalResponseCode.SUCCESS);
    }

    @GetMapping("/ranks")
    public ApiResponse<List<MagicFormulaRank>> getRanks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<MagicFormulaRank> ranks = rankRepository.findAllByRankDateOrderByCombinedRankAsc(targetDate);
        return ApiResponse.ok(GlobalResponseCode.SUCCESS, ranks);
    }

    public record UniverseRequest(String ticker, String companyName) {}
}
