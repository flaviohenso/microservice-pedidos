package com.ecommerce.pedidos.infrastructure.persistence.mapper;

import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.infrastructure.persistence.entity.OutboxJpaEntity;

/**
 * Mapper para conversão entre OutboxEvent (domínio) e OutboxJpaEntity (infraestrutura)
 */
public class OutboxMapper {
    
    /**
     * Converte entidade de domínio para entidade JPA
     */
    public static OutboxJpaEntity toJpaEntity(OutboxEvent event) {
        if (event == null) {
            return null;
        }
        
        OutboxJpaEntity jpaEntity = new OutboxJpaEntity();
        jpaEntity.setId(event.getId());
        jpaEntity.setAggregateType(event.getAggregateType());
        jpaEntity.setAggregateId(event.getAggregateId());
        jpaEntity.setEventType(event.getEventType());
        jpaEntity.setPayload(event.getPayload());
        jpaEntity.setStatus(event.getStatus());
        jpaEntity.setCreatedAt(event.getCreatedAt());
        jpaEntity.setProcessedAt(event.getProcessedAt());
        jpaEntity.setRetryCount(event.getRetryCount());
        jpaEntity.setErrorMessage(event.getErrorMessage());
        
        return jpaEntity;
    }
    
    /**
     * Converte entidade JPA para entidade de domínio
     */
    public static OutboxEvent toDomain(OutboxJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        return new OutboxEvent(
                jpaEntity.getId(),
                jpaEntity.getAggregateType(),
                jpaEntity.getAggregateId(),
                jpaEntity.getEventType(),
                jpaEntity.getPayload(),
                jpaEntity.getStatus(),
                jpaEntity.getCreatedAt(),
                jpaEntity.getProcessedAt(),
                jpaEntity.getRetryCount(),
                jpaEntity.getErrorMessage()
        );
    }
}




