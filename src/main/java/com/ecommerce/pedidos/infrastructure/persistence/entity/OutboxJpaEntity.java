package com.ecommerce.pedidos.infrastructure.persistence.entity;

import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade JPA para Outbox
 * Representa a tabela outbox no banco de dados
 */
@Entity
@Table(name = "outbox", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;
    
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}

