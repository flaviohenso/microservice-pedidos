package com.ecommerce.pedidos.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ
 * Define exchanges, queues, bindings e Dead Letter Queue
 */
@Configuration
public class RabbitMQConfig {
    
    // Exchange
    public static final String PEDIDOS_EXCHANGE = "pedidos.exchange";
    
    // Queues
    public static final String PEDIDOS_CRIADOS_QUEUE = "pedidos.criados.queue";
    public static final String PEDIDOS_DLQ = "pedidos.dlq";
    
    // Routing Keys
    public static final String PEDIDO_CRIADO_ROUTING_KEY = "pedido.criado";
    public static final String PEDIDO_CANCELADO_ROUTING_KEY = "pedido.cancelado";
    
    /**
     * Exchange principal (Topic)
     */
    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(PEDIDOS_EXCHANGE, true, false);
    }
    
    /**
     * Queue para pedidos criados
     */
    @Bean
    public Queue pedidosCriadosQueue() {
        return QueueBuilder.durable(PEDIDOS_CRIADOS_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PEDIDOS_DLQ)
                .build();
    }
    
    /**
     * Dead Letter Queue (DLQ)
     */
    @Bean
    public Queue pedidosDlq() {
        return new Queue(PEDIDOS_DLQ, true);
    }
    
    /**
     * Binding entre exchange e queue de pedidos criados
     */
    @Bean
    public Binding bindingPedidosCriados(Queue pedidosCriadosQueue, TopicExchange pedidosExchange) {
        return BindingBuilder
                .bind(pedidosCriadosQueue)
                .to(pedidosExchange)
                .with(PEDIDO_CRIADO_ROUTING_KEY);
    }
    
    /**
     * Conversor de mensagens JSON
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitTemplate configurado com conversor JSON
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

