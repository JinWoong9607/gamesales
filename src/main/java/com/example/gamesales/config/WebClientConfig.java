package com.example.gamesales.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient steamWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://store.steampowered.com")  // 호스트 지정
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
