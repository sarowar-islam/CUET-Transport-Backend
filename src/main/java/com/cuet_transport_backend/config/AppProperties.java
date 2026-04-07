package com.cuet_transport_backend.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors) {

    public record Jwt(String secret, long expirationMs) {
    }

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            if (allowedOrigins == null) {
                allowedOrigins = new ArrayList<>();
            }
        }
    }
}
