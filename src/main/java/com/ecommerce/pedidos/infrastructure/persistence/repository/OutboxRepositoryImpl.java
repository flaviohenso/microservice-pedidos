package com.ecommerce.pedidos.infrastructure.persistence.repository;

import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.infrastructure.persistence.mapper.OutboxMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementação do repositório de Outbox usando Spring Data JPA
 */
@Component
public class OutboxRepositoryImpl implements OutboxRepositoryPort {
    
    private final OutboxJpaRepository jpaRepository;
    
    public OutboxRepositoryImpl(OutboxJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public OutboxEvent salvar(OutboxEvent event) {
        var jpaEntity = OutboxMapper.toJpaEntity(event);
        var savedEntity = jpaRepository.save(jpaEntity);
        return OutboxMapper.toDomain(savedEntity);
    }
    
    @Override
    public List<OutboxEvent> buscarPorStatus(OutboxStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(OutboxMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OutboxEvent> buscarPendentesPaginado(int limite) {
        var pageable = PageRequest.of(0, limite);
        return jpaRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, pageable).stream()
                .map(OutboxMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public OutboxEvent atualizar(OutboxEvent event) {
        var jpaEntity = OutboxMapper.toJpaEntity(event);
        var updatedEntity = jpaRepository.save(jpaEntity);
        return OutboxMapper.toDomain(updatedEntity);
    }
}

