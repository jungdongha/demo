package com.obigo.demodong.domain.quant.domain.service;

import com.obigo.demodong.domain.quant.domain.entity.QuantFeatureSnapshot;
import com.obigo.demodong.domain.quant.domain.entity.QuantSignal;
import com.obigo.demodong.domain.quant.domain.repository.QuantFeatureSnapshotRepository;
import com.obigo.demodong.domain.quant.domain.repository.QuantSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuantSignalWriter {

    private final QuantSignalRepository quantSignalRepository;
    private final QuantFeatureSnapshotRepository quantFeatureSnapshotRepository;

    public QuantSignal save(QuantSignal signal) {
        return quantSignalRepository.save(signal);
    }

    public QuantFeatureSnapshot saveSnapshot(QuantFeatureSnapshot snapshot) {
        return quantFeatureSnapshotRepository.save(snapshot);
    }

    public void deleteAll(List<QuantSignal> signals) {
        quantSignalRepository.deleteAll(signals);
    }
}
