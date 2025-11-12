package com.ecommerce.pedidos.domain.entity;

import java.math.BigDecimal;

/**
 * Entidade de domínio - Representa um item de pedido
 * Importante: Não tem anotações de Framework (JPA, Hibernate, etc)
 */
public class ItemPedido {
    
    private Long id;
    private Long produtoId;
    private String nomeProduto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    
    // Construtor para criação (sem ID)
    public ItemPedido(Long produtoId, String nomeProduto, Integer quantidade, BigDecimal precoUnitario) {
        this.validarCamposObrigatorios(produtoId, nomeProduto, quantidade, precoUnitario);
        this.validarRegrasDeNegocio(quantidade, precoUnitario);
        
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }
    
    // Construtor para reconstituição (com ID - vindo do BD)
    public ItemPedido(Long id, Long produtoId, String nomeProduto, Integer quantidade, BigDecimal precoUnitario) {
        this.id = id;
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }
    
    // ===== REGRAS DE NEGÓCIO =====
    
    private void validarCamposObrigatorios(Long produtoId, String nomeProduto, Integer quantidade, BigDecimal precoUnitario) {
        if (produtoId == null) {
            throw new IllegalArgumentException("ID do produto é obrigatório");
        }
        if (nomeProduto == null || nomeProduto.isBlank()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório");
        }
        if (quantidade == null) {
            throw new IllegalArgumentException("Quantidade é obrigatória");
        }
        if (precoUnitario == null) {
            throw new IllegalArgumentException("Preço unitário é obrigatório");
        }
    }
    
    private void validarRegrasDeNegocio(Integer quantidade, BigDecimal precoUnitario) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        if (precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço unitário deve ser maior que zero");
        }
    }
    
    /**
     * Calcula o subtotal do item (quantidade * preço unitário)
     */
    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
    
    // ===== GETTERS (sem setters para imutabilidade) =====
    
    public Long getId() {
        return id;
    }
    
    public Long getProdutoId() {
        return produtoId;
    }
    
    public String getNomeProduto() {
        return nomeProduto;
    }
    
    public Integer getQuantidade() {
        return quantidade;
    }
    
    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}

