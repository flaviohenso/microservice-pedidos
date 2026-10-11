package com.ecommerce.pedidos.presentation.dto;



import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * DTO para requisição de criação de pedido
 */
public record PedidoRequestDTO(
        @NotNull(message = "ID do cliente é obrigatório")
        @Positive(message = "ID do cliente deve ser positivo")
        Long clienteId,

        @NotNull(message = "Lista de itens é obrigatória")
        @NotEmpty(message = "Pedido deve conter ao menos um item")
        @Valid
        List<ItemPedidoDTO> itens
) {
}




