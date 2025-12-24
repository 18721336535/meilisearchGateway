package com.example.gateway.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 负载均衡配置：手动注册Meilisearch实例（轮询策略）
 */
@Configuration
public class LoadBalancerConfig {

    @Bean
    @Primary
    public ServiceInstanceListSupplier meilisearchServiceInstanceListSupplier() {
        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "meilisearch-service"; // 与网关路由的lb://服务名一致
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                // 手动创建两个Meilisearch实例
                ServiceInstance instance1 = createServiceInstance("meilisearch-1", "192.168.1.2", 7700);
                ServiceInstance instance2 = createServiceInstance("meilisearch-2", "192.168.1.2", 6600);
                return Flux.just(List.of(instance1, instance2));
            }
        };
    }

    /**
     * 构建ServiceInstance对象
     */
    private ServiceInstance createServiceInstance(String instanceId, String host, int port) {
        return new ServiceInstance() {
            @Override
            public String getInstanceId() {
                return instanceId;
            }

            @Override
            public String getServiceId() {
                return "meilisearch-service";
            }

            @Override
            public String getHost() {
                return host;
            }

            @Override
            public int getPort() {
                return port;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public URI getUri() {
                return URI.create(String.format("http://%s:%d", host, port));
            }

            @Override
            public Map<String, String> getMetadata() {
                return Map.of(); // 空元数据
            }
        };
    }
}