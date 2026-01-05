package com.ecommerce.pedidos.application.usecase;

import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.domain.exception.PedidoNotFoundException;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caso de Uso: Buscar pedido por ID
 */
public class BuscarPedidoPorIdUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(BuscarPedidoPorIdUseCase.class);
    
    private final PedidoRepositoryPort pedidoRepository;
    
    public BuscarPedidoPorIdUseCase(PedidoRepositoryPort pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }
    
    /**
     * Executa a busca de um pedido por ID
     * 
     * @param id ID do pedido
     * @return Pedido encontrado
     * @throws PedidoNotFoundException se o pedido não for encontrado
     */
    public Pedido executar(Long id) {
        logger.debug("Buscando pedido com ID {}", id);
        
        return pedidoRepository.buscarPorId(id)
                .orElseThrow(() -> {
                    logger.warn("Pedido com ID {} não encontrado", id);
                    return new PedidoNotFoundException(id);
                });
    }
}




