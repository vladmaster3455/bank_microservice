package com.example.bank.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    AsyncProxyManager<String> caffeineProxyManager() {
        Caffeine<String, RemoteBucketState> builder = (Caffeine) Caffeine.newBuilder()
                .maximumSize(10_000);
        return new CaffeineProxyManager<>(builder, Duration.ofMinutes(5)).asAsync();
    }

    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                        .map(Object::toString)
                        .defaultIfEmpty("anonymous"));
    }
}
