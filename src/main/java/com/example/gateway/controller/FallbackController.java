package com.example.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 熔断降级控制器：处理Meilisearch服务不可用时的降级请求
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Meilisearch熔断降级处理
     */
    @GetMapping("/meilisearch")
    public ResponseEntity<Map<String, Object>> meilisearchFallback() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.SERVICE_UNAVAILABLE.value());
        result.put("msg", "Meilisearch服务暂不可用，已触发熔断保护，请稍后重试");
        result.put("data", null);
        return new ResponseEntity<>(result, HttpStatus.SERVICE_UNAVAILABLE);
    }
}