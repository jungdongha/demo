package com.obigo.demodong.domain.ai.presentation;


import com.obigo.demodong.domain.ai.application.dto.response.AiResponse;
import com.obigo.demodong.domain.ai.application.usecase.AiUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {
    private final AiUseCase aiUseCase;

    @GetMapping("/chat")
    public ApiResponse<AiResponse> chat(@RequestParam String question) {
        AiResponse response = aiUseCase.execute(question);
        return ApiResponse.ok(AiResponseCode.AI_CHAT_SUCCESS, response);
    }
}
