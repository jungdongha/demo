package com.obigo.demodong.global.common.infrastructure.dart;

import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Configuration
@EnableConfigurationProperties(DartProperties.class)
public class DartConfig {

    @Bean
    public WebClient dartWebClient(DartProperties dartProperties) throws SSLException {
        // opendart.fss.or.kr 는 TLS_RSA_WITH_AES_128_GCM_SHA256 (레거시 RSA 키 교환) 만 지원한다.
        // Java 21 + -Djdk.tls.client.protocols=TLSv1.2 조합에서는 해당 cipher 가 기본 ClientHello 에서 제외되어
        // SSLHandshakeException: handshake_failure 가 발생한다.
        // INSTANCE_DEFAULTING_TO_SUPPORTED_CIPHERS 는 getSupportedCipherSuites() (레거시 포함 전체) 를
        // setEnabledCipherSuites() 로 활성화하여 해결한다.
        var sslContext = SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .ciphers(null, IdentityCipherSuiteFilter.INSTANCE_DEFAULTING_TO_SUPPORTED_CIPHERS)
                .build();

        var httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));

        return WebClient.builder()
                .baseUrl(dartProperties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // corpCode.xml ZIP 대응
                .build();
    }
}
