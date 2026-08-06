package com.example.book.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Registers the WebClient bean for the catalog common-layer.
 * One @Configuration bean per downstream endpoint (BCBSM convention).
 */
@Configuration
public class WebClientConfig {

    @Bean("catalogWebClient")
    public WebClient catalogWebClient(@Value("${catalog.base-url:http://localhost:8081}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
