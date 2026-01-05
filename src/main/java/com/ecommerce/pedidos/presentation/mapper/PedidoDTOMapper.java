package com.ecommerce.pedidos.presentation.mapper;

import com.ecommerce.pedidos.domain.entity.ItemPedido;
import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.presentation.dto.ItemPedidoDTO;
import com.ecommerce.pedidos.presentation.dto.PedidoResponseDTO;

import java.util.stream.Collectors;

/**
 * Mapper para conversão entre entidades de domínio e DTOs
 */
public class PedidoDTOMapper {
    
    /**
     * Converte Pedido (domínio) para PedidoResponseDTO
     */
    public static PedidoResponseDTO toResponseDTO(Pedido pedido) {
        if (pedido == null) {
            return null;
        }
        
        var itensDTO = pedido.getItens().stream()
                .map(PedidoDTOMapper::toDTO)
                .collect(Collectors.toList());
        
        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getClienteId(),
                itensDTO,
                pedido.calcularTotal(),
                pedido.getStatus().name(),
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }
    
    /**
     * Converte ItemPedido (domínio) para ItemPedidoDTO
     */
    public static ItemPedidoDTO toDTO(ItemPedido item) {
        if (item == null) {
            return null;
        }
        
        return new ItemPedidoDTO(
                item.getProdutoId(),
                item.getNomeProduto(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.calcularSubtotal()
        );
    }
}




