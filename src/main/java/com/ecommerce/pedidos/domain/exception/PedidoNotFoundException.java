package com.ecommerce.pedidos.domain.exception;

/**
 * Exceção lançada quando um pedido não é encontrado
 */
public class PedidoNotFoundException extends RuntimeException {
    
    public PedidoNotFoundException(Long id) {
        super("Pedido não encontrado com ID: " + id);
    }
    
    public PedidoNotFoundException(String numeroPedido) {
        super("Pedido não encontrado com número: " + numeroPedido);
    }
    
    public PedidoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}




