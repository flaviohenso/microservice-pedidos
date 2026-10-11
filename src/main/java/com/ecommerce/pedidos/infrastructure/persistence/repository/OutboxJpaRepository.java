package com.ecommerce.pedidos.infrastructure.persistence.repository;

import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import com.ecommerce.pedidos.infrastructure.persistence.entity.OutboxJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /**
     * Busca eventos pendentes bloqueando as linhas com SKIP LOCKED para concorrência segura multi-instância
     */
    @Query(value = """
            SELECT * FROM outbox 
            WHERE status = :status 
            ORDER BY created_at ASC 
            LIMIT :limite 
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxJpaEntity> findByStatusOrderByCreatedAtAscSkipLocked(
            @Param("status") String status,
            @Param("limite") int limite
    );

    /**
     * Busca eventos por ID do agregado
     */
    List<OutboxJpaEntity> findByAggregateId(Long aggregateId);

    /**
     * Remove eventos processados antes de uma data limite (expurgo periódico)
     */
    @Modifying
    @Query("DELETE FROM OutboxJpaEntity o WHERE o.status = :status AND o.processedAt <= :dataLimite")
    int deleteByStatusAndProcessedAtBefore(
            @Param("status") OutboxStatus status,
            @Param("dataLimite") LocalDateTime dataLimite
    );
}




