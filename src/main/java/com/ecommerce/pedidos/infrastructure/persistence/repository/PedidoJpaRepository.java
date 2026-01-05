package com.ecommerce.pedidos.infrastructure.persistence.repository;

import com.ecommerce.pedidos.infrastructure.persistence.entity.PedidoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para Pedido
 */
@Repository
public interface PedidoJpaRepository extends JpaRepository<PedidoJpaEntity, Long> {
    
    /**
     * Busca pedido por número
     */
    Optional<PedidoJpaEntity> findByNumeroPedido(String numeroPedido);
    
    /**
     * Lista pedidos de um cliente
     */
    List<PedidoJpaEntity> findByClienteId(Long clienteId);
}




