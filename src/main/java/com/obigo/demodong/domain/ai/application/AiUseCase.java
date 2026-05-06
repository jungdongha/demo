package com.obigo.demodong.domain.ai.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiUseCase {

    private final ChatClient chatClient;

    public AiUseCase(@Qualifier("groqChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String execute(String question) {
        return this.chatClient.prompt()
                .user(question)
                .call()
                .content();
    }


}
