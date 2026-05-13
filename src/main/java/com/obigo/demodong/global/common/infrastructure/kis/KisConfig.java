package com.obigo.demodong.global.common.infrastructure.kis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(KisProperties.class)
public class KisConfig {

    @Bean
    public WebClient kisWebClient(KisProperties kisProperties) {
        return WebClient.builder()
                .baseUrl(kisProperties.baseUrl())
                .defaultHeader("appkey", kisProperties.appKey())
                .defaultHeader("appsecret", kisProperties.appSecret())
                .build();
    }
}
