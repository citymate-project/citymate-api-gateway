package com.citymate.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Filtre global qui intercepte TOUTES les requêtes
 * Valide le JWT avant de forward vers les APIs backend
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    // Endpoints publics (pas besoin de JWT)
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator",
            "/api/v1/auth/health",
            "/api/v1/events",
            "/api/v1/events/categories",
            "/api/v1/deals/categories",
            "/api/v1/deals"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().toString();

        log.debug("Gateway Filter: {} {}", request.getMethod(), path);

        // Si c'est un endpoint public
        if (isPublicPath(path)) {
            log.debug("Public path, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // Sinon, valider le JWT
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Pas de header Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorizedResponse(response, "Missing or invalid Authorization header", path);
        }

        // Extraire le token
        String token = authHeader.substring(7);

        // Valider le token
        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired JWT token for path: {}", path);
                return unauthorizedResponse(response, "Invalid or expired JWT token", path);
            }
        } catch (Exception e) {
            log.error("JWT validation error for path {}: {}", path, e.getMessage());
            return unauthorizedResponse(response, "JWT validation failed", path);
        }

        // Token valide
        String username = jwtUtil.extractUsername(token);
        log.debug("JWT valid for user: {}", username);

        // Ajouter le username dans un header pour les APIs backend
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Auth-Username", username)
                .build();

        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        // Continuer la chaîne de filtres
        return chain.filter(modifiedExchange);
    }

    /**
     * Retourne une réponse 401 Unauthorized avec un message JSON
     * Format identique à ErrorResponse du user-api (timestamp, status, error, message, path)
     */
    private Mono<Void> unauthorizedResponse(ServerHttpResponse response, String message, String path) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                java.time.LocalDateTime.now(),
                message,
                path
        );

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
        );
    }

    /**
     * Vérifie si le chemin est public
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}