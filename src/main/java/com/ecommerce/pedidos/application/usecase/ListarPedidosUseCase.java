package com.ecommerce.pedidos.application.usecase;

import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Caso de Uso: Listar todos os pedidos
 */
public class ListarPedidosUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ListarPedidosUseCase.class);
    
    private final PedidoRepositoryPort pedidoRepository;
    
    public ListarPedidosUseCase(PedidoRepositoryPort pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }
    
    /**
     * Executa a listagem de todos os pedidos
     * 
     * @return Lista de pedidos
     */
    public List<Pedido> executar() {
        logger.debug("Listando todos os pedidos");
        return pedidoRepository.listarTodos();
    }
    
    /**
     * Executa a listagem de pedidos de um cliente específico
     * 
     * @param clienteId ID do cliente
     * @return Lista de pedidos do cliente
     */
    public List<Pedido> executarPorCliente(Long clienteId) {
        logger.debug("Listando pedidos do cliente {}", clienteId);
        return pedidoRepository.listarPorClienteId(clienteId);
    }
}

