package com.ecommerce.pedidos.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ItemPedidoTest {
    
    @Test
    void deveCriarItemPedidoComSucesso() {
        // Arrange & Act
        ItemPedido item = new ItemPedido(
                1L,
                "Notebook",
                2,
                new BigDecimal("2500.00")
        );
        
        // Assert
        assertNotNull(item);
        assertEquals(1L, item.getProdutoId());
        assertEquals("Notebook", item.getNomeProduto());
        assertEquals(2, item.getQuantidade());
        assertEquals(new BigDecimal("2500.00"), item.getPrecoUnitario());
    }
    
    @Test
    void deveCalcularSubtotalCorretamente() {
        // Arrange
        ItemPedido item = new ItemPedido(
                1L,
                "Mouse",
                3,
                new BigDecimal("50.00")
        );
        
        // Act
        BigDecimal subtotal = item.calcularSubtotal();
        
        // Assert
        assertEquals(new BigDecimal("150.00"), subtotal);
    }
    
    @Test
    void deveLancarExcecaoQuandoProdutoIdNulo() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new ItemPedido(
                    null,
                    "Produto",
                    1,
                    BigDecimal.TEN
            );
        });
    }
    
    @Test
    void deveLancarExcecaoQuandoNomeProdutoNulo() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new ItemPedido(
                    1L,
                    null,
                    1,
                    BigDecimal.TEN
            );
        });
    }
    
    @Test
    void deveLancarExcecaoQuandoQuantidadeZero() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new ItemPedido(
                    1L,
                    "Produto",
                    0,
                    BigDecimal.TEN
            );
        });
    }
    
    @Test
    void deveLancarExcecaoQuandoPrecoNegativo() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new ItemPedido(
                    1L,
                    "Produto",
                    1,
                    new BigDecimal("-10.00")
            );
        });
    }
}

