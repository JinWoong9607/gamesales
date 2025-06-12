package com.example.gamesales.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "igdb.client")
@Validated
@Getter
@Setter
public class IgdbProperties {
    @NotBlank
    private String id;
    @NotBlank
    private String secret;
    @NotBlank
    private String baseUrl;
}