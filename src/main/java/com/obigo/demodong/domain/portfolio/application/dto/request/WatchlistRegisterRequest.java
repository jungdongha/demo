package com.obigo.demodong.domain.portfolio.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WatchlistRegisterRequest(
        @NotBlank(message = "종목코드를 입력해주세요.")
        String ticker
) {
}
