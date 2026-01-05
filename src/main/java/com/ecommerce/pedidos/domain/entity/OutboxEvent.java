package com.ecommerce.pedidos.domain.entity;

import java.time.LocalDateTime;

/**
 * Entidade de domínio - Representa um evento na tabela Outbox
 * Garante consistência eventual entre persistência e mensageria
 */
public class OutboxEvent {
    
    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private OutboxStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Integer retryCount;
    private String errorMessage;
    
    // Construtor para criação (sem ID)
    public OutboxEvent(String aggregateType, Long aggregateId, String eventType, String payload) {
        this.validarCamposObrigatorios(aggregateType, aggregateId, eventType, payload);
        
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }
    
    // Construtor para reconstituição (com ID - vindo do BD)
    public OutboxEvent(Long id, String aggregateType, Long aggregateId, String eventType, 
                       String payload, OutboxStatus status, LocalDateTime createdAt, 
                       LocalDateTime processedAt, Integer retryCount, String errorMessage) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
    }
    
    // ===== REGRAS DE NEGÓCIO =====
    
    private void validarCamposObrigatorios(String aggregateType, Long aggregateId, String eventType, String payload) {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("Tipo do agregado é obrigatório");
        }
        if (aggregateId == null) {
            throw new IllegalArgumentException("ID do agregado é obrigatório");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Tipo do evento é obrigatório");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload do evento é obrigatório");
        }
    }
    
    /**
     * Marca o evento como processado com sucesso
     */
    public void marcarComoProcessado() {
        if (this.status == OutboxStatus.PROCESSED) {
            throw new IllegalStateException("Evento já foi processado");
        }
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * Marca o evento como falho e incrementa o contador de tentativas
     */
    public void marcarComoFalho(String errorMessage) {
        this.retryCount++;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * Marca o evento como falho permanentemente (excedeu tentativas)
     */
    public void marcarComoFalhoDefinitivo(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * Verifica se o evento pode ser reprocessado
     */
    public boolean podeReprocessar(int maxRetries) {
        return this.status == OutboxStatus.PENDING && this.retryCount < maxRetries;
    }
    
    /**
     * Verifica se o evento está pendente
     */
    public boolean isPending() {
        return this.status == OutboxStatus.PENDING;
    }
    
    // ===== GETTERS =====
    
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
}




