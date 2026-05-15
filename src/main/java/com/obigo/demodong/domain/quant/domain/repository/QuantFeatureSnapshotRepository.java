package com.obigo.demodong.domain.quant.domain.repository;

import com.obigo.demodong.domain.quant.domain.entity.QuantFeatureSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuantFeatureSnapshotRepository extends JpaRepository<QuantFeatureSnapshot, Long> {

    Optional<QuantFeatureSnapshot> findByQuantSignalId(Long quantSignalId);
}
