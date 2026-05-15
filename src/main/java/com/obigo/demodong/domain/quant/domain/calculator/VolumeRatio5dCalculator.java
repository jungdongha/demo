package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Feature: volume_ratio_5d
 * 현재 거래량 / 5일 평균 거래량 (배수).
 * 1.0 = 평균, 2.0 = 2배, 3.0 이상 = 급등 신호.
 *
 * 정규화: min(volumeRatio / 3.0, 1.0) * 100
 * 가중치: 35%
 */
@Slf4j
@Component
public class VolumeRatio5dCalculator implements QuantFeatureCalculator {

    @Override
    public QuantFeatureResult calculate(QuantFeatureInput input) {
        List<DailyQuote> prices = input.priceHistory();

        if (prices == null || prices.size() < 2) {
            log.warn("[VolumeRatio5d] 가격 데이터 부족 - ticker: {}", input.ticker());
            return new QuantFeatureResult(getFeatureType(), 1.0, 33.3);  // 중간값 반환
        }

        // priceHistory는 최신순 정렬 가정. 최신 거래량
        long currentVolume = prices.get(0).volume() != null ? prices.get(0).volume() : 0L;

        // 최근 5일 평균 거래량 (현재일 제외 4일 + 현재일)
        int days = Math.min(prices.size(), 5);
        double avgVolume = prices.subList(0, days).stream()
                .filter(q -> q.volume() != null)
                .mapToLong(DailyQuote::volume)
                .average()
                .orElse(1.0);

        if (avgVolume <= 0) {
            return new QuantFeatureResult(getFeatureType(), 1.0, 33.3);
        }

        double ratio = currentVolume / avgVolume;
        double normalized = Math.min(ratio / 3.0, 1.0) * 100.0;

        log.debug("[VolumeRatio5d] ticker={}, ratio={:.2f}, score={:.1f}", input.ticker(), ratio, normalized);
        return new QuantFeatureResult(getFeatureType(), ratio, normalized);
    }

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.VOLUME_RATIO_5D;
    }
}
