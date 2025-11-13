package com.ecommerce.pedidos.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para resposta de pedido
 */

public record PedidoResponseDTO(Long id, String numeroPedido, Long clienteId, List<ItemPedidoDTO> itens, BigDecimal valorTotal, String status, LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
    
}

