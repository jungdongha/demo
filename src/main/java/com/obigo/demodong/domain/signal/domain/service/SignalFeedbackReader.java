package com.obigo.demodong.domain.signal.domain.service;

import com.obigo.demodong.domain.signal.domain.entity.SignalFeedback;
import com.obigo.demodong.domain.signal.domain.entity.SignalReport;
import com.obigo.demodong.domain.signal.domain.repository.SignalFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SignalFeedbackReader {

    private final SignalFeedbackRepository signalFeedbackRepository;

    public Optional<SignalFeedback> findByReport(SignalReport report) {
        return signalFeedbackRepository.findByReportAndDeletedFalse(report);
    }

    public boolean existsByReport(SignalReport report) {
        return signalFeedbackRepository.existsByReportAndDeletedFalse(report);
    }

    public List<SignalFeedback> findAllWithReport() {
        return signalFeedbackRepository.findAllWithReport();
    }

    public List<SignalFeedback> findRecentWithReport(int limit) {
        return signalFeedbackRepository.findRecentWithReport(PageRequest.of(0, limit));
    }
}
