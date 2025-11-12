package com.ecommerce.pedidos.domain.exception;

/**
 * Exceção lançada quando um produto não está disponível para pedido
 */
public class ProdutoIndisponivelException extends RuntimeException {
    
    public ProdutoIndisponivelException(Long produtoId) {
        super("Produto indisponível com ID: " + produtoId);
    }
    
    public ProdutoIndisponivelException(Long produtoId, String motivo) {
        super("Produto ID " + produtoId + " indisponível: " + motivo);
    }
    
    public ProdutoIndisponivelException(String message) {
        super(message);
    }
}

