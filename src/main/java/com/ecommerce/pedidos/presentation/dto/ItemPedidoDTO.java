package com.ecommerce.pedidos.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para representar um item de pedido nas requisições/respostas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoDTO {
    
    @NotNull(message = "ID do produto é obrigatório")
    private Long produtoId;
    
    private String nomeProduto;
    
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;
    
    private BigDecimal precoUnitario;
    
    private BigDecimal subtotal;
}

