package com.ecommerce.pedidos.infrastructure.persistence.repository;

import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import com.ecommerce.pedidos.infrastructure.persistence.entity.OutboxJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para Outbox
 */
@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, Long> {
    
    /**
     * Busca eventos por status
     */
    List<OutboxJpaEntity> findByStatus(OutboxStatus status);
    
    /**
     * Busca eventos pendentes com limite (paginação)
     */
    List<OutboxJpaEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}

