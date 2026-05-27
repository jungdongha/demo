package com.obigo.demodong.domain.analysis.domain.entity;

import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "magic_formula_rank",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ticker", "rankDate"})
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MagicFormulaRank extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false)
    private LocalDate rankDate;

    @Column(precision = 18, scale = 4)
    private BigDecimal roic;

    @Column(precision = 18, scale = 4)
    private BigDecimal earningsYield;

    @Column(nullable = false)
    private int roicRank;

    @Column(nullable = false)
    private int earningsYieldRank;

    @Column(nullable = false)
    private int combinedRank;

    @Column(nullable = false)
    private int universeSize;
}
