package com.ecommerce.pedidos.application.service;

/**
 * Porta (interface) para publicação de eventos
 * Define o contrato para publicação de eventos no sistema de mensageria
 */
public interface EventPublisherPort {
    
    /**
     * Publica um evento
     * 
     * @param eventType Tipo do evento (ex: "pedido.criado")
     * @param routingKey Routing key para roteamento no exchange
     * @param payload Conteúdo do evento em JSON
     */
    void publicarEvento(String eventType, String routingKey, String payload);
}

