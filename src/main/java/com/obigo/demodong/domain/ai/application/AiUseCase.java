package com.obigo.demodong.domain.ai.application;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiUseCase {

    private final ChatClient chatClient;


}
