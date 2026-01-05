package com.ecommerce.pedidos.domain.repository;

import com.ecommerce.pedidos.domain.entity.Pedido;

import java.util.List;
import java.util.Optional;

/**
 * Porta (interface) do repositório de Pedido
 * Define o contrato para persistência de pedidos
 */
public interface PedidoRepositoryPort {
    
    /**
     * Salva um pedido
     */
    Pedido salvar(Pedido pedido);
    
    /**
     * Busca um pedido por ID
     */
    Optional<Pedido> buscarPorId(Long id);
    
    /**
     * Busca um pedido por número
     */
    Optional<Pedido> buscarPorNumeroPedido(String numeroPedido);
    
    /**
     * Lista todos os pedidos
     */
    List<Pedido> listarTodos();
    
    /**
     * Lista pedidos de um cliente específico
     */
    List<Pedido> listarPorClienteId(Long clienteId);
    
    /**
     * Atualiza um pedido
     */
    Pedido atualizar(Pedido pedido);
    
    /**
     * Deleta um pedido por ID
     */
    void deletar(Long id);
}




