package com.obigo.demodong.domain.technical.calculator;

import com.obigo.demodong.domain.technical.domain.calculator.RsRatingCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RsRatingCalculator 단위 테스트")
class RsRatingCalculatorTest {

    private final RsRatingCalculator rsRatingCalculator = new RsRatingCalculator();

    @Test
    @DisplayName("동일 수익률 시나리오: 종목 수익률과 시장 수익률이 같으면 50점을 반환한다")
    void 동일수익률_50점() {
        int result = rsRatingCalculator.calculate(BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0));
        assertThat(result).isEqualTo(50);
    }

    @Test
    @DisplayName("극단적 상회 시나리오: 시장 대비 초과수익률이 +20%p 이상이면 100점을 반환한다")
    void 초과수익률_20p이상_100점() {
        int result1 = rsRatingCalculator.calculate(BigDecimal.valueOf(30.0), BigDecimal.valueOf(10.0));
        int result2 = rsRatingCalculator.calculate(BigDecimal.valueOf(40.0), BigDecimal.valueOf(10.0));
        assertThat(result1).isEqualTo(100);
        assertThat(result2).isEqualTo(100);
    }

    @Test
    @DisplayName("극단적 하회 시나리오: 시장 대비 초과수익률이 -20%p 이하이면 0점을 반환한다")
    void 초과수익률_마이너스20p이하_0점() {
        int result1 = rsRatingCalculator.calculate(BigDecimal.valueOf(-10.0), BigDecimal.valueOf(10.0));
        int result2 = rsRatingCalculator.calculate(BigDecimal.valueOf(-20.0), BigDecimal.valueOf(10.0));
        assertThat(result1).isEqualTo(0);
        assertThat(result2).isEqualTo(0);
    }

    @Test
    @DisplayName("상회 시나리오: 초과수익률이 +5%p 이면 선형 보간된 점수(63점)를 반환한다")
    void 초과수익률_5p_63점() {
        int result = rsRatingCalculator.calculate(BigDecimal.valueOf(15.0), BigDecimal.valueOf(10.0));
        // diff = 5%p. (5 + 20) * 2.5 = 62.5 -> 반올림 63점
        assertThat(result).isEqualTo(63);
    }

    @Test
    @DisplayName("음수 수익률 상회 시나리오: 둘 다 마이너스지만 덜 하락했으면 상응하는 높은 점수를 반환한다")
    void 음수수익률_상대강세_75점() {
        int result = rsRatingCalculator.calculate(BigDecimal.valueOf(-5.0), BigDecimal.valueOf(-15.0));
        // diff = 10%p. (10 + 20) * 2.5 = 75.0 -> 75점
        assertThat(result).isEqualTo(75);
    }

    @Test
    @DisplayName("null 입력 시나리오: 입력 값이 null이면 중립(50점)을 반환한다")
    void null입력_50점() {
        int result1 = rsRatingCalculator.calculate(null, BigDecimal.valueOf(10.0));
        int result2 = rsRatingCalculator.calculate(BigDecimal.valueOf(10.0), null);
        assertThat(result1).isEqualTo(50);
        assertThat(result2).isEqualTo(50);
    }
}
