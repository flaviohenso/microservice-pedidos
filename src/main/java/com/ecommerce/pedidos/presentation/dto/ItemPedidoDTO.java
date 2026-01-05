package com.ecommerce.pedidos.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para representar um item de pedido nas requisições/respostas
 */
public record ItemPedidoDTO(Long produtoId, String nomeProduto, Integer quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
}




