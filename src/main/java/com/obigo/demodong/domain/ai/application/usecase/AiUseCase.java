package com.obigo.demodong.domain.ai.application.usecase;

import com.obigo.demodong.domain.ai.application.dto.response.AiResponse;
import com.obigo.demodong.domain.ai.domain.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiUseCase {

    private final AiChatService aiChatService;

    public AiResponse execute(String question) {
        String content = aiChatService.getChatResponse(question);
        return new AiResponse(content);
    }
}


