package com.obigo.demodong.global.common.infrastructure.naver;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({NaverProperties.class})
public class NaverApiConfig {

    @Bean
    public WebClient naverWebClient(NaverProperties naverProperties) {
        return WebClient.builder()
                .baseUrl(naverProperties.newsUrl())
                .defaultHeader("X-Naver-Client-Id", naverProperties.clientId())
                .defaultHeader("X-Naver-Client-Secret", naverProperties.clientSecret())
                .build();
    }
}
