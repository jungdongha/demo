package com.obigo.demodong.domain.ai.presentation;


import com.obigo.demodong.domain.ai.application.AiUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<String>> chat(@RequestParam String question) {
        String response = aiUseCase.execute(question);
        return ResponseEntity.ok(ApiResponse.ok(AiResponseCode.AI_CHAT_SUCCESS, response));
    }
}
