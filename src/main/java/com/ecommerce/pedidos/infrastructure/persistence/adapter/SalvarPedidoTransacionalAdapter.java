package com.ecommerce.pedidos.infrastructure.persistence.adapter;

import com.ecommerce.pedidos.application.service.SalvarPedidoTransacionalPort;
import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * Adaptador transacional que garante atomicidade da persistência do Pedido e Outbox
 * mantendo a transação do banco restrita apenas a essas operações.
 */
@Component
public class SalvarPedidoTransacionalAdapter implements SalvarPedidoTransacionalPort {

    private final PedidoRepositoryPort pedidoRepository;
    private final OutboxRepositoryPort outboxRepository;

    public SalvarPedidoTransacionalAdapter(
            PedidoRepositoryPort pedidoRepository,
            OutboxRepositoryPort outboxRepository) {
        this.pedidoRepository = pedidoRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional
    public Pedido salvar(Pedido pedido, Function<Pedido, OutboxEvent> outboxEventFactory) {
        Pedido pedidoSalvo = pedidoRepository.salvar(pedido);
        OutboxEvent outboxEvent = outboxEventFactory.apply(pedidoSalvo);
        outboxRepository.salvar(outboxEvent);
        return pedidoSalvo;
    }
}
