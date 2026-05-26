package com.obigo.demodong.domain.analysis.service;

import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;
import com.obigo.demodong.domain.analysis.domain.service.MarketRegimeService;
import com.obigo.demodong.domain.technical.domain.calculator.ReturnCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@DisplayName("MarketRegimeService 단위 테스트")
class MarketRegimeServiceTest {

    private ReturnCalculator returnCalculator;
    private MarketRegimeService marketRegimeService;
    private final List<BigDecimal> mockPrices = List.of(BigDecimal.valueOf(100), BigDecimal.valueOf(102));

    @BeforeEach
    void setUp() {
        returnCalculator = Mockito.mock(ReturnCalculator.class);
        marketRegimeService = new MarketRegimeService(returnCalculator);
    }

    @Test
    @DisplayName("CRISIS 시나리오: 5일 수익률이 -5% 미만인 경우 CRISIS를 반환한다")
    void CRISIS_판단() {
        when(returnCalculator.calculate(anyList(), Mockito.eq(5))).thenReturn(BigDecimal.valueOf(-6.0));
        when(returnCalculator.calculate(anyList(), Mockito.eq(20))).thenReturn(BigDecimal.valueOf(-2.0));

        MarketRegime regime = marketRegimeService.determine(mockPrices);
        assertThat(regime).isEqualTo(MarketRegime.CRISIS);
    }

    @Test
    @DisplayName("STRONG_BULL 시나리오: 5일 수익률이 +3% 초과인 경우 STRONG_BULL을 반환한다")
    void STRONG_BULL_판단() {
        when(returnCalculator.calculate(anyList(), Mockito.eq(5))).thenReturn(BigDecimal.valueOf(4.0));
        when(returnCalculator.calculate(anyList(), Mockito.eq(20))).thenReturn(BigDecimal.valueOf(8.0));

        MarketRegime regime = marketRegimeService.determine(mockPrices);
        assertThat(regime).isEqualTo(MarketRegime.STRONG_BULL);
    }

    @Test
    @DisplayName("BEAR 시나리오: 5일 수익률이 -1% 미만이거나 20일 수익률이 -2% 미만인 경우 BEAR를 반환한다")
    void BEAR_판단() {
        // 5일 수익률이 -1.5% (< -1%) 인 경우
        when(returnCalculator.calculate(anyList(), Mockito.eq(5))).thenReturn(BigDecimal.valueOf(-1.5));
        when(returnCalculator.calculate(anyList(), Mockito.eq(20))).thenReturn(BigDecimal.valueOf(1.0));

        MarketRegime regime1 = marketRegimeService.determine(mockPrices);
        assertThat(regime1).isEqualTo(MarketRegime.BEAR);

        // 20일 수익률이 -2.5% (< -2%) 인 경우
        when(returnCalculator.calculate(anyList(), Mockito.eq(5))).thenReturn(BigDecimal.valueOf(-0.5));
        when(returnCalculator.calculate(anyList(), Mockito.eq(20))).thenReturn(BigDecimal.valueOf(-2.5));

        MarketRegime regime2 = marketRegimeService.determine(mockPrices);
        assertThat(regime2).isEqualTo(MarketRegime.BEAR);
    }

    @Test
    @DisplayName("BULL 시나리오: 5일과 20일 수익률 모두 0% 초과인 경우 BULL을 반환한다")
    void BULL_판단() {
        when(returnCalculator.calculate(anyList(), Mockito.eq(5))).thenReturn(BigDecimal.valueOf(1.0));
        when(returnCalculator.calculate(anyList(), Mockito.eq(20))).thenReturn(BigDecimal.valueOf(2.0));

        MarketRegime regime = marketRegimeService.determine(mockPrices);
        assertThat(regime).isEqualTo(MarketRegime.BULL);
    }

    @Test
    @DisplayName("SIDEWAYS 시나리오: 그 외 보합 범위인 경우 SIDEWAYS를 반환한다")
    void SIDEWAYS_판단() {
        when(returnCalculator.calculate(anyList(), Mockito.eq(5))).thenReturn(BigDecimal.valueOf(0.0));
        when(returnCalculator.calculate(anyList(), Mockito.eq(20))).thenReturn(BigDecimal.valueOf(-0.5));

        MarketRegime regime = marketRegimeService.determine(mockPrices);
        assertThat(regime).isEqualTo(MarketRegime.SIDEWAYS);
    }

    @Test
    @DisplayName("null 입력 및 데이터 부족 시나리오: 수익률이 null인 경우 SIDEWAYS를 반환한다")
    void 데이터부족_SIDEWAYS() {
        when(returnCalculator.calculate(anyList(), anyInt())).thenReturn(null);

        MarketRegime regime = marketRegimeService.determine(mockPrices);
        assertThat(regime).isEqualTo(MarketRegime.SIDEWAYS);
    }
}
