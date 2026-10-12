package com.ecommerce.pedidos.application.service;

import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.Pedido;

import java.util.function.Function;

/**
 * Porta para persistência transacional atômica de Pedido e Evento Outbox.
 * Isola a transação de banco de dados para evitar prender conexões durante I/O de rede externo.
 */
public interface SalvarPedidoTransacionalPort {

    /**
     * Salva o pedido e o respectivo evento Outbox na mesma transação de banco.
     *
     * @param pedido Pedido a ser persistido
     * @param outboxEventFactory Função que recebe o pedido já com ID persistido e retorna o OutboxEvent
     * @return Pedido salvo
     */
    Pedido salvar(Pedido pedido, Function<Pedido, OutboxEvent> outboxEventFactory);
}
