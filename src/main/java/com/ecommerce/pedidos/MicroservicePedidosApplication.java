package com.ecommerce.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal do Microserviço de Pedidos
 * 
 * @EnableCaching - Habilita suporte a cache (para cliente REST de produtos)
 * @EnableScheduling - Habilita jobs agendados (para OutboxProcessor)
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableFeignClients
public class MicroservicePedidosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicePedidosApplication.class, args);
    }
}




