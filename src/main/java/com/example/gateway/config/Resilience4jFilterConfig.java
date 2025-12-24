package com.example.gateway.config;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Resilience4j过滤器配置：通过全局过滤器实现限流和熔断，修正GatewayFilterFactory找不到的问题
 */
@Configuration
public class Resilience4jFilterConfig {

    /**
     * 限流过滤器：优先级高于熔断（Order值越小，执行越早）
     * 注：Order设置为-100，确保在路由转发前执行限流
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100) // 调整执行顺序，避免与内置过滤器冲突
    public GlobalFilter rateLimiterFilter(RateLimiterRegistry rateLimiterRegistry) {
        // 从配置文件加载限流实例
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("meilisearchRateLimiter");

        return (exchange, chain) -> {
            // 对网关的请求转发流程应用限流规则
            return chain.filter(exchange)
                    .transformDeferred(RateLimiterOperator.of(rateLimiter))
                    // 限流触发时的异常处理
                    .onErrorResume(throwable -> {
                        ServerWebExchange responseExchange = handleRateLimitError(exchange);
                        return responseExchange.getResponse().setComplete();
                    });
        };
    }

    /**
     * 熔断过滤器：优先级低于限流
     * 注：Order设置为-200，在限流后执行熔断
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 200)
    public GlobalFilter circuitBreakerFilter(CircuitBreakerRegistry circuitBreakerRegistry) {
        // 从配置文件加载熔断实例
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("meilisearchCircuitBreaker");

        return (exchange, chain) -> {
            // 对网关的请求转发流程应用熔断规则
            return chain.filter(exchange)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    // 熔断触发时的异常处理
                    .onErrorResume(throwable -> {
                        ServerWebExchange responseExchange = handleCircuitBreakError(exchange);
                        return responseExchange.getResponse().setComplete();
                    });
        };
    }

    /**
     * 处理限流异常：返回429 JSON响应
     */
    private ServerWebExchange handleRateLimitError(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构造JSON错误信息
        String errorMsg = "{\"code\":429,\"msg\":\"请求过于频繁，已触发限流保护\",\"data\":null}";
        byte[] bytes = errorMsg.getBytes(StandardCharsets.UTF_8);

        // 写入响应体
        response.getHeaders().setContentLength(bytes.length);
        response.writeWith(Mono.just(response.bufferFactory().wrap(bytes))).subscribe();

        return exchange;
    }

    /**
     * 处理熔断异常：返回503 JSON响应
     */
    private ServerWebExchange handleCircuitBreakError(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构造JSON错误信息
        String errorMsg = "{\"code\":503,\"msg\":\"Meilisearch服务异常，已触发熔断保护\",\"data\":null}";
        byte[] bytes = errorMsg.getBytes(StandardCharsets.UTF_8);

        // 写入响应体
        response.getHeaders().setContentLength(bytes.length);
        response.writeWith(Mono.just(response.bufferFactory().wrap(bytes))).subscribe();

        return exchange;
    }
}