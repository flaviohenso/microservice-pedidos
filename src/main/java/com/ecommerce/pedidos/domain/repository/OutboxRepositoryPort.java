package com.ecommerce.pedidos.domain.repository;

import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.OutboxStatus;

import java.util.List;

/**
 * Porta (interface) do repositório de Outbox
 * Define o contrato para persistência de eventos Outbox
 */
public interface OutboxRepositoryPort {
    
    /**
     * Salva um evento Outbox
     */
    OutboxEvent salvar(OutboxEvent event);
    
    /**
     * Busca eventos pendentes (para processamento)
     */
    List<OutboxEvent> buscarPorStatus(OutboxStatus status);
    
    /**
     * Busca eventos pendentes com limite de registros
     */
    List<OutboxEvent> buscarPendentesPaginado(int limite);
    
    /**
     * Atualiza um evento Outbox
     */
    OutboxEvent atualizar(OutboxEvent event);
}

