package com.obigo.demodong.domain.analysis.domain.entity;

import com.obigo.demodong.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "magic_formula_universe")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MagicFormulaUniverse extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false)
    private boolean active;

    public void updateActive(boolean active) {
        this.active = active;
    }
}
