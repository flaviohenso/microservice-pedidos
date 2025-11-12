package com.ecommerce.pedidos.domain.entity;

/**
 * Enum representando os possíveis status de um pedido
 */
public enum StatusPedido {
    PENDENTE("Pedido criado, aguardando confirmação"),
    CONFIRMADO("Pedido confirmado e processado"),
    CANCELADO("Pedido cancelado");
    
    private final String descricao;
    
    StatusPedido(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}

