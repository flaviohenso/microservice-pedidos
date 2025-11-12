package com.ecommerce.pedidos.infrastructure.messaging;

import com.ecommerce.pedidos.application.service.EventPublisherPort;
import com.ecommerce.pedidos.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementação do publisher de eventos usando RabbitMQ
 */
@Component
public class RabbitMQPublisher implements EventPublisherPort {
    
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQPublisher.class);
    
    private final RabbitTemplate rabbitTemplate;
    
    public RabbitMQPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    @Override
    public void publicarEvento(String eventType, String routingKey, String payload) {
        try {
            // Cria propriedades da mensagem
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            messageProperties.setHeader("event_type", eventType);
            messageProperties.setHeader("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            messageProperties.setHeader("correlation_id", UUID.randomUUID().toString());
            
            // Cria a mensagem
            Message message = new Message(payload.getBytes(), messageProperties);
            
            // Publica no exchange
            rabbitTemplate.send(RabbitMQConfig.PEDIDOS_EXCHANGE, routingKey, message);
            
            logger.info("Evento publicado com sucesso: tipo={}, routingKey={}", eventType, routingKey);
            
        } catch (Exception e) {
            logger.error("Erro ao publicar evento: tipo={}, routingKey={}, erro={}", 
                    eventType, routingKey, e.getMessage(), e);
            throw new RuntimeException("Erro ao publicar evento no RabbitMQ", e);
        }
    }
}

