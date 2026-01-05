package com.ecommerce.pedidos.infrastructure.persistence.entity;

import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import jakarta.persistence.*;


import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade JPA para Outbox
 * Representa a tabela outbox no banco de dados
 */
@Entity
@Table(name = "outbox", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_created_at", columnList = "created_at")
})

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

    public Long getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }
    
    public Long getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }
    
    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OutboxJpaEntity other = (OutboxJpaEntity) obj;
        return Objects.equals(id, other.id) && Objects.equals(aggregateType, other.aggregateType) && Objects.equals(aggregateId, other.aggregateId) && Objects.equals(eventType, other.eventType) && Objects.equals(payload, other.payload) && Objects.equals(status, other.status) && Objects.equals(createdAt, other.createdAt) && Objects.equals(processedAt, other.processedAt) && Objects.equals(retryCount, other.retryCount) && Objects.equals(errorMessage, other.errorMessage);
    }

    public int hashCode() {
        return Objects.hash(id, aggregateType, aggregateId, eventType, payload, status, createdAt, processedAt, retryCount, errorMessage);
    }
}




