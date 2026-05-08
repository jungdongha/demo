package com.obigo.demodong.domain.ai.domain.service;

import com.obigo.demodong.domain.ai.application.exception.AiErrorCode;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;

    public String getChatResponse(String question) {
        if (!StringUtils.hasText(question)) {
            throw new ApplicationException(AiErrorCode.AI_INVALID_REQUEST);
        }

        try {
            String content = this.chatClient.prompt()
                    .user(question)
                    .call()
                    .content();

            if (!StringUtils.hasText(content)) {
                throw new ApplicationException(AiErrorCode.AI_RESPONSE_EMPTY);
            }

            return content;
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI LLM 호출 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw new ApplicationException(AiErrorCode.AI_SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    // ★ system + user 분리 메서드
    //   system: 역할 정의 + 출력 규칙 (정적, LLM이 우선 처리)
    //   userMessage: 실제 데이터 (뉴스, 종목명 등 동적 내용)
    //   LLM은 system 지시를 따르면서 user 데이터를 분석함
    public String getChatResponse(String systemPrompt, String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            throw new ApplicationException(AiErrorCode.AI_INVALID_REQUEST);
        }

        try {
            String content = this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            if (!StringUtils.hasText(content)) {
                throw new ApplicationException(AiErrorCode.AI_RESPONSE_EMPTY);
            }

            return content;
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI LLM 호출 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw new ApplicationException(AiErrorCode.AI_SERVICE_UNAVAILABLE, e.getMessage());
        }
    }
}
