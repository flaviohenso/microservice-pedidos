package com.ecommerce.pedidos.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {
    
    @Test
    void deveCriarPedidoComSucesso() {
        // Arrange
        List<ItemPedido> itens = List.of(
                new ItemPedido(1L, "Produto 1", 2, new BigDecimal("100.00")),
                new ItemPedido(2L, "Produto 2", 1, new BigDecimal("50.00"))
        );
        
        // Act
        Pedido pedido = new Pedido(123L, itens);
        
        // Assert
        assertNotNull(pedido);
        assertEquals(123L, pedido.getClienteId());
        assertEquals(2, pedido.getItens().size());
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        assertNotNull(pedido.getNumeroPedido());
        assertNotNull(pedido.getDataCriacao());
    }
    
    @Test
    void deveCalcularTotalCorretamente() {
        // Arrange
        List<ItemPedido> itens = List.of(
                new ItemPedido(1L, "Produto 1", 2, new BigDecimal("100.00")),
                new ItemPedido(2L, "Produto 2", 3, new BigDecimal("50.00"))
        );
        Pedido pedido = new Pedido(123L, itens);
        
        // Act
        BigDecimal total = pedido.calcularTotal();
        
        // Assert
        assertEquals(new BigDecimal("350.00"), total);
    }
    
    @Test
    void deveCancelarPedidoPendente() {
        // Arrange
        List<ItemPedido> itens = List.of(
                new ItemPedido(1L, "Produto 1", 1, BigDecimal.TEN)
        );
        Pedido pedido = new Pedido(123L, itens);
        
        // Act
        pedido.cancelar();
        
        // Assert
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertTrue(pedido.isCancelado());
    }
    
    @Test
    void naoDeveCancelarPedidoJaCancelado() {
        // Arrange
        List<ItemPedido> itens = List.of(
                new ItemPedido(1L, "Produto 1", 1, BigDecimal.TEN)
        );
        Pedido pedido = new Pedido(123L, itens);
        pedido.cancelar();
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> pedido.cancelar());
    }
    
    @Test
    void deveLancarExcecaoQuandoClienteIdNulo() {
        // Arrange
        List<ItemPedido> itens = List.of(
                new ItemPedido(1L, "Produto 1", 1, BigDecimal.TEN)
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new Pedido(null, itens);
        });
    }
    
    @Test
    void deveLancarExcecaoQuandoListaItensVazia() {
        // Arrange
        List<ItemPedido> itens = new ArrayList<>();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new Pedido(123L, itens);
        });
    }
    
    @Test
    void deveRetornarListaItensImutavel() {
        // Arrange
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido(1L, "Produto 1", 1, BigDecimal.TEN));
        Pedido pedido = new Pedido(123L, itens);
        
        // Act & Assert
        List<ItemPedido> itensRetornados = pedido.getItens();
        assertThrows(UnsupportedOperationException.class, () -> {
            itensRetornados.add(new ItemPedido(2L, "Produto 2", 1, BigDecimal.TEN));
        });
    }
}

