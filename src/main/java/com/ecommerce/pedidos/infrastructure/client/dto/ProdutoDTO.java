package com.ecommerce.pedidos.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para representar dados de produto vindos do microserviço de produtos
 * Espelha o ProdutoResponseDTO do microservice-produtos
 */
public record ProdutoDTO(Long id, String nome, String descricao, BigDecimal preco, Integer estoque, String categoria,
        LocalDateTime dataCriacao) {

}


