package com.snag.price.parser.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ApplicationConfig {

    @Bean
    public RestClient vkRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.vk.com") 
                .build();
    }
}
