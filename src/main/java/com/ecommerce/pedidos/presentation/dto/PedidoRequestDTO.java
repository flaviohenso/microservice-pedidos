package com.ecommerce.pedidos.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para requisição de criação de pedido
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequestDTO {
    
    @NotNull(message = "ID do cliente é obrigatório")
    private Long clienteId;
    
    @NotEmpty(message = "O pedido deve conter pelo menos um item")
    @Valid
    private List<ItemPedidoDTO> itens;
}

