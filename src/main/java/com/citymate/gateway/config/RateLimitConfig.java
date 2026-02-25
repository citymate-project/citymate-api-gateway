package com.citymate.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Configuration du Rate Limiting
 * Définit comment identifier les clients (par IP)
 */
@Configuration
public class RateLimitConfig {

    /**
     *
     * KeyResolver basé sur l'IP du client
     * Chaque IP a sa propre limite de requêtes
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();

            System.out.println("Rate Limit check for IP: " + ip);
            return Mono.just(ip);
        };
    }
}