package com.obigo.demodong.domain.signal.domain.service;

import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.repository.SignalReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignalReportWriter {

    private final SignalReportRepository signalReportRepository;

    public SignalReport save(SignalReport report) {
        return signalReportRepository.save(report);
    }
}
