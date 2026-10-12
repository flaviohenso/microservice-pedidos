package com.ecommerce.pedidos.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para representar dados de produto vindos do microserviço de produtos
 * Mantido na camada de aplicação para respeitar o isolamento de arquitetura hexagonal
 */
public record ProdutoDTO(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer estoque,
        String categoria,
        LocalDateTime dataCriacao
) {
}
