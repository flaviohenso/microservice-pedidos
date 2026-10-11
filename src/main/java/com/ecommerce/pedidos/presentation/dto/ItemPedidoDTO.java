package com.ecommerce.pedidos.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO para representar um item de pedido nas requisições/respostas
 */
public record ItemPedidoDTO(
        @NotNull(message = "ID do produto é obrigatório")
        @Positive(message = "ID do produto deve ser positivo")
        Long produtoId,

        String nomeProduto,

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
        Integer quantidade,

        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
}




