package com.cheng.meetlink.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "meetlink")
public class MeetLinkConfig {

    private String password;
    private int limit;
    private String name;
    private int expires;
    private AiConfig doubao;
    private AiConfig deepSeek;

    @Data
    public static class AiConfig {
        private boolean enabled = true;
        private String apiKey;
        private int countLimit;
        private int lengthLimit;
        private String model;
    }
}
