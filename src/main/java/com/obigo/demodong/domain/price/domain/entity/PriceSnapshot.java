package com.obigo.demodong.domain.price.domain.entity;

import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "price_snapshot")
@SuperBuilder
@Getter
public class PriceSnapshot extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal closePrice;

    /** 일별 거래량 (주). KIS API 제공 시 저장, 없으면 null. */
    @Column
    private Long volume;

    @Column(nullable = false)
    private LocalDate recordedDate;
}
