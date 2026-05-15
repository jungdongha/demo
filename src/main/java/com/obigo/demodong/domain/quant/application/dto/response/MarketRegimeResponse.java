package com.obigo.demodong.domain.quant.application.dto.response;

import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;

import java.time.LocalDateTime;

public record MarketRegimeResponse(
        MarketRegime korRegime,
        double korIndexReturn5d,
        MarketRegime usaRegime,
        LocalDateTime analyzedAt
) {
    public static MarketRegimeResponse of(MarketRegime korRegime, double korReturn,
                                          MarketRegime usaRegime) {
        return new MarketRegimeResponse(korRegime, korReturn, usaRegime, LocalDateTime.now());
    }
}
