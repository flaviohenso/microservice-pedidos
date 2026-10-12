package com.ecommerce.pedidos.domain.exception;

/**
 * Exceção lançada quando um usuário tenta acessar ou modificar um recurso que pertence a outro usuário (IDOR/BOLA).
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String message) {
        super(message);
    }
}
