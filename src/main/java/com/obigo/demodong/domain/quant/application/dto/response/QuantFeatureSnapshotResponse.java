package com.obigo.demodong.domain.quant.application.dto.response;

import com.obigo.demodong.domain.quant.domain.entity.QuantFeatureSnapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuantFeatureSnapshotResponse(
        // 기존 Feature
        BigDecimal volumeRatio5d,
        BigDecimal priceMomentum5d,
        BigDecimal newsFreshnessScore,
        BigDecimal marketRegimeBonus,
        // 신규: 밸류에이션
        BigDecimal perValue,
        BigDecimal pbrValue,
        BigDecimal valuationScore,
        // 신규: 목표주가
        BigDecimal targetPrice,
        BigDecimal targetPriceUpsidePct,
        BigDecimal targetPriceScore,
        // 신규: 섹터 상대 강도
        BigDecimal sectorRelativeScore,
        LocalDateTime snapshotAt
) {
    public static QuantFeatureSnapshotResponse from(QuantFeatureSnapshot snapshot) {
        return new QuantFeatureSnapshotResponse(
                snapshot.getVolumeRatio5d(),
                snapshot.getPriceMomentum5d(),
                snapshot.getNewsFreshnessScore(),
                snapshot.getMarketRegimeBonus(),
                snapshot.getPerValue(),
                snapshot.getPbrValue(),
                snapshot.getValuationScore(),
                snapshot.getTargetPrice(),
                snapshot.getTargetPriceUpsidePct(),
                snapshot.getTargetPriceScore(),
                snapshot.getSectorRelativeScore(),
                snapshot.getSnapshotAt()
        );
    }
}
