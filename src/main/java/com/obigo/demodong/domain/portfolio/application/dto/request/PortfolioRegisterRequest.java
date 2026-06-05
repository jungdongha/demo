package com.obigo.demodong.domain.portfolio.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PortfolioRegisterRequest(
        @NotBlank(message = "종목코드를 입력해주세요.")
        String ticker,
        @Positive(message = "수량은 1 이상이어야 합니다.")
        int quantity,
        @Positive(message = "평균단가는 0보다 커야 합니다.")
        BigDecimal avgPrice
) {
}
