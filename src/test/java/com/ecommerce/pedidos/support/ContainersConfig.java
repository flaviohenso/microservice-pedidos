package com.ecommerce.pedidos.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Containers reais (mesmas versões do docker-compose) compartilhados por todos os testes de integração.
 * O @ServiceConnection injeta automaticamente URL/credenciais no Spring.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:15-alpine");
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbit() {
        return new RabbitMQContainer("rabbitmq:3-management-alpine");
    }
}
