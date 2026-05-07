package com.obigo.demodong.global.common.infrastructure.naver;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="naver.api")
public record NaverProperties
        (
                String clientId,
                String clientSecret,
                String newsUrl

        )
{}
