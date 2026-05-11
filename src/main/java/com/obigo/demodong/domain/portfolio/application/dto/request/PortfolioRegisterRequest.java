package com.obigo.demodong.domain.portfolio.application.dto.request;

import java.math.BigDecimal;

public record PortfolioRegisterRequest(
        String ticker,
        int quantity,
        BigDecimal avgPrice
) {
}
