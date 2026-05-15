package com.obigo.demodong.domain.quant.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyQuote(
        LocalDate date,
        BigDecimal closePrice,
        Long volume
) {}
