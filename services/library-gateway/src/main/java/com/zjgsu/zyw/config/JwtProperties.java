package com.zjgsu.zyw.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "zyw.jwt")
public class JwtProperties {
    private String secret;
    private Long expireMinutes;
}
