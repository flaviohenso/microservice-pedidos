package com.ecommerce.pedidos.application.usecase;

import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.domain.exception.PedidoNotFoundException;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Caso de Uso: Cancelar um pedido
 */
public class CancelarPedidoUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(CancelarPedidoUseCase.class);
    
    private final PedidoRepositoryPort pedidoRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final ObjectMapper objectMapper;
    
    public CancelarPedidoUseCase(
            PedidoRepositoryPort pedidoRepository,
            OutboxRepositoryPort outboxRepository,
            ObjectMapper objectMapper) {
        this.pedidoRepository = pedidoRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Executa o cancelamento de um pedido
     * 
     * @param id ID do pedido
     * @return Pedido cancelado
     * @throws PedidoNotFoundException se o pedido não for encontrado
     * @throws IllegalStateException se o pedido não puder ser cancelado
     */
    @Transactional
    public Pedido executar(Long id) {
        logger.info("Iniciando cancelamento do pedido {}", id);
        
        // 1. Busca pedido
        Pedido pedido = pedidoRepository.buscarPorId(id)
                .orElseThrow(() -> {
                    logger.warn("Pedido {} não encontrado para cancelamento", id);
                    return new PedidoNotFoundException(id);
                });
        
        // 2. Cancela pedido (validação no domínio)
        pedido.cancelar();
        
        // 3. Atualiza pedido no banco
        Pedido pedidoAtualizado = pedidoRepository.atualizar(pedido);
        logger.info("Pedido {} cancelado com sucesso", pedido.getNumeroPedido());
        
        // 4. Criar evento na tabela Outbox
        criarEventoOutbox(pedidoAtualizado);
        
        return pedidoAtualizado;
    }
    
    /**
     * Cria evento na tabela Outbox
     */
    private void criarEventoOutbox(Pedido pedido) {
        try {
            // Cria payload simplificado para evento de cancelamento
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("pedidoId", pedido.getId());
            eventData.put("numeroPedido", pedido.getNumeroPedido());
            eventData.put("clienteId", pedido.getClienteId());
            eventData.put("status", pedido.getStatus().name());
            eventData.put("dataAtualizacao", pedido.getDataAtualizacao());
            
            String payload = objectMapper.writeValueAsString(eventData);
            
            // Cria registro na Outbox
            OutboxEvent outboxEvent = new OutboxEvent(
                    "PEDIDO",
                    pedido.getId(),
                    "PEDIDO_CANCELADO",
                    payload
            );
            
            outboxRepository.salvar(outboxEvent);
            
            logger.debug("Evento PEDIDO_CANCELADO registrado na Outbox para pedido {}", pedido.getId());
            
        } catch (Exception e) {
            logger.error("Erro ao criar evento Outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao registrar evento de cancelamento", e);
        }
    }
}




