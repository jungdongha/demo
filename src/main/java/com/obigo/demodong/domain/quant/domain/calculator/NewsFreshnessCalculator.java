package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Feature: news_freshness_score
 * 최근 뉴스 개수 기반 신선도 점수.
 * 뉴스 1개 = 10점, 10개 이상 = 100점 (cap).
 *
 * 정규화: min(newsCount * 10, 100) → 이미 0~100
 * 가중치: 20%
 */
@Slf4j
@Component
public class NewsFreshnessCalculator implements QuantFeatureCalculator {

    @Override
    public QuantFeatureResult calculate(QuantFeatureInput input) {
        List<String> news = input.recentNews();
        int count = (news == null) ? 0 : news.size();

        double score = Math.min(count * 10.0, 100.0);

        log.debug("[NewsFreshness] ticker={}, newsCount={}, score={}", input.ticker(), count, score);
        return new QuantFeatureResult(getFeatureType(), count, score);
    }

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.NEWS_FRESHNESS;
    }
}
