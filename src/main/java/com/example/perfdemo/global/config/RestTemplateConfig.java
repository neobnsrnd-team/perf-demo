package com.example.perfdemo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Scenario 9: 타행 이체 확인용 RestTemplate 설정
 * - restTemplateNoTimeout: 타임아웃 없음 (병목 버전 — 타행 무응답 시 무한 대기)
 * - restTemplateWithTimeout: connect 3초 / read 3초 (최적화 버전)
 */
@Configuration
public class RestTemplateConfig {

    @Bean("restTemplateNoTimeout")
    public RestTemplate restTemplateNoTimeout() {
        return new RestTemplate();
    }

    @Bean("restTemplateWithTimeout")
    public RestTemplate restTemplateWithTimeout() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }
}
