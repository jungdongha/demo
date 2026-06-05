package com.obigo.demodong.global.common.infrastructure.kis;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(KisProperties.class)
public class KisConfig {

    @Bean
    public WebClient kisWebClient(KisProperties kisProperties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)   // TCP 연결 타임아웃 5초
                .responseTimeout(Duration.ofSeconds(10));               // 응답 수신 타임아웃 10초

        return WebClient.builder()
                .baseUrl(kisProperties.baseUrl())
                .defaultHeader("appkey", kisProperties.appKey())
                .defaultHeader("appsecret", kisProperties.appSecret())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
