package com.obigo.demodong.domain.quant.application.dto.response;

import com.obigo.demodong.domain.quant.domain.entity.QuantFeatureSnapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuantFeatureSnapshotResponse(
        BigDecimal volumeRatio5d,
        BigDecimal priceMomentum5d,
        BigDecimal newsFreshnessScore,
        BigDecimal marketRegimeBonus,
        LocalDateTime snapshotAt
) {
    public static QuantFeatureSnapshotResponse from(QuantFeatureSnapshot snapshot) {
        return new QuantFeatureSnapshotResponse(
                snapshot.getVolumeRatio5d(),
                snapshot.getPriceMomentum5d(),
                snapshot.getNewsFreshnessScore(),
                snapshot.getMarketRegimeBonus(),
                snapshot.getSnapshotAt()
        );
    }
}
