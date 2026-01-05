package com.ecommerce.pedidos.infrastructure.persistence.mapper;

import com.ecommerce.pedidos.domain.entity.ItemPedido;
import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.infrastructure.persistence.entity.ItemPedidoJpaEntity;
import com.ecommerce.pedidos.infrastructure.persistence.entity.PedidoJpaEntity;

import java.util.stream.Collectors;

/**
 * Mapper para conversão entre Pedido (domínio) e PedidoJpaEntity (infraestrutura)
 */
public class PedidoMapper {
    
    /**
     * Converte entidade de domínio para entidade JPA
     */
    public static PedidoJpaEntity toJpaEntity(Pedido pedido) {
        if (pedido == null) {
            return null;
        }
        
        PedidoJpaEntity jpaEntity = new PedidoJpaEntity();
        jpaEntity.setId(pedido.getId());
        jpaEntity.setNumeroPedido(pedido.getNumeroPedido());
        jpaEntity.setClienteId(pedido.getClienteId());
        jpaEntity.setStatus(pedido.getStatus());
        jpaEntity.setDataCriacao(pedido.getDataCriacao());
        jpaEntity.setDataAtualizacao(pedido.getDataAtualizacao());
        
        // Converte itens
        pedido.getItens().forEach(item -> {
            ItemPedidoJpaEntity itemJpa = toJpaEntity(item);
            jpaEntity.adicionarItem(itemJpa);
        });
        
        return jpaEntity;
    }
    
    /**
     * Converte ItemPedido (domínio) para ItemPedidoJpaEntity
     */
    private static ItemPedidoJpaEntity toJpaEntity(ItemPedido item) {
        if (item == null) {
            return null;
        }
        
        ItemPedidoJpaEntity jpaEntity = new ItemPedidoJpaEntity();
        jpaEntity.setId(item.getId());
        jpaEntity.setProdutoId(item.getProdutoId());
        jpaEntity.setNomeProduto(item.getNomeProduto());
        jpaEntity.setQuantidade(item.getQuantidade());
        jpaEntity.setPrecoUnitario(item.getPrecoUnitario());
        
        return jpaEntity;
    }
    
    /**
     * Converte entidade JPA para entidade de domínio
     */
    public static Pedido toDomain(PedidoJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        // Converte itens
        var itens = jpaEntity.getItens().stream()
                .map(PedidoMapper::toDomain)
                .collect(Collectors.toList());
        
        return new Pedido(
                jpaEntity.getId(),
                jpaEntity.getNumeroPedido(),
                jpaEntity.getClienteId(),
                itens,
                jpaEntity.getStatus(),
                jpaEntity.getDataCriacao(),
                jpaEntity.getDataAtualizacao()
        );
    }
    
    /**
     * Converte ItemPedidoJpaEntity para ItemPedido (domínio)
     */
    private static ItemPedido toDomain(ItemPedidoJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        return new ItemPedido(
                jpaEntity.getId(),
                jpaEntity.getProdutoId(),
                jpaEntity.getNomeProduto(),
                jpaEntity.getQuantidade(),
                jpaEntity.getPrecoUnitario()
        );
    }
}




