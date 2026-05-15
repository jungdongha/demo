package com.obigo.demodong.domain.signal.domain.service;

import com.obigo.demodong.domain.signal.domain.entity.SignalFeedback;
import com.obigo.demodong.domain.signal.domain.repository.SignalFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignalFeedbackWriter {

    private final SignalFeedbackRepository signalFeedbackRepository;

    public SignalFeedback save(SignalFeedback feedback) {
        return signalFeedbackRepository.save(feedback);
    }
}
