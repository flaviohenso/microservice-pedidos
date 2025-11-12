package com.ecommerce.pedidos.application.usecase;

import com.ecommerce.pedidos.application.service.ProdutoServicePort;
import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.domain.exception.ProdutoIndisponivelException;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import com.ecommerce.pedidos.infrastructure.client.dto.ProdutoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CriarPedidoUseCaseTest {
    
    @Mock
    private PedidoRepositoryPort pedidoRepository;
    
    @Mock
    private OutboxRepositoryPort outboxRepository;
    
    @Mock
    private ProdutoServicePort produtoService;
    
    private CriarPedidoUseCase useCase;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        useCase = new CriarPedidoUseCase(pedidoRepository, outboxRepository, produtoService, objectMapper);
    }
    
    @Test
    void deveCriarPedidoComSucesso() {
        // Arrange
        Long clienteId = 123L;
        List<CriarPedidoUseCase.ItemPedidoRequest> itensRequest = List.of(
                new CriarPedidoUseCase.ItemPedidoRequest(1L, 2),
                new CriarPedidoUseCase.ItemPedidoRequest(2L, 1)
        );
        
        ProdutoDTO produto1 = new ProdutoDTO(1L, "Produto 1", "Desc", new BigDecimal("100.00"), 10, "Cat", null);
        ProdutoDTO produto2 = new ProdutoDTO(2L, "Produto 2", "Desc", new BigDecimal("50.00"), 5, "Cat", null);
        
        when(produtoService.buscarProdutoPorId(1L)).thenReturn(Optional.of(produto1));
        when(produtoService.buscarProdutoPorId(2L)).thenReturn(Optional.of(produto2));
        when(produtoService.verificarEstoque(1L, 2)).thenReturn(true);
        when(produtoService.verificarEstoque(2L, 1)).thenReturn(true);
        when(pedidoRepository.salvar(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        Pedido pedidoCriado = useCase.executar(clienteId, itensRequest);
        
        // Assert
        assertNotNull(pedidoCriado);
        assertEquals(clienteId, pedidoCriado.getClienteId());
        assertEquals(2, pedidoCriado.getItens().size());
        
        verify(pedidoRepository, times(1)).salvar(any(Pedido.class));
        verify(outboxRepository, times(1)).salvar(any());
    }
    
    @Test
    void deveLancarExcecaoQuandoProdutoNaoEncontrado() {
        // Arrange
        Long clienteId = 123L;
        List<CriarPedidoUseCase.ItemPedidoRequest> itensRequest = List.of(
                new CriarPedidoUseCase.ItemPedidoRequest(999L, 1)
        );
        
        when(produtoService.buscarProdutoPorId(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ProdutoIndisponivelException.class, () -> {
            useCase.executar(clienteId, itensRequest);
        });
        
        verify(pedidoRepository, never()).salvar(any());
        verify(outboxRepository, never()).salvar(any());
    }
    
    @Test
    void deveLancarExcecaoQuandoEstoqueInsuficiente() {
        // Arrange
        Long clienteId = 123L;
        List<CriarPedidoUseCase.ItemPedidoRequest> itensRequest = List.of(
                new CriarPedidoUseCase.ItemPedidoRequest(1L, 100)
        );
        
        ProdutoDTO produto = new ProdutoDTO(1L, "Produto 1", "Desc", new BigDecimal("100.00"), 5, "Cat", null);
        
        when(produtoService.buscarProdutoPorId(1L)).thenReturn(Optional.of(produto));
        when(produtoService.verificarEstoque(1L, 100)).thenReturn(false);
        
        // Act & Assert
        assertThrows(ProdutoIndisponivelException.class, () -> {
            useCase.executar(clienteId, itensRequest);
        });
        
        verify(pedidoRepository, never()).salvar(any());
        verify(outboxRepository, never()).salvar(any());
    }
}

