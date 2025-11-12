package com.ecommerce.pedidos.application.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento representando a criação de um pedido
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoCriadoEvent {
    
    private Long pedidoId;
    private String numeroPedido;
    private Long clienteId;
    private List<ItemPedidoEvent> itens;
    private BigDecimal valorTotal;
    private String status;
    private LocalDateTime dataCriacao;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPedidoEvent {
        private Long produtoId;
        private String nomeProduto;
        private Integer quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal subtotal;
    }
}

