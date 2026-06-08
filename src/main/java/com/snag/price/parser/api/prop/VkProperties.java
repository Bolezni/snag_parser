package com.snag.price.parser.api.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vk")
public class VkProperties {
    private String token;
    private String domain = "probnick_ru";
    private int postsPerRequest = 100;
    private int maxRequests = 10;
}
