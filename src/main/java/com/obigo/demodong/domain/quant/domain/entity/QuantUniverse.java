package com.obigo.demodong.domain.quant.domain.entity;

import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Quant Signal Engine 분석 대상 유니버스.
 * KOR TOP10 + USA TOP10 = 20개 고정 종목 (분기별 리밸런싱).
 * active=true인 종목만 배치 분석 대상.
 */
@Entity
@Table(name = "quant_universe",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ticker", "market_type"}))
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QuantUniverse extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String stockName;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 10)
    private MarketType marketType;

    @Column(nullable = false)
    private int marketCapRank;  // 1~10

    @Column(length = 50)
    private String sector;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDate addedAt;

    @Column
    private LocalDate removedAt;

    public static QuantUniverse create(String ticker, String stockName, MarketType marketType,
                                       int marketCapRank, String sector) {
        return new QuantUniverse(ticker, stockName, marketType, marketCapRank, sector,
                true, LocalDate.now(), null);
    }

    public void deactivate() {
        this.active = false;
        this.removedAt = LocalDate.now();
    }
}
