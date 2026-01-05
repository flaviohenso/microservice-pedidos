package com.ecommerce.pedidos.presentation.dto;



import java.util.List;

/**
 * DTO para requisição de criação de pedido
 */

public record PedidoRequestDTO(Long clienteId, List<ItemPedidoDTO> itens) {
    
}




