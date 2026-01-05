package com.ecommerce.pedidos.domain.entity;

/**
 * Enum representando os possíveis status de um evento na tabela Outbox
 */
public enum OutboxStatus {
    PENDING("Evento aguardando processamento"),
    PROCESSED("Evento processado com sucesso"),
    FAILED("Evento falhou após tentativas");
    
    private final String descricao;
    
    OutboxStatus(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}




