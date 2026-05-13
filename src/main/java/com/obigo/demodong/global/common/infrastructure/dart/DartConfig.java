package com.obigo.demodong.global.common.infrastructure.dart;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(DartProperties.class)
public class DartConfig {

    @Bean
    public WebClient dartWebClient(DartProperties dartProperties) {
        return WebClient.builder()
                .baseUrl(dartProperties.baseUrl())
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // corpCode.xml ZIP 대응
                .build();
    }
}
