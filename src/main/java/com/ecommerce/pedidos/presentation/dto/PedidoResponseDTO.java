package com.ecommerce.pedidos.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para resposta de pedido
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponseDTO {
    
    private Long id;
    private String numeroPedido;
    private Long clienteId;
    private List<ItemPedidoDTO> itens;
    private BigDecimal valorTotal;
    private String status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}

