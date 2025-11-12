package com.ecommerce.pedidos.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para representar dados de produto vindos do microserviço de produtos
 * Espelha o ProdutoResponseDTO do microservice-produtos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDTO {
    
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private Integer estoque;
    private String categoria;
    private LocalDateTime dataCriacao;
}

