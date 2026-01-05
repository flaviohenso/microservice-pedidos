package com.ecommerce.pedidos.application.service;

import com.ecommerce.pedidos.infrastructure.client.dto.ProdutoDTO;

import java.util.Optional;

/**
 * Porta (interface) para serviço de produtos
 * Define o contrato para comunicação com o microserviço de produtos
 */
public interface ProdutoServicePort {
    
    /**
     * Busca um produto por ID
     */
    Optional<ProdutoDTO> buscarProdutoPorId(Long id);
    
    /**
     * Verifica se há estoque disponível para um produto
     */
    boolean verificarEstoque(Long produtoId, Integer quantidade);
}




