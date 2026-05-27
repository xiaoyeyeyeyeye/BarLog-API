package com.alcohol.places;

import com.alcohol.config.GooglePlacesProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class GooglePlacesConfig {

    @Bean
    RestTemplate googlePlacesRestTemplate(RestTemplateBuilder builder, GooglePlacesProperties properties) {
        return builder
                .rootUri(properties.getBaseUrl())
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
